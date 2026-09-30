package com.tkno.gallery.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import com.tkno.gallery.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaIntentResolver {

    fun isMediaIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        val action = intent.action ?: return false
        return action == Intent.ACTION_VIEW ||
                action == Intent.ACTION_SEND ||
                action == Intent.ACTION_SEND_MULTIPLE ||
                action == MediaStore.ACTION_REVIEW ||
                action == "com.android.camera.action.REVIEW" ||
                action == "android.provider.action.REVIEW"
    }

    fun extractUrisFromIntent(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        val uris = mutableListOf<Uri>()

        try {
            // 1. ClipData (Standard on Android for sharing one or multiple items)
            val clipData = intent.clipData
            if (clipData != null && clipData.itemCount > 0) {
                for (i in 0 until clipData.itemCount) {
                    val item = clipData.getItemAt(i)
                    item?.uri?.let { uris.add(it) }
                }
            }

            // 2. Intent Data URI
            intent.data?.let { uris.add(it) }

            // 3. EXTRA_STREAM single Parcelable
            try {
                @Suppress("DEPRECATION")
                val stream = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)
                if (stream is Uri) {
                    uris.add(stream)
                }
            } catch (_: Throwable) {}

            // 4. EXTRA_STREAM ArrayList
            try {
                @Suppress("DEPRECATION")
                val streamList = intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)
                if (!streamList.isNullOrEmpty()) {
                    for (p in streamList) {
                        if (p is Uri) uris.add(p)
                    }
                }
            } catch (_: Throwable) {}

            // 5. Raw Extras inspection fallback
            try {
                val extras = intent.extras
                if (extras != null) {
                    val extraStream = extras.get(Intent.EXTRA_STREAM)
                    if (extraStream is Uri) {
                        uris.add(extraStream)
                    } else if (extraStream is List<*>) {
                        for (item in extraStream) {
                            if (item is Uri) uris.add(item)
                            else if (item is String) {
                                try { uris.add(Uri.parse(item)) } catch (_: Throwable) {}
                            }
                        }
                    } else if (extraStream is String) {
                        try { uris.add(Uri.parse(extraStream)) } catch (_: Throwable) {}
                    }
                }
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}

        return uris.filterNotNull().distinct()
    }

    suspend fun resolveMediaItemsFromIntent(
        context: Context,
        intent: Intent
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val uris = extractUrisFromIntent(intent)
            if (uris.isEmpty()) return@withContext emptyList()

            val mimeTypeHint = intent.type
            uris.mapIndexed { index, uri ->
                resolveUriToMediaItem(context, uri, mimeTypeHint, fallbackIndex = index)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun resolveUriToMediaItem(
        context: Context,
        uri: Uri,
        mimeTypeHint: String? = null,
        fallbackIndex: Int = 0
    ): MediaItem {
        // Safe unique positive ID
        val safeUniqueId: Long = runCatching {
            ContentUris.parseId(uri)
        }.getOrNull() ?: run {
            val hash = uri.toString().hashCode().toLong()
            val positiveHash = if (hash == Long.MIN_VALUE) 1L else kotlin.math.abs(hash)
            (positiveHash + fallbackIndex).coerceAtLeast(1L)
        }

        var displayName = "Media_$safeUniqueId"
        var sizeBytes = 0L
        var rawPath = ""

        // Try querying OpenableColumns or MediaStore columns safely
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameCol != -1) cursor.getString(nameCol)?.let { displayName = it }

                    val sizeCol = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeCol != -1) sizeBytes = cursor.getLong(sizeCol)

                    val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (dataCol != -1) cursor.getString(dataCol)?.let { rawPath = it }
                }
            }
        } catch (_: Throwable) {}

        if (rawPath.isBlank() && uri.scheme == ContentResolver.SCHEME_FILE) {
            rawPath = uri.path ?: ""
        }

        if (displayName.startsWith("Media_") && rawPath.isNotBlank()) {
            try {
                val file = File(rawPath)
                if (file.name.isNotBlank()) {
                    displayName = file.name
                    if (sizeBytes <= 0L) sizeBytes = file.length()
                }
            } catch (_: Throwable) {}
        }

        // Detect MIME Type
        var detectedMime = runCatching { context.contentResolver.getType(uri) }.getOrNull() ?: mimeTypeHint ?: ""
        if (detectedMime.isBlank() || detectedMime == "*/*") {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                .ifEmpty { displayName.substringAfterLast('.', "") }
            if (extension.isNotEmpty()) {
                detectedMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: ""
            }
        }

        val isVideo = detectedMime.startsWith("video/") ||
                displayName.endsWith(".mp4", true) ||
                displayName.endsWith(".mkv", true) ||
                displayName.endsWith(".mov", true) ||
                displayName.endsWith(".webm", true) ||
                displayName.endsWith(".3gp", true) ||
                displayName.endsWith(".ts", true)

        val mimeType = if (detectedMime.isNotBlank()) detectedMime else if (isVideo) "video/*" else "image/*"

        var width = 0
        var height = 0
        var durationMs = 0L

        if (isVideo) {
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0

                if (rot == 90 || rot == 270) {
                    width = h
                    height = w
                } else {
                    width = w
                    height = h
                }
                durationMs = dur
            } catch (_: Throwable) {} finally {
                try { retriever?.release() } catch (_: Throwable) {}
            }
        } else {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
                width = options.outWidth
                height = options.outHeight
            } catch (_: Throwable) {}

            if (width <= 0 || height <= 0) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val exif = ExifInterface(stream)
                        width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                        height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                    }
                } catch (_: Throwable) {}
            }
        }

        var albumName = "Shared"
        if (rawPath.isNotBlank()) {
            try {
                val parent = File(rawPath).parentFile
                if (parent != null && parent.name.isNotBlank()) {
                    albumName = parent.name
                }
            } catch (_: Throwable) {}
        }

        val nowSec = System.currentTimeMillis() / 1000L

        return MediaItem(
            id = safeUniqueId,
            uri = uri,
            name = displayName,
            path = rawPath,
            size = sizeBytes,
            dateAddedSec = nowSec,
            dateTakenMs = nowSec * 1000L,
            width = width,
            height = height,
            durationMs = durationMs,
            isVideo = isVideo,
            mimeType = mimeType,
            albumName = albumName,
            bucketId = albumName.hashCode().toString(),
            volumeName = "external"
        )
    }

    fun findMatchingMediaItem(
        targetItem: MediaItem,
        mediaList: List<MediaItem>
    ): MediaItem? {
        val targetUri = targetItem.uri
        val targetUriStr = targetUri.toString()
        val targetPath = targetItem.path
        val targetName = targetItem.name

        // 1. Direct URI matching
        mediaList.find { it.uri == targetUri || it.uri.toString() == targetUriStr }?.let { return it }

        // 2. ID matching
        try {
            val parsedId = ContentUris.parseId(targetUri)
            mediaList.find { it.id == parsedId }?.let { return it }
        } catch (_: Throwable) {
            if (targetItem.id > 0L) {
                mediaList.find { it.id == targetItem.id }?.let { return it }
            }
        }

        // 3. Exact Path matching
        if (targetPath.isNotBlank()) {
            mediaList.find { it.path.equals(targetPath, ignoreCase = true) }?.let { return it }
        }

        // 4. Filename / Last path segment matching
        val lastSegment = targetUri.lastPathSegment
        if (!lastSegment.isNullOrBlank()) {
            mediaList.find {
                it.name.equals(lastSegment, ignoreCase = true) ||
                        it.path.endsWith("/$lastSegment", ignoreCase = true)
            }?.let { return it }
        }

        if (targetName.isNotBlank() && !targetName.startsWith("Media_")) {
            mediaList.find {
                it.name.equals(targetName, ignoreCase = true) ||
                        it.path.endsWith("/$targetName", ignoreCase = true)
            }?.let { return it }
        }

        return null
    }
}
