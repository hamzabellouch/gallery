package com.tkno.gallery.ui.page.settings.about

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tkno.gallery.R
import com.tkno.gallery.theme.NotoSansArabicFontFamily

private val Translate: ImageVector
    get() {
        if (_translate != null) {
            return _translate!!
        }
        _translate = ImageVector.Builder(
            name = "translate",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(11.9f, 22f)
                lineTo(16.45f, 10f)
                horizontalLineToRelative(2.1f)
                lineTo(23.1f, 22f)
                horizontalLineTo(21f)
                lineTo(19.93f, 18.95f)
                horizontalLineTo(15.08f)
                lineTo(14f, 22f)
                horizontalLineTo(11.9f)
                close()
                moveTo(4f, 19f)
                lineTo(2.6f, 17.6f)
                lineTo(7.65f, 12.55f)
                quadToRelative(-0.88f, -0.88f, -1.59f, -2f)
                reflectiveQuadTo(4.75f, 8f)
                horizontalLineToRelative(2.1f)
                quadToRelative(0.5f, 0.97f, 1f, 1.7f)
                reflectiveQuadToRelative(1.2f, 1.45f)
                quadTo(9.88f, 10.33f, 10.76f, 8.84f)
                reflectiveQuadTo(12.1f, 6f)
                horizontalLineTo(1f)
                verticalLineTo(4f)
                horizontalLineTo(8f)
                verticalLineTo(2f)
                horizontalLineToRelative(2f)
                verticalLineTo(4f)
                horizontalLineToRelative(7f)
                verticalLineTo(6f)
                horizontalLineTo(14.1f)
                quadTo(13.58f, 7.8f, 12.53f, 9.7f)
                reflectiveQuadToRelative(-2.07f, 2.9f)
                lineToRelative(2.4f, 2.45f)
                lineTo(12.1f, 17.1f)
                lineTo(9.05f, 13.98f)
                lineTo(4f, 19f)
                close()
                moveTo(15.7f, 17.2f)
                horizontalLineToRelative(3.6f)
                lineTo(17.5f, 12.1f)
                lineToRelative(-1.8f, 5.1f)
                close()
            }
        }.build()
        return _translate!!
    }
private var _translate: ImageVector? = null

private val MenuBook: ImageVector
    get() {
        if (_menuBook != null) {
            return _menuBook!!
        }
        _menuBook = ImageVector.Builder(
            name = "menu_book",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(14f, 9.9f)
                verticalLineTo(8.2f)
                quadTo(14.83f, 7.85f, 15.69f, 7.68f)
                reflectiveQuadTo(17.5f, 7.5f)
                quadToRelative(0.65f, 0f, 1.28f, 0.1f)
                reflectiveQuadTo(20f, 7.85f)
                verticalLineToRelative(1.6f)
                quadTo(19.4f, 9.23f, 18.79f, 9.11f)
                reflectiveQuadTo(17.5f, 9f)
                quadTo(16.55f, 9f, 15.68f, 9.24f)
                quadTo(14.8f, 9.48f, 14f, 9.9f)
                close()
                moveToRelative(0f, 5.5f)
                verticalLineTo(13.7f)
                quadToRelative(0.83f, -0.35f, 1.69f, -0.53f)
                reflectiveQuadTo(17.5f, 13f)
                quadToRelative(0.65f, 0f, 1.28f, 0.1f)
                reflectiveQuadTo(20f, 13.35f)
                verticalLineToRelative(1.6f)
                quadTo(19.4f, 14.73f, 18.79f, 14.61f)
                reflectiveQuadTo(17.5f, 14.5f)
                quadToRelative(-0.95f, 0f, -1.82f, 0.22f)
                reflectiveQuadTo(14f, 15.4f)
                close()
                moveToRelative(0f, -2.75f)
                verticalLineToRelative(-1.7f)
                quadToRelative(0.83f, -0.35f, 1.69f, -0.53f)
                reflectiveQuadTo(17.5f, 10.25f)
                quadToRelative(0.65f, 0f, 1.28f, 0.1f)
                reflectiveQuadTo(20f, 10.6f)
                verticalLineToRelative(1.6f)
                quadTo(19.4f, 11.98f, 18.79f, 11.86f)
                reflectiveQuadTo(17.5f, 11.75f)
                quadToRelative(-0.95f, 0f, -1.82f, 0.24f)
                quadTo(14.8f, 12.23f, 14f, 12.65f)
                close()
                moveTo(6.5f, 16f)
                quadToRelative(1.18f, 0f, 2.29f, 0.26f)
                reflectiveQuadTo(11f, 17.05f)
                verticalLineTo(7.2f)
                quadTo(9.98f, 6.6f, 8.83f, 6.3f)
                reflectiveQuadTo(6.5f, 6f)
                quadTo(5.6f, 6f, 4.71f, 6.18f)
                reflectiveQuadTo(3f, 6.7f)
                verticalLineToRelative(9.9f)
                quadTo(3.88f, 16.3f, 4.74f, 16.15f)
                reflectiveQuadTo(6.5f, 16f)
                close()
                moveTo(13f, 17.05f)
                quadToRelative(1.1f, -0.53f, 2.21f, -0.79f)
                reflectiveQuadTo(17.5f, 16f)
                quadToRelative(0.9f, 0f, 1.76f, 0.15f)
                reflectiveQuadTo(21f, 16.6f)
                verticalLineTo(6.7f)
                quadTo(20.18f, 6.35f, 19.29f, 6.18f)
                reflectiveQuadTo(17.5f, 6f)
                quadTo(16.33f, 6f, 15.18f, 6.3f)
                reflectiveQuadTo(13f, 7.2f)
                verticalLineToRelative(9.85f)
                close()
                moveTo(12f, 20f)
                quadTo(10.8f, 19.05f, 9.4f, 18.52f)
                reflectiveQuadTo(6.5f, 18f)
                quadTo(5.45f, 18f, 4.44f, 18.27f)
                reflectiveQuadTo(2.5f, 19.05f)
                quadTo(1.98f, 19.33f, 1.49f, 19.02f)
                quadTo(1f, 18.73f, 1f, 18.15f)
                verticalLineTo(6.1f)
                quadTo(1f, 5.82f, 1.14f, 5.57f)
                quadTo(1.28f, 5.32f, 1.55f, 5.2f)
                quadTo(2.7f, 4.6f, 3.95f, 4.3f)
                reflectiveQuadTo(6.5f, 4f)
                quadTo(7.95f, 4f, 9.34f, 4.38f)
                reflectiveQuadTo(12f, 5.5f)
                quadTo(13.28f, 4.75f, 14.66f, 4.38f)
                reflectiveQuadTo(17.5f, 4f)
                quadToRelative(1.3f, 0f, 2.55f, 0.3f)
                reflectiveQuadToRelative(2.4f, 0.9f)
                quadToRelative(0.27f, 0.13f, 0.41f, 0.38f)
                reflectiveQuadTo(23f, 6.1f)
                verticalLineTo(18.15f)
                quadToRelative(0f, 0.58f, -0.49f, 0.88f)
                quadToRelative(-0.49f, 0.3f, -1.01f, 0.03f)
                quadToRelative(-0.92f, -0.5f, -1.94f, -0.78f)
                reflectiveQuadTo(17.5f, 18f)
                quadTo(16f, 18f, 14.6f, 18.52f)
                reflectiveQuadTo(12f, 20f)
                close()
                moveTo(7f, 11.65f)
                close()
            }
        }.build()
        return _menuBook!!
    }
