package com.tkno.gallery.ui.screens.editor

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.tkno.gallery.data.model.MediaItem
import com.tkno.gallery.ui.components.CustomIcons
import com.tkno.gallery.util.FilterType
import com.tkno.gallery.util.ImageFilterUtils
import kotlinx.coroutines.launch

private data class PhotoEditorHistoryState(
    val rotationAngle: Float = 0f,
    val selectedFilter: FilterType = FilterType.NONE,
    val cropRectNormalized: RectF? = null
)

@Composable
fun PhotoEditorScreen(
    mediaItem: MediaItem,
    onNavigateBack: () -> Unit,
    onSaveSuccess: (Uri) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val history = remember { mutableStateListOf(PhotoEditorHistoryState()) }
    var historyIndex by remember { mutableIntStateOf(0) }

    val currentState = history.getOrElse(historyIndex) { PhotoEditorHistoryState() }
    val rotationAngle = currentState.rotationAngle
    val selectedFilter = currentState.selectedFilter
    val cropRectNormalized = currentState.cropRectNormalized

    val canUndo = historyIndex > 0
    val canRedo = historyIndex < history.size - 1

    fun pushState(newState: PhotoEditorHistoryState) {
        if (newState == currentState) return
        while (history.size > historyIndex + 1) {
            history.removeAt(history.size - 1)
        }
        history.add(newState)
        historyIndex = history.size - 1
    }

    fun undo() {
        if (canUndo) {
            historyIndex--
        }
    }

    fun redo() {
        if (canRedo) {
            historyIndex++
        }
    }

    var isCropMode by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val hasChanges = remember(rotationAngle, selectedFilter, cropRectNormalized) {
        val normalizedAngle = ((rotationAngle % 360f) + 360f) % 360f
        normalizedAngle != 0f || selectedFilter != FilterType.NONE || cropRectNormalized != null
    }

    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "rotationAnimation"
    )

    // Sampled high-res bitmap for main editor & crop tool
    var editorBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(mediaItem.uri) {
        editorBitmap = ImageFilterUtils.decodeSampledBitmapFromUri(
            context = context,
            uri = mediaItem.uri,
            reqWidth = 2048,
            reqHeight = 2048
        )
    }

    // Sampled small thumbnail for filter preview chips
    var previewThumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(mediaItem.uri) {
        previewThumbnailBitmap = ImageFilterUtils.decodeSampledBitmapFromUri(
            context = context,
            uri = mediaItem.uri,
            reqWidth = 140,
            reqHeight = 140
        )
    }

    // Cropped sub-bitmap for main screen preview if crop is applied
    val displayBitmap = remember(editorBitmap, cropRectNormalized) {
        val bmp = editorBitmap
        val crop = cropRectNormalized
        if (bmp == null) null
        else if (crop == null) bmp
        else {
            val left = (crop.left.coerceIn(0f, 1f) * bmp.width).toInt()
            val top = (crop.top.coerceIn(0f, 1f) * bmp.height).toInt()
            val right = (crop.right.coerceIn(0f, 1f) * bmp.width).toInt()
            val bottom = (crop.bottom.coerceIn(0f, 1f) * bmp.height).toInt()
            val w = (right - left).coerceAtLeast(1).coerceAtMost(bmp.width - left)
            val h = (bottom - top).coerceAtLeast(1).coerceAtMost(bmp.height - top)
            Bitmap.createBitmap(bmp, left, top, w, h)
        }
    }

    val uncroppedIntrinsicSize = remember(editorBitmap, mediaItem) {
        val bmp = editorBitmap
        if (bmp != null) {
            Size(bmp.width.toFloat(), bmp.height.toFloat())
        } else if (mediaItem.width > 0 && mediaItem.height > 0) {
            Size(mediaItem.width.toFloat(), mediaItem.height.toFloat())
        } else {
            Size.Zero
        }
    }

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val imageIntrinsicSize = remember(displayBitmap, mediaItem) {
        if (displayBitmap != null) {
            Size(displayBitmap.width.toFloat(), displayBitmap.height.toFloat())
        } else if (mediaItem.width > 0 && mediaItem.height > 0) {
            Size(mediaItem.width.toFloat(), mediaItem.height.toFloat())
        } else {
            Size.Zero
        }
    }

    val targetScale by remember(canvasSize, imageIntrinsicSize, rotationAngle) {
        derivedStateOf {
            if (canvasSize.width <= 0f || canvasSize.height <= 0f || imageIntrinsicSize.width <= 0f || imageIntrinsicSize.height <= 0f) {
                1f
            } else {
                val cw = canvasSize.width
                val ch = canvasSize.height
                val iw = imageIntrinsicSize.width
                val ih = imageIntrinsicSize.height

                val scaleFit = kotlin.math.min(cw / iw, ch / ih)
                val baseW = iw * scaleFit
                val baseH = ih * scaleFit

                val rad = Math.toRadians(rotationAngle.toDouble())
                val cos = kotlin.math.abs(kotlin.math.cos(rad)).toFloat()
                val sin = kotlin.math.abs(kotlin.math.sin(rad)).toFloat()

                val rotatedBoundingW = baseW * cos + baseH * sin
                val rotatedBoundingH = baseW * sin + baseH * cos

                if (rotatedBoundingW > cw || rotatedBoundingH > ch) {
                    kotlin.math.min(cw / rotatedBoundingW, ch / rotatedBoundingH)
                } else {
                    1f
                }
            }
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scaleAnimation"
    )

    BackHandler {
        if (isCropMode) {
            isCropMode = false
        } else if (hasChanges) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Image Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .onGloballyPositioned { coordinates ->
                        canvasSize = coordinates.size.toSize()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!isCropMode) {
                    val composeMatrix = selectedFilter.getComposeColorMatrix()
                    val colorFilter = composeMatrix?.let { ColorFilter.colorMatrix(it) }

                    if (displayBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = displayBitmap.asImageBitmap(),
                            contentDescription = "Editing Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationZ = animatedRotation
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                },
                            colorFilter = colorFilter,
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(mediaItem.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Editing Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationZ = animatedRotation
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                },
                            colorFilter = colorFilter,
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    // Interactive Crop View Overlay
                    CropOverlayView(
                        imageUri = mediaItem.uri,
                        imageBitmap = editorBitmap,
                        initialImageIntrinsicSize = uncroppedIntrinsicSize,
                        rotationAngle = rotationAngle,
                        selectedFilter = selectedFilter,
                        initialCropRect = cropRectNormalized,
                        onCropApplied = { newCrop ->
                            pushState(currentState.copy(cropRectNormalized = newCrop))
                            isCropMode = false
                        },
                        onCancelCrop = {
                            isCropMode = false
                        }
                    )
                }
            }

            if (!isCropMode) {
                // Action Buttons Row: Rotate Capsule, Crop Capsule, Undo Circle, Redo Circle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotate Button (Capsule Shape)
                    Surface(
                        onClick = {
                            pushState(currentState.copy(rotationAngle = rotationAngle - 90f))
                        },
                        shape = CircleShape,
                        color = Color(0xFF1E1E1E),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                                contentDescription = "Rotate",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Rotate",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Crop Button (Capsule Shape)
                    Surface(
                        onClick = {
                            isCropMode = true
                        },
                        shape = CircleShape,
                        color = Color(0xFF1E1E1E),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Crop,
                                contentDescription = "Crop",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Crop",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Undo Button (Circle Shape, Height = 52.dp)
                    Surface(
                        onClick = { undo() },
                        enabled = canUndo,
                        shape = CircleShape,
                        color = Color(0xFF1E1E1E),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = CustomIcons.Undo,
                                contentDescription = "Undo",
                                tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Redo Button (Circle Shape, Height = 52.dp)
                    Surface(
                        onClick = { redo() },
                        enabled = canRedo,
                        shape = CircleShape,
                        color = Color(0xFF1E1E1E),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = CustomIcons.Redo,
                                contentDescription = "Redo",
                                tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Carousel Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allFilters = FilterType.values()
                    allFilters.forEachIndexed { index, filter ->
                        FilterThumbnailItem(
                            filter = filter,
                            isSelected = (selectedFilter == filter),
                            previewBitmap = previewThumbnailBitmap,
                            imageUri = mediaItem.uri,
                            onClick = {
                                pushState(currentState.copy(selectedFilter = filter))
                            }
                        )

                        // Vertical separator after Vivid (between index 1 and 2)
                        if (index == 1) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .width(1.dp)
                                    .height(44.dp)
                                    .background(Color.White.copy(alpha = 0.25f))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Bar (Cancel & Save copy)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel Button
                    TextButton(
                        onClick = {
                            if (hasChanges) {
                                showDiscardDialog = true
                            } else {
                                onNavigateBack()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }

                    // Save Copy Button
                    Button(
                        onClick = {
                            if (!hasChanges || isSaving) return@Button
                            isSaving = true
                            coroutineScope.launch {
                                val savedUri = ImageFilterUtils.saveEditedImage(
                                    context = context,
                                    originalUri = mediaItem.uri,
                                    originalName = mediaItem.name,
                                    rotationAngle = rotationAngle,
                                    cropRectNormalized = cropRectNormalized,
                                    filterType = selectedFilter
                                )
                                isSaving = false
                                if (savedUri != null) {
                                    Toast.makeText(context, "Saved copy to Gallery", Toast.LENGTH_SHORT).show()
                                    onSaveSuccess(savedUri)
                                } else {
                                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = hasChanges && !isSaving,
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA8C7FA),
                            contentColor = Color(0xFF062E6F),
                            disabledContainerColor = Color(0xFF1E1E1E),
                            disabledContentColor = Color.White.copy(alpha = 0.35f)
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF062E6F),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "Save copy",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Discard Changes Dialog
        if (showDiscardDialog) {
            DiscardChangesDialog(
                onKeepEditing = { showDiscardDialog = false },
                onDiscard = {
                    showDiscardDialog = false
                    onNavigateBack()
                }
            )
        }
    }
}

@Composable
private fun FilterThumbnailItem(
    filter: FilterType,
    isSelected: Boolean,
    previewBitmap: Bitmap?,
    imageUri: Uri,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val composeMatrix = remember(filter) { filter.getComposeColorMatrix() }
    val colorFilter = remember(composeMatrix) {
        composeMatrix?.let { ColorFilter.colorMatrix(it) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            border = BorderStroke(2.dp, Color.White),
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = filter.displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    colorFilter = colorFilter,
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .size(120)
                        .crossfade(true)
                        .build(),
                    contentDescription = filter.displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    colorFilter = colorFilter,
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = filter.displayName,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DiscardChangesDialog(
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit
) {
    Dialog(onDismissRequest = onKeepEditing) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF2C2D31),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Discard changes?",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Your changes won't be saved",
                    color = Color(0xFFC4C7C5),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(26.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Keep Editing button (Capsule Shape)
                    Surface(
                        onClick = onKeepEditing,
                        shape = CircleShape,
                        color = Color(0xFF383A42),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Keep Editing",
                                color = Color(0xFFA8C7FA),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Discard button (Capsule Shape)
                    Surface(
                        onClick = onDiscard,
                        shape = CircleShape,
                        color = Color(0xFFA8C7FA),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Discard",
                                color = Color(0xFF062E6F),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class CropDragHandle {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    EDGE_TOP,
    EDGE_BOTTOM,
    EDGE_LEFT,
    EDGE_RIGHT,
    CENTER
}

@Composable
private fun CropOverlayView(
    imageUri: Uri,
    imageBitmap: Bitmap?,
    initialImageIntrinsicSize: Size,
    rotationAngle: Float,
    selectedFilter: FilterType,
    initialCropRect: RectF?,
    onCropApplied: (RectF?) -> Unit,
    onCancelCrop: () -> Unit
) {
    val context = LocalContext.current
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var imageIntrinsicSize by remember(initialImageIntrinsicSize, imageBitmap) {
        mutableStateOf(
            if (initialImageIntrinsicSize.width > 0f && initialImageIntrinsicSize.height > 0f) {
                initialImageIntrinsicSize
            } else if (imageBitmap != null && imageBitmap.width > 0 && imageBitmap.height > 0) {
                Size(imageBitmap.width.toFloat(), imageBitmap.height.toFloat())
            } else {
                Size.Zero
            }
        )
    }

    LaunchedEffect(initialImageIntrinsicSize, imageBitmap) {
        if (initialImageIntrinsicSize.width > 0f && initialImageIntrinsicSize.height > 0f) {
            imageIntrinsicSize = initialImageIntrinsicSize
        } else if (imageBitmap != null && imageBitmap.width > 0 && imageBitmap.height > 0) {
            imageIntrinsicSize = Size(imageBitmap.width.toFloat(), imageBitmap.height.toFloat())
        }
    }

    val composeMatrix = remember(selectedFilter) { selectedFilter.getComposeColorMatrix() }
    val colorFilter = remember(composeMatrix) {
        composeMatrix?.let { ColorFilter.colorMatrix(it) }
    }

    // Normalized rotation
    val normalizedRotation = ((rotationAngle % 360f) + 360f) % 360f
    val is90or270 = normalizedRotation == 90f || normalizedRotation == 270f

    val rawW = imageIntrinsicSize.width.takeIf { it > 0f }
        ?: (if (imageBitmap != null) imageBitmap.width.toFloat() else 1080f)
    val rawH = imageIntrinsicSize.height.takeIf { it > 0f }
        ?: (if (imageBitmap != null) imageBitmap.height.toFloat() else 1920f)
    val orientedW = if (is90or270) rawH else rawW
    val orientedH = if (is90or270) rawW else rawH

    val targetScale by remember(containerSize, imageIntrinsicSize, rotationAngle) {
        derivedStateOf {
            if (containerSize.width <= 0f || containerSize.height <= 0f || imageIntrinsicSize.width <= 0f || imageIntrinsicSize.height <= 0f) {
                1f
            } else {
                val cw = containerSize.width
                val ch = containerSize.height
                val iw = imageIntrinsicSize.width
                val ih = imageIntrinsicSize.height

                val scaleFit = kotlin.math.min(cw / iw, ch / ih)
                val baseW = iw * scaleFit
                val baseH = ih * scaleFit

                val rad = Math.toRadians(rotationAngle.toDouble())
                val cos = kotlin.math.abs(kotlin.math.cos(rad)).toFloat()
                val sin = kotlin.math.abs(kotlin.math.sin(rad)).toFloat()

                val rotatedBoundingW = baseW * cos + baseH * sin
                val rotatedBoundingH = baseW * sin + baseH * cos

                if (rotatedBoundingW > cw || rotatedBoundingH > ch) {
                    kotlin.math.min(cw / rotatedBoundingW, ch / rotatedBoundingH)
                } else {
                    1f
                }
            }
        }
    }

    // Normalized crop rect: left, top, right, bottom in 0f..1f relative to the oriented image
    // Defaults to 0f, 0f, 1f, 1f (exact full size of original image in Free mode)
    var cropLeft by remember { mutableFloatStateOf(initialCropRect?.left ?: 0f) }
    var cropTop by remember { mutableFloatStateOf(initialCropRect?.top ?: 0f) }
    var cropRight by remember { mutableFloatStateOf(initialCropRect?.right ?: 1f) }
    var cropBottom by remember { mutableFloatStateOf(initialCropRect?.bottom ?: 1f) }

    var selectedAspectRatio by remember { mutableStateOf("Free") }

    // Active drag state
    var activeHandle by remember { mutableStateOf(CropDragHandle.NONE) }
    var dragStartLeft by remember { mutableFloatStateOf(0f) }
    var dragStartTop by remember { mutableFloatStateOf(0f) }
    var dragStartRight by remember { mutableFloatStateOf(1f) }
    var dragStartBottom by remember { mutableFloatStateOf(1f) }
    var accumulatedDx by remember { mutableFloatStateOf(0f) }
    var accumulatedDy by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Crop Area Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .onGloballyPositioned { coordinates ->
                    containerSize = coordinates.size.toSize()
                },
            contentAlignment = Alignment.Center
        ) {
            // 1. Crisp Vibrant Original Image Layer (100% true colors, hardware accelerated)
            if (imageBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = imageBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = rotationAngle
                            scaleX = targetScale
                            scaleY = targetScale
                        },
                    colorFilter = colorFilter,
                    contentScale = ContentScale.Fit
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    onSuccess = { state ->
                        val painter = state.painter
                        if (painter.intrinsicSize.width > 0 && painter.intrinsicSize.height > 0) {
                            imageIntrinsicSize = painter.intrinsicSize
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = rotationAngle
                            scaleX = targetScale
                            scaleY = targetScale
                        },
                    colorFilter = colorFilter,
                    contentScale = ContentScale.Fit
                )
            }

            // 2. Interactive Crop Overlay Canvas
            if (containerSize.width > 0f && containerSize.height > 0f) {
                val cw = containerSize.width
                val ch = containerSize.height

                val scaleFit = kotlin.math.min(cw / rawW, ch / rawH)
                val baseW = rawW * scaleFit
                val baseH = rawH * scaleFit

                val displayedW = (if (is90or270) baseH else baseW) * targetScale
                val displayedH = (if (is90or270) baseW else baseH) * targetScale
                val imgLeft = (cw - displayedW) / 2f
                val imgTop = (ch - displayedH) / 2f

                val boxLeft = imgLeft + cropLeft * displayedW
                val boxTop = imgTop + cropTop * displayedH
                val boxRight = imgLeft + cropRight * displayedW
                val boxBottom = imgTop + cropBottom * displayedH
                val boxWidth = (boxRight - boxLeft).coerceAtLeast(1f)
                val boxHeight = (boxBottom - boxTop).coerceAtLeast(1f)

                val currentCropLeft by rememberUpdatedState(cropLeft)
                val currentCropTop by rememberUpdatedState(cropTop)
                val currentCropRight by rememberUpdatedState(cropRight)
                val currentCropBottom by rememberUpdatedState(cropBottom)
                val currentDisplayedW by rememberUpdatedState(displayedW)
                val currentDisplayedH by rememberUpdatedState(displayedH)
                val currentImgLeft by rememberUpdatedState(imgLeft)
                val currentImgTop by rememberUpdatedState(imgTop)

                val density = LocalDensity.current
                val cornerTolerance = with(density) { 48.dp.toPx() }
                val edgeTolerance = with(density) { 36.dp.toPx() }

                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val curBoxL = currentImgLeft + currentCropLeft * currentDisplayedW
                                    val curBoxT = currentImgTop + currentCropTop * currentDisplayedH
                                    val curBoxR = currentImgLeft + currentCropRight * currentDisplayedW
                                    val curBoxB = currentImgTop + currentCropBottom * currentDisplayedH
                                    val x = offset.x
                                    val y = offset.y

                                    activeHandle = when {
                                        // 1. Corners (highest priority)
                                        kotlin.math.hypot(x - curBoxL, y - curBoxT) <= cornerTolerance -> CropDragHandle.TOP_LEFT
                                        kotlin.math.hypot(x - curBoxR, y - curBoxT) <= cornerTolerance -> CropDragHandle.TOP_RIGHT
                                        kotlin.math.hypot(x - curBoxL, y - curBoxB) <= cornerTolerance -> CropDragHandle.BOTTOM_LEFT
                                        kotlin.math.hypot(x - curBoxR, y - curBoxB) <= cornerTolerance -> CropDragHandle.BOTTOM_RIGHT

                                        // 2. Edges
                                        kotlin.math.abs(y - curBoxT) <= edgeTolerance && x >= curBoxL - edgeTolerance && x <= curBoxR + edgeTolerance -> CropDragHandle.EDGE_TOP
                                        kotlin.math.abs(y - curBoxB) <= edgeTolerance && x >= curBoxL - edgeTolerance && x <= curBoxR + edgeTolerance -> CropDragHandle.EDGE_BOTTOM
                                        kotlin.math.abs(x - curBoxL) <= edgeTolerance && y >= curBoxT - edgeTolerance && y <= curBoxB + edgeTolerance -> CropDragHandle.EDGE_LEFT
                                        kotlin.math.abs(x - curBoxR) <= edgeTolerance && y >= curBoxT - edgeTolerance && y <= curBoxB + edgeTolerance -> CropDragHandle.EDGE_RIGHT

                                        // 3. Center pan
                                        x in curBoxL..curBoxR && y in curBoxT..curBoxB -> CropDragHandle.CENTER

                                        else -> CropDragHandle.NONE
                                    }

                                    dragStartLeft = currentCropLeft
                                    dragStartTop = currentCropTop
                                    dragStartRight = currentCropRight
                                    dragStartBottom = currentCropBottom
                                    accumulatedDx = 0f
                                    accumulatedDy = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    val dw = currentDisplayedW
                                    val dh = currentDisplayedH
                                    if (activeHandle != CropDragHandle.NONE && dw > 0f && dh > 0f) {
                                        change.consume()
                                        accumulatedDx += dragAmount.x
                                        accumulatedDy += dragAmount.y

                                        val totalDx = accumulatedDx / dw
                                        val totalDy = accumulatedDy / dh
                                        val minW = (40.dp.toPx() / dw).coerceIn(0.01f, 0.4f)
                                        val minH = (40.dp.toPx() / dh).coerceIn(0.01f, 0.4f)

                                        when (activeHandle) {
                                            CropDragHandle.TOP_LEFT -> {
                                                cropLeft = (dragStartLeft + totalDx).coerceIn(0f, dragStartRight - minW)
                                                cropTop = (dragStartTop + totalDy).coerceIn(0f, dragStartBottom - minH)
                                            }
                                            CropDragHandle.TOP_RIGHT -> {
                                                cropRight = (dragStartRight + totalDx).coerceIn(dragStartLeft + minW, 1f)
                                                cropTop = (dragStartTop + totalDy).coerceIn(0f, dragStartBottom - minH)
                                            }
                                            CropDragHandle.BOTTOM_LEFT -> {
                                                cropLeft = (dragStartLeft + totalDx).coerceIn(0f, dragStartRight - minW)
                                                cropBottom = (dragStartBottom + totalDy).coerceIn(dragStartTop + minH, 1f)
                                            }
                                            CropDragHandle.BOTTOM_RIGHT -> {
                                                cropRight = (dragStartRight + totalDx).coerceIn(dragStartLeft + minW, 1f)
                                                cropBottom = (dragStartBottom + totalDy).coerceIn(dragStartTop + minH, 1f)
                                            }
                                            CropDragHandle.EDGE_TOP -> {
                                                cropTop = (dragStartTop + totalDy).coerceIn(0f, dragStartBottom - minH)
                                            }
                                            CropDragHandle.EDGE_BOTTOM -> {
                                                cropBottom = (dragStartBottom + totalDy).coerceIn(dragStartTop + minH, 1f)
                                            }
                                            CropDragHandle.EDGE_LEFT -> {
                                                cropLeft = (dragStartLeft + totalDx).coerceIn(0f, dragStartRight - minW)
                                            }
                                            CropDragHandle.EDGE_RIGHT -> {
                                                cropRight = (dragStartRight + totalDx).coerceIn(dragStartLeft + minW, 1f)
                                            }
                                            CropDragHandle.CENTER -> {
                                                val curW = dragStartRight - dragStartLeft
                                                val curH = dragStartBottom - dragStartTop
                                                val newL = (dragStartLeft + totalDx).coerceIn(0f, 1f - curW)
                                                val newT = (dragStartTop + totalDy).coerceIn(0f, 1f - curH)
                                                cropLeft = newL
                                                cropRight = newL + curW
                                                cropTop = newT
                                                cropBottom = newT + curH
                                            }
                                            CropDragHandle.NONE -> {}
                                        }
                                    }
                                },
                                onDragEnd = { activeHandle = CropDragHandle.NONE },
                                onDragCancel = { activeHandle = CropDragHandle.NONE }
                            )
                        }
                ) {
                    // 1. Semi-transparent dark overlay outside crop box ONLY (Inside crop box is 100% clear and vibrant)
                    val overlayColor = Color.Black.copy(alpha = 0.58f)

                    // Top outer area
                    if (boxTop > 0f) {
                        drawRect(
                            color = overlayColor,
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width, boxTop.coerceAtMost(size.height))
                        )
                    }
                    // Bottom outer area
                    if (boxBottom < size.height) {
                        drawRect(
                            color = overlayColor,
                            topLeft = Offset(0f, boxBottom.coerceAtLeast(0f)),
                            size = Size(size.width, (size.height - boxBottom).coerceAtLeast(0f))
                        )
                    }
                    // Left outer area (between boxTop and boxBottom)
                    if (boxLeft > 0f && boxHeight > 0f) {
                        drawRect(
                            color = overlayColor,
                            topLeft = Offset(0f, boxTop.coerceAtLeast(0f)),
                            size = Size(boxLeft.coerceAtMost(size.width), boxHeight)
                        )
                    }
                    // Right outer area (between boxTop and boxBottom)
                    if (boxRight < size.width && boxHeight > 0f) {
                        drawRect(
                            color = overlayColor,
                            topLeft = Offset(boxRight.coerceAtLeast(0f), boxTop.coerceAtLeast(0f)),
                            size = Size((size.width - boxRight).coerceAtLeast(0f), boxHeight)
                        )
                    }

                    // 2. Crop Window Outline
                    drawRect(
                        color = Color.White.copy(alpha = 0.9f),
                        topLeft = Offset(boxLeft, boxTop),
                        size = Size(boxWidth, boxHeight),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // 3. Rule of thirds grid lines
                    val oneThirdW = boxWidth / 3f
                    val oneThirdH = boxHeight / 3f

                    // Verticals
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(boxLeft + oneThirdW, boxTop), Offset(boxLeft + oneThirdW, boxBottom), strokeWidth = 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(boxLeft + 2 * oneThirdW, boxTop), Offset(boxLeft + 2 * oneThirdW, boxBottom), strokeWidth = 1.dp.toPx())

                    // Horizontals
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(boxLeft, boxTop + oneThirdH), Offset(boxRight, boxTop + oneThirdH), strokeWidth = 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(boxLeft, boxTop + 2 * oneThirdH), Offset(boxRight, boxTop + 2 * oneThirdH), strokeWidth = 1.dp.toPx())

                    // 4. Corner L-Brackets
                    val cornerLen = 22.dp.toPx()
                    val cornerStroke = 3.5.dp.toPx()

                    // Top-Left
                    drawLine(Color.White, Offset(boxLeft - 1.dp.toPx(), boxTop), Offset(boxLeft + cornerLen, boxTop), cornerStroke)
                    drawLine(Color.White, Offset(boxLeft, boxTop - 1.dp.toPx()), Offset(boxLeft, boxTop + cornerLen), cornerStroke)

                    // Top-Right
                    drawLine(Color.White, Offset(boxRight + 1.dp.toPx(), boxTop), Offset(boxRight - cornerLen, boxTop), cornerStroke)
                    drawLine(Color.White, Offset(boxRight, boxTop - 1.dp.toPx()), Offset(boxRight, boxTop + cornerLen), cornerStroke)

                    // Bottom-Left
                    drawLine(Color.White, Offset(boxLeft - 1.dp.toPx(), boxBottom), Offset(boxLeft + cornerLen, boxBottom), cornerStroke)
                    drawLine(Color.White, Offset(boxLeft, boxBottom + 1.dp.toPx()), Offset(boxLeft, boxBottom - cornerLen), cornerStroke)

                    // Bottom-Right
                    drawLine(Color.White, Offset(boxRight + 1.dp.toPx(), boxBottom), Offset(boxRight - cornerLen, boxBottom), cornerStroke)
                    drawLine(Color.White, Offset(boxRight, boxBottom - 1.dp.toPx()), Offset(boxRight, boxBottom - cornerLen), cornerStroke)

                    // 5. Edge Midpoint Handles
                    val handleLen = 18.dp.toPx()
                    val handleStroke = 2.5.dp.toPx()
                    val midX = (boxLeft + boxRight) / 2f
                    val midY = (boxTop + boxBottom) / 2f

                    drawLine(Color.White, Offset(midX - handleLen / 2f, boxTop), Offset(midX + handleLen / 2f, boxTop), handleStroke)
                    drawLine(Color.White, Offset(midX - handleLen / 2f, boxBottom), Offset(midX + handleLen / 2f, boxBottom), handleStroke)
                    drawLine(Color.White, Offset(boxLeft, midY - handleLen / 2f), Offset(boxLeft, midY + handleLen / 2f), handleStroke)
                    drawLine(Color.White, Offset(boxRight, midY - handleLen / 2f), Offset(boxRight, midY + handleLen / 2f), handleStroke)
                }
            }
        }

        // Aspect Ratio Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val ratios = listOf("Free", "Original", "1:1", "4:3", "3:4", "16:9", "9:16")
            ratios.forEach { ratio ->
                val isSelected = selectedAspectRatio == ratio
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedAspectRatio = ratio
                        val imgRatio = if (orientedH > 0f) orientedW / orientedH else 1f
                        val targetRatio: Float? = when (ratio) {
                            "Free" -> null
                            "Original" -> imgRatio
                            "1:1" -> 1f
                            "4:3" -> 4f / 3f
                            "3:4" -> 3f / 4f
                            "16:9" -> 16f / 9f
                            "9:16" -> 9f / 16f
                            else -> null
                        }

                        if (targetRatio == null) {
                            // Free: reset to full original image size
                            cropLeft = 0f
                            cropTop = 0f
                            cropRight = 1f
                            cropBottom = 1f
                        } else {
                            val rNorm = targetRatio / imgRatio
                            val (normW, normH) = if (rNorm <= 1f) {
                                rNorm to 1f
                            } else {
                                1f to (1f / rNorm)
                            }
                            val newLeft = (1f - normW) / 2f
                            val newTop = (1f - normH) / 2f
                            cropLeft = newLeft.coerceIn(0f, 1f)
                            cropTop = newTop.coerceIn(0f, 1f)
                            cropRight = (newLeft + normW).coerceIn(0f, 1f)
                            cropBottom = (newTop + normH).coerceIn(0f, 1f)
                        }
                    },
                    label = { Text(ratio, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF1E1E1E),
                        labelColor = Color.White
                    )
                )
            }
        }

        // Crop Actions (Cancel & Apply)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancelCrop) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cancel Crop",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = {
                    val isFull = cropLeft <= 0.001f && cropTop <= 0.001f && cropRight >= 0.999f && cropBottom >= 0.999f
                    if (isFull) {
                        onCropApplied(null)
                    } else {
                        onCropApplied(RectF(cropLeft, cropTop, cropRight, cropBottom))
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Apply Crop",
                    tint = Color(0xFFA8C7FA),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
