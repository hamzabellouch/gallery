package com.tkno.gallery.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat

@Composable
fun PermissionHandler(
    onPermissionsGranted: @Composable () -> Unit
) {
    val context = LocalContext.current

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    var hasPermission by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        hasPermission = permissionsResult.values.all { it }
    }

    if (hasPermission) {
        onPermissionsGranted()
    } else {
        PermissionScreenContent(
            onGrantClick = { launcher.launch(requiredPermissions) }
        )
    }
}

private data class OrbitCardData(
    val id: Int,
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val bgGradient: List<Color>,
    val angleOffsetDeg: Float
)

@Composable
private fun PermissionScreenContent(
    onGrantClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbitTransition")

    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitAngle"
    )

    val mediaCards = remember {
        listOf(
            OrbitCardData(
                id = 0,
                title = "Photos",
                icon = Icons.Default.PhotoLibrary,
                accentColor = Color(0xFF8AB4F8),
                bgGradient = listOf(Color(0xFF2D3854), Color(0xFF161F36)),
                angleOffsetDeg = 0f
            ),
            OrbitCardData(
                id = 1,
                title = "Videos",
                icon = Icons.Default.Videocam,
                accentColor = Color(0xFF80DEEA),
                bgGradient = listOf(Color(0xFF1B3C47), Color(0xFF0E232C)),
                angleOffsetDeg = 120f
            ),
            OrbitCardData(
                id = 2,
                title = "Audio",
                icon = Icons.Default.MusicNote,
                accentColor = Color(0xFFD0BCFF),
                bgGradient = listOf(Color(0xFF3C2857), Color(0xFF221538)),
                angleOffsetDeg = 240f
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E17))
    ) {
        // Ambient background glowing radial lights
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-40).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C4DFF).copy(alpha = 0.25f),
                            Color(0xFF00E5FF).copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomCenter)
                .offset(y = 60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.18f),
                            Color(0xFF7C4DFF).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Main scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 3D Orbiting Media Cards (Photos, Videos, Audio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentAlignment = Alignment.Center
            ) {
                // Calculate position parameters for each card and sort by z-index so front cards overlay back cards
                val calculatedCards = mediaCards.map { card ->
                    val currentAngleRad = Math.toRadians((orbitAngle + card.angleOffsetDeg).toDouble())
                    val sinVal = kotlin.math.sin(currentAngleRad).toFloat()
                    val cosVal = kotlin.math.cos(currentAngleRad).toFloat()

                    val offsetX = (85.dp.value * cosVal).dp
                    val offsetY = (16.dp.value * sinVal).dp
                    val scale = 0.82f + 0.22f * ((sinVal + 1f) / 2f)
                    val alpha = 0.65f + 0.35f * ((sinVal + 1f) / 2f)
                    val zIndexVal = sinVal
                    val rotationZ = 12f * cosVal

                    CardRenderInfo(
                        data = card,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        scale = scale,
                        alpha = alpha,
                        zIndexVal = zIndexVal,
                        rotationZ = rotationZ
                    )
                }.sortedBy { it.zIndexVal }

                // Render cards in z-index order
                calculatedCards.forEach { cardInfo ->
                    Surface(
                        modifier = Modifier
                            .size(130.dp, 160.dp)
                            .offset(x = cardInfo.offsetX, y = cardInfo.offsetY)
                            .rotate(cardInfo.rotationZ)
                            .zIndex(cardInfo.zIndexVal)
                            .graphicsLayer {
                                scaleX = cardInfo.scale
                                scaleY = cardInfo.scale
                                this.alpha = cardInfo.alpha
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF282638),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            cardInfo.data.accentColor.copy(alpha = 0.8f)
                        ),
                        shadowElevation = 12.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(cardInfo.data.bgGradient)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            cardInfo.data.accentColor.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = cardInfo.data.icon,
                                        contentDescription = cardInfo.data.title,
                                        tint = cardInfo.data.accentColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = cardInfo.data.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title & Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Access Your Media",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Gallery requires local storage permission to explore your photos, videos, and audio in raw resolution with ultra-fast 120Hz performance.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Bottom CTA Button (Pill shape, Taskbar Blue color)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onGrantClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color(0xFF0F0E17)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF0F0E17),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Grant Media Access",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F0E17)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF0F0E17),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class CardRenderInfo(
    val data: OrbitCardData,
    val offsetX: androidx.compose.ui.unit.Dp,
    val offsetY: androidx.compose.ui.unit.Dp,
    val scale: Float,
    val alpha: Float,
    val zIndexVal: Float,
    val rotationZ: Float
)


