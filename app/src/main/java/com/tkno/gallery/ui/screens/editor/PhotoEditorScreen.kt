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
import com.tkno.gallery.util.FilterType
import com.tkno.gallery.util.ImageFilterUtils
import kotlinx.coroutines.launch

@Composable
fun PhotoEditorScreen(
    mediaItem: MediaItem,
    onNavigateBack: () -> Unit,
    onSaveSuccess: (Uri) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var selectedFilter by remember { mutableStateOf(FilterType.NONE) }
    var cropRectNormalized by remember { mutableStateOf<RectF?>(null) }

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

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var imageIntrinsicSize by remember {
        mutableStateOf(
            if (mediaItem.width > 0 && mediaItem.height > 0)
                Size(mediaItem.width.toFloat(), mediaItem.height.toFloat())
            else Size.Zero
        )
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

    // Load sample thumbnail bitmap for filter previews
    var previewThumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(mediaItem.uri) {
        previewThumbnailBitmap = ImageFilterUtils.decodeSampledBitmapFromUri(
            context = context,
            uri = mediaItem.uri,
            reqWidth = 140,
            reqHeight = 140
        )
    }

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

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(mediaItem.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Editing Image",
                        onSuccess = { state ->
                            val painter = state.painter
                            if (painter.intrinsicSize.width > 0 && painter.intrinsicSize.height > 0) {
                                imageIntrinsicSize = painter.intrinsicSize
                            }
                        },
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
                    // Interactive Crop View Overlay
                    CropOverlayView(
                        imageUri = mediaItem.uri,
                        rotationAngle = rotationAngle,
                        initialCropRect = cropRectNormalized,
                        onCropApplied = { newCrop ->
                            cropRectNormalized = newCrop
                            isCropMode = false
                        },
                        onCancelCrop = {
                            isCropMode = false
                        }
                    )
                }
            }

            if (!isCropMode) {
                // Action Buttons (Rotate & Crop) - Pill Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Rotate Button
                    Surface(
                        onClick = {
                            rotationAngle = (rotationAngle - 90f) % 360f
                        },
                        shape = RoundedCornerShape(16.dp),
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
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Rotate",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Crop Button
                    Surface(
                        onClick = {
                            isCropMode = true
                        },
                        shape = RoundedCornerShape(16.dp),
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
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Crop",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
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
                            onClick = { selectedFilter = filter }
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

        // Discard Changes Dialog (Exact match to Screenshot 2)
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
                    // Keep Editing button
                    Surface(
                        onClick = onKeepEditing,
                        shape = RoundedCornerShape(14.dp),
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

                    // Discard button
                    Surface(
                        onClick = onDiscard,
                        shape = RoundedCornerShape(14.dp),
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

@Composable
private fun CropOverlayView(
    imageUri: Uri,
    rotationAngle: Float,
    initialCropRect: RectF?,
    onCropApplied: (RectF) -> Unit,
    onCancelCrop: () -> Unit
) {
    val context = LocalContext.current
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var imageIntrinsicSize by remember { mutableStateOf(Size.Zero) }

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

    // Normalized crop rect: left, top, right, bottom in 0f..1f
    var cropLeft by remember { mutableFloatStateOf(initialCropRect?.left ?: 0.05f) }
    var cropTop by remember { mutableFloatStateOf(initialCropRect?.top ?: 0.05f) }
    var cropRight by remember { mutableFloatStateOf(initialCropRect?.right ?: 0.95f) }
    var cropBottom by remember { mutableFloatStateOf(initialCropRect?.bottom ?: 0.95f) }

    var selectedAspectRatio by remember { mutableStateOf("Free") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Crop Area Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    containerSize = coordinates.size.toSize()
                },
            contentAlignment = Alignment.Center
        ) {
            // Background Image
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUri)
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
                contentScale = ContentScale.Fit
            )

            // Crop Overlay Box & Grid
            if (containerSize.width > 0 && containerSize.height > 0) {
                val boxLeft = cropLeft * containerSize.width
                val boxTop = cropTop * containerSize.height
                val boxRight = cropRight * containerSize.width
                val boxBottom = cropBottom * containerSize.height
                val boxWidth = (boxRight - boxLeft).coerceAtLeast(40f)
                val boxHeight = (boxBottom - boxTop).coerceAtLeast(40f)

                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(containerSize) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x / containerSize.width
                                val dy = dragAmount.y / containerSize.height

                                val curWidth = cropRight - cropLeft
                                val curHeight = cropBottom - cropTop

                                var newLeft = (cropLeft + dx).coerceIn(0f, 1f - curWidth)
                                var newTop = (cropTop + dy).coerceIn(0f, 1f - curHeight)
                                var newRight = newLeft + curWidth
                                var newBottom = newTop + curHeight

                                cropLeft = newLeft
                                cropTop = newTop
                                cropRight = newRight
                                cropBottom = newBottom
                            }
                        }
                ) {
                    val overlayPath = Path().apply {
                        addRect(Rect(0f, 0f, size.width, size.height))
                        addRect(Rect(boxLeft, boxTop, boxRight, boxBottom))
                        fillType = PathFillType.EvenOdd
                    }
                    drawPath(overlayPath, Color.Black.copy(alpha = 0.55f))

                    // Border of crop window
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(boxLeft, boxTop),
                        size = Size(boxWidth, boxHeight),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Rule of thirds grid lines
                    val oneThirdW = boxWidth / 3f
                    val oneThirdH = boxHeight / 3f

                    // Verticals
                    drawLine(Color.White.copy(alpha = 0.4f), Offset(boxLeft + oneThirdW, boxTop), Offset(boxLeft + oneThirdW, boxBottom), strokeWidth = 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.4f), Offset(boxLeft + 2 * oneThirdW, boxTop), Offset(boxLeft + 2 * oneThirdW, boxBottom), strokeWidth = 1.dp.toPx())

                    // Horizontals
                    drawLine(Color.White.copy(alpha = 0.4f), Offset(boxLeft, boxTop + oneThirdH), Offset(boxRight, boxTop + oneThirdH), strokeWidth = 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.4f), Offset(boxLeft, boxTop + 2 * oneThirdH), Offset(boxRight, boxTop + 2 * oneThirdH), strokeWidth = 1.dp.toPx())

                    // Corner indicators
                    val cornerLen = 18.dp.toPx()
                    val cornerStroke = 3.5.dp.toPx()

                    // Top-Left
                    drawLine(Color.White, Offset(boxLeft, boxTop), Offset(boxLeft + cornerLen, boxTop), cornerStroke)
                    drawLine(Color.White, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + cornerLen), cornerStroke)

                    // Top-Right
                    drawLine(Color.White, Offset(boxRight, boxTop), Offset(boxRight - cornerLen, boxTop), cornerStroke)
                    drawLine(Color.White, Offset(boxRight, boxTop), Offset(boxRight, boxTop + cornerLen), cornerStroke)

                    // Bottom-Left
                    drawLine(Color.White, Offset(boxLeft, boxBottom), Offset(boxLeft + cornerLen, boxBottom), cornerStroke)
                    drawLine(Color.White, Offset(boxLeft, boxBottom), Offset(boxLeft, boxBottom - cornerLen), cornerStroke)

                    // Bottom-Right
                    drawLine(Color.White, Offset(boxRight, boxBottom), Offset(boxRight - cornerLen, boxBottom), cornerStroke)
                    drawLine(Color.White, Offset(boxRight, boxBottom), Offset(boxRight, boxBottom - cornerLen), cornerStroke)
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
            val ratios = listOf("Free", "1:1", "4:3", "3:4", "16:9", "9:16")
            ratios.forEach { ratio ->
                val isSelected = selectedAspectRatio == ratio
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedAspectRatio = ratio
                        when (ratio) {
                            "Free" -> {
                                cropLeft = 0.05f; cropTop = 0.05f; cropRight = 0.95f; cropBottom = 0.95f
                            }
                            "1:1" -> {
                                cropLeft = 0.15f; cropTop = 0.2f; cropRight = 0.85f; cropBottom = 0.85f
                            }
                            "4:3" -> {
                                cropLeft = 0.1f; cropTop = 0.2f; cropRight = 0.9f; cropBottom = 0.8f
                            }
                            "16:9" -> {
                                cropLeft = 0.05f; cropTop = 0.25f; cropRight = 0.95f; cropBottom = 0.75f
                            }
                            "9:16" -> {
                                cropLeft = 0.2f; cropTop = 0.05f; cropRight = 0.8f; cropBottom = 0.95f
                            }
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
                    onCropApplied(RectF(cropLeft, cropTop, cropRight, cropBottom))
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