private var _menuBook: ImageVector? = null

private fun getAppTypography(isArabic: Boolean): Typography {
    val fontFamily = if (isArabic) NotoSansArabicFontFamily else FontFamily.Default
    val defaultTypography = Typography()

    return Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = defaultTypography.bodyLarge.copy(
            fontFamily = fontFamily,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = fontFamily)
    )
}

private data class WhatsNewItem(
    val textEn: String,
    val textAr: String,
    @DrawableRes val beforeImage: Int? = null,
    @DrawableRes val afterImage: Int? = null,
    @DrawableRes val singleImage: Int? = null,
)

@Composable
fun WhatsNewDialog(
    onDismissRequest: () -> Unit,
    versionName: String = "0.0.5-beta",
    releaseDate: String = "October 20, 2026",
    releasesUrl: String = "https://github.com/hamzabellouch/gallery/releases",
) {
    var isArabic by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    val formattedVersion = if (versionName.startsWith("v", ignoreCase = true)) versionName else "v$versionName"
    val dateText = if (isArabic) "20 أكتوبر 2026" else releaseDate
    val titleText = if (isArabic) "ما الجديد" else stringResource(R.string.whats_new)
    val highlightsTitleText = if (isArabic) "أحدث التحديثات والتحسينات" else "Recent updates & improvements"

    val updateItems = listOf(
        WhatsNewItem(
            textEn = "As always, we've included improvements to performance for a better app experience. Thanks for choosing Gallery!",
            textAr = "كما هو الحال دائمًا، قمنا بتضمين تحسينات في الأداء لتجربة تطبيق أفضل. شكرًا لاختيارك Gallery!"
        )
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            MaterialTheme(
                typography = remember(isArabic) { getAppTypography(isArabic) }
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .heightIn(max = 640.dp)
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header: Title, version/date, and Close 'X' Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$dateText | $formattedVersion",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = onDismissRequest,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Scrollable Content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Banner Image
                            Image(
                                painter = painterResource(id = R.drawable.whats_new_banner),
                                contentDescription = "What's New Banner",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1024f / 537f)
                                    .clip(RoundedCornerShape(16.dp))
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Section Title
                            Text(
                                text = highlightsTitleText,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Update items list
                            updateItems.forEach { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 7.dp)
                                                .size(6.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (isArabic) item.textAr else item.textEn,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 20.sp
                                        )
                                    }

                                    if (item.beforeImage != null && item.afterImage != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = item.beforeImage),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(16f / 9f)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )

                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )

                                            Image(
                                                painter = painterResource(id = item.afterImage),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(16f / 9f)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        }
                                    } else if (item.singleImage != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp, bottom = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(id = item.singleImage),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth(0.48f)
                                                    .aspectRatio(16f / 9f)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Bottom Action Capsules (Language Toggle & Release Notes)
                            CompositionLocalProvider(
                                LocalLayoutDirection provides LayoutDirection.Ltr
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Language Toggle Capsule Button (Arabic / English)
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.clip(CircleShape)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .height(36.dp)
                                                .clickable(
                                                    role = Role.Button,
                                                    onClick = { isArabic = !isArabic }
                                                )
                                                .padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Translate,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = if (isArabic) "English" else "Arabic",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }

                                    // Release Notes Capsule Button
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.clip(CircleShape)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .height(36.dp)
                                                .clickable(
                                                    role = Role.Button,
                                                    onClick = {
                                                        try {
                                                            uriHandler.openUri(releasesUrl)
                                                        } catch (_: Exception) { }
                                                    }
                                                )
                                                .padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = MenuBook,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = if (isArabic) "ملاحظات الإصدار" else "Release Notes",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}
