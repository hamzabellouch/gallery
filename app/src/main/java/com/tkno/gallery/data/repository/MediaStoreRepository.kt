package com.tkno.gallery.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.tkno.gallery.data.model.Album
import com.tkno.gallery.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreRepository(private val context: Context) {

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun getTrashedMediaItemsFlow(): Flow<List<MediaItem>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(fetchTrashedMediaItemsSync())
            }

            override fun onChange(selfChange: Boolean) {
                onChange(selfChange, null)
            }
        }

        val resolver = context.contentResolver

        try {
            resolver.registerContentObserver(
                MediaStore.AUTHORITY_URI,
                true,
                observer
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        trySend(fetchTrashedMediaItemsSync())

        awaitClose {
            try {
                resolver.unregisterContentObserver(observer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
        .debounce(200)
        .flowOn(Dispatchers.IO)

    fun fetchTrashedMediaItemsSync(): List<MediaItem> {
        val trashedList = mutableListOf<MediaItem>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bundle = Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
            }
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                "datetaken",
                MediaStore.Files.FileColumns.WIDTH,
                MediaStore.Files.FileColumns.HEIGHT,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DURATION,
                MediaStore.Files.FileColumns.BUCKET_ID,
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
            )
            try {
                context.contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    bundle,
                    null
                )?.use { cursor ->
                    parseCursorToMediaList(cursor, "external", trashedList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        trashedList.sortByDescending { it.dateAddedSec }
        return trashedList
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun getMediaItemsFlow(): Flow<List<MediaItem>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(fetchMediaItemsSync())
            }

            override fun onChange(selfChange: Boolean) {
                onChange(selfChange, null)
            }
        }

        val resolver = context.contentResolver

        try {
            resolver.registerContentObserver(
                MediaStore.AUTHORITY_URI,
                true,
                observer
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            resolver.registerContentObserver(
                MediaStore.Files.getContentUri("external"),
                true,
                observer
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initial emission
        trySend(fetchMediaItemsSync())

        awaitClose {
            try {
                resolver.unregisterContentObserver(observer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
        .debounce(200)
        .flowOn(Dispatchers.IO)

    suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val mediaList = fetchMediaItemsSync()
        val albumMap = LinkedHashMap<String, MutableList<MediaItem>>()

        for (item in mediaList) {
            val compositeKey = if (item.bucketId.isNotBlank()) "${item.volumeName}_${item.bucketId}" else item.albumName
            albumMap.getOrPut(compositeKey) { mutableListOf() }.add(item)
        }

        return@withContext albumMap.map { (key, items) ->
            val displayName = items.firstOrNull { it.albumName.isNotBlank() }?.albumName ?: "Other"
            val isOnSdCard = items.any { isSdCardVolume(it.volumeName) || isPathOnSdCard(it.path) }

            Album(
                id = key,
                name = displayName,
                coverUri = items.firstOrNull()?.uri,
                itemCount = items.size,
                isVideoAlbum = items.all { it.isVideo },
                isOnSdCard = isOnSdCard
            )
        }.sortedByDescending { it.itemCount }
    }

    private fun getVolumeNames(): List<String> {
        val volumes = mutableSetOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val volNames = MediaStore.getExternalVolumeNames(context)
                volumes.addAll(volNames)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (volumes.isEmpty()) {
            volumes.add("external")
        }
        return volumes.toList()
    }

    private fun isSdCardVolume(volumeName: String): Boolean {
        if (volumeName.isBlank()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (volumeName.equals(MediaStore.VOLUME_EXTERNAL_PRIMARY, ignoreCase = true)) return false
            if (volumeName.equals("internal", ignoreCase = true)) return false
            if (volumeName.equals("external", ignoreCase = true)) return false
            return true
        }
        return false
    }

    private fun isPathOnSdCard(path: String): Boolean {
        if (path.isBlank()) return false
        return path.contains("/storage/", ignoreCase = true) &&
                !path.contains("/storage/emulated/", ignoreCase = true) &&
                !path.contains("/storage/self/", ignoreCase = true)
    }

    private fun fetchMediaItemsSync(): List<MediaItem> {
        val mediaList = mutableListOf<MediaItem>()
        val seenUris = HashSet<String>()
        val volumes = getVolumeNames()

        for (volume in volumes) {
            val volumeMediaList = mutableListOf<MediaItem>()

            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                "datetaken",
                MediaStore.Files.FileColumns.WIDTH,
                MediaStore.Files.FileColumns.HEIGHT,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DURATION,
                MediaStore.Files.FileColumns.BUCKET_ID,
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
            )

            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
            val selectionArgs = arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )

            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC, ${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC, ${MediaStore.Files.FileColumns._ID} DESC"

            val contentUri = try {
                MediaStore.Files.getContentUri(volume)
            } catch (e: Exception) {
                MediaStore.Files.getContentUri("external")
            }

            try {
                context.contentResolver.query(
                    contentUri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    parseCursorToMediaList(cursor, volume, volumeMediaList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Fallback for this volume if MediaStore.Files query returns empty
            if (volumeMediaList.isEmpty()) {
                fetchImagesFallback(volume, volumeMediaList)
                fetchVideosFallback(volume, volumeMediaList)
            }

            // Add non-duplicate items
            for (item in volumeMediaList) {
                if (seenUris.add(item.uri.toString())) {
                    mediaList.add(item)
                }
            }
        }

        // Always sort all media items by effective date descending
        mediaList.sortByDescending { it.dateAddedSec }

        return mediaList
    }

    private fun normalizeToSeconds(rawTimestamp: Long): Long {
        if (rawTimestamp == 0L) return 0L
        val absVal = kotlin.math.abs(rawTimestamp)
        return if (absVal >= 100_000_000_000L) rawTimestamp / 1000L else rawTimestamp
    }

    private fun calculateEffectiveDateSec(dateTaken: Long, dateModified: Long, dateAdded: Long): Long {
        val normTaken = normalizeToSeconds(dateTaken)
        val normModified = normalizeToSeconds(dateModified)
        val normAdded = normalizeToSeconds(dateAdded)

        return when {
            normTaken != 0L -> normTaken
            normModified != 0L -> normModified
            normAdded != 0L -> normAdded
            else -> System.currentTimeMillis() / 1000L
        }
    }

    private fun parseCursorToMediaList(cursor: Cursor, volumeName: String, output: MutableList<MediaItem>) {
        val idColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
        val nameColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val pathColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
        val sizeColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
        val dateAddedColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)
        val dateModifiedColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
        val dateTakenColumn = cursor.getColumnIndex("datetaken")
        val widthColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.WIDTH)
        val heightColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.HEIGHT)
        val mediaTypeColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val mimeColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
        val durationColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DURATION)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_ID)
        val bucketColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)

        while (cursor.moveToNext()) {
            val id = if (idColumn != -1) cursor.getLong(idColumn) else continue
            val mediaType = if (mediaTypeColumn != -1) cursor.getInt(mediaTypeColumn) else MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            val isVideo = (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

            val name = if (nameColumn != -1) cursor.getString(nameColumn) ?: (if (isVideo) "Video_$id" else "Image_$id") else if (isVideo) "Video_$id" else "Image_$id"
            val path = if (pathColumn != -1) cursor.getString(pathColumn) ?: "" else ""
            val size = if (sizeColumn != -1) cursor.getLong(sizeColumn) else 0L

            val dateAdded = if (dateAddedColumn != -1) cursor.getLong(dateAddedColumn) else 0L
            val dateModified = if (dateModifiedColumn != -1) cursor.getLong(dateModifiedColumn) else 0L
            val dateTaken = if (dateTakenColumn != -1) cursor.getLong(dateTakenColumn) else 0L

            val effectiveDateSec = calculateEffectiveDateSec(dateTaken, dateModified, dateAdded)
            val effectiveDateTakenMs = if (dateTaken != 0L) (if (kotlin.math.abs(dateTaken) >= 100_000_000_000L) dateTaken else dateTaken * 1000L) else effectiveDateSec * 1000L

            val width = if (widthColumn != -1) cursor.getInt(widthColumn) else 0
            val height = if (heightColumn != -1) cursor.getInt(heightColumn) else 0
            val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L
            val mimeType = if (mimeColumn != -1) cursor.getString(mimeColumn) ?: (if (isVideo) "video/*" else "image/*") else (if (isVideo) "video/*" else "image/*")
            
            var bucketId = if (bucketIdColumn != -1) cursor.getString(bucketIdColumn) ?: "" else ""
            var albumName = if (bucketColumn != -1) cursor.getString(bucketColumn) ?: "" else ""

            if (albumName.isBlank() && path.isNotBlank()) {
                val parentFile = File(path).parentFile
                if (parentFile != null && parentFile.name.isNotBlank()) {
                    albumName = parentFile.name
                    if (bucketId.isBlank()) {
                        bucketId = parentFile.absolutePath.hashCode().toString()
                    }
                }
            }
            if (albumName.isBlank()) albumName = "Other"
            if (bucketId.isBlank()) bucketId = albumName

            val baseUri = try {
                if (isVideo) MediaStore.Video.Media.getContentUri(volumeName) else MediaStore.Images.Media.getContentUri(volumeName)
            } catch (e: Exception) {
                if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val contentUri = ContentUris.withAppendedId(baseUri, id)

            output.add(
                MediaItem(
                    id = id,
                    uri = contentUri,
                    name = name,
                    path = path,
                    size = size,
                    dateAddedSec = effectiveDateSec,
                    dateTakenMs = effectiveDateTakenMs,
                    width = width,
                    height = height,
                    durationMs = duration,
                    isVideo = isVideo,
                    mimeType = mimeType,
                    albumName = albumName,
                    bucketId = bucketId,
                    volumeName = volumeName
                )
            )
        }
    }

    private fun fetchImagesFallback(volumeName: String, output: MutableList<MediaItem>) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val uri = try {
            MediaStore.Images.Media.getContentUri(volumeName)
        } catch (e: Exception) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
                val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = if (idCol != -1) cursor.getLong(idCol) else continue
                    val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Image_$id" else "Image_$id"
                    val path = if (pathCol != -1) cursor.getString(pathCol) ?: "" else ""
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L

                    val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else 0L
                    val dateModified = if (dateModifiedCol != -1) cursor.getLong(dateModifiedCol) else 0L
                    val dateTaken = if (dateTakenCol != -1) cursor.getLong(dateTakenCol) else 0L

                    val effectiveDateSec = calculateEffectiveDateSec(dateTaken, dateModified, dateAdded)
                    val effectiveDateTakenMs = if (dateTaken != 0L) (if (kotlin.math.abs(dateTaken) >= 100_000_000_000L) dateTaken else dateTaken * 1000L) else effectiveDateSec * 1000L

                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
                    val mimeType = if (mimeCol != -1) cursor.getString(mimeCol) ?: "image/*" else "image/*"
                    var bucketId = if (bucketIdCol != -1) cursor.getString(bucketIdCol) ?: "" else ""
                    var albumName = if (bucketCol != -1) cursor.getString(bucketCol) ?: "" else ""

                    if (albumName.isBlank() && path.isNotBlank()) {
                        val parentFile = File(path).parentFile
                        if (parentFile != null && parentFile.name.isNotBlank()) {
                            albumName = parentFile.name
                            if (bucketId.isBlank()) {
                                bucketId = parentFile.absolutePath.hashCode().toString()
                            }
                        }
                    }
                    if (albumName.isBlank()) albumName = "Other"
                    if (bucketId.isBlank()) bucketId = albumName

                    val contentUri = ContentUris.withAppendedId(uri, id)

                    output.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            name = name,
                            path = path,
                            size = size,
                            dateAddedSec = effectiveDateSec,
                            dateTakenMs = effectiveDateTakenMs,
                            width = width,
                            height = height,
                            isVideo = false,
                            mimeType = mimeType,
                            albumName = albumName,
                            bucketId = bucketId,
                            volumeName = volumeName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchVideosFallback(volumeName: String, output: MutableList<MediaItem>) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val uri = try {
            MediaStore.Video.Media.getContentUri(volumeName)
        } catch (e: Exception) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = if (idCol != -1) cursor.getLong(idCol) else continue
                    val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Video_$id" else "Video_$id"
                    val path = if (pathCol != -1) cursor.getString(pathCol) ?: "" else ""
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L

                    val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else 0L
                    val dateModified = if (dateModifiedCol != -1) cursor.getLong(dateModifiedCol) else 0L
                    val dateTaken = if (dateTakenCol != -1) cursor.getLong(dateTakenCol) else 0L

                    val effectiveDateSec = calculateEffectiveDateSec(dateTaken, dateModified, dateAdded)
                    val effectiveDateTakenMs = if (dateTaken != 0L) (if (kotlin.math.abs(dateTaken) >= 100_000_000_000L) dateTaken else dateTaken * 1000L) else effectiveDateSec * 1000L

                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
                    val duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val mimeType = if (mimeCol != -1) cursor.getString(mimeCol) ?: "video/*" else "video/*"
                    var bucketId = if (bucketIdCol != -1) cursor.getString(bucketIdCol) ?: "" else ""
                    var albumName = if (bucketCol != -1) cursor.getString(bucketCol) ?: "" else ""

                    if (albumName.isBlank() && path.isNotBlank()) {
                        val parentFile = File(path).parentFile
                        if (parentFile != null && parentFile.name.isNotBlank()) {
                            albumName = parentFile.name
                            if (bucketId.isBlank()) {
                                bucketId = parentFile.absolutePath.hashCode().toString()
                            }
                        }
                    }
                    if (albumName.isBlank()) albumName = "Other"
                    if (bucketId.isBlank()) bucketId = albumName

                    val contentUri = ContentUris.withAppendedId(uri, id)

                    output.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            name = name,
                            path = path,
                            size = size,
                            dateAddedSec = effectiveDateSec,
                            dateTakenMs = effectiveDateTakenMs,
                            width = width,
                            height = height,
                            durationMs = duration,
                            isVideo = true,
                            mimeType = mimeType,
                            albumName = albumName,
                            bucketId = bucketId,
                            volumeName = volumeName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
