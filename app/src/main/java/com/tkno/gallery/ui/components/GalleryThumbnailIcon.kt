package com.tkno.gallery.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GalleryThumbnailIcon: ImageVector
    get() {
        if (_galleryThumbnailIcon != null) {
            return _galleryThumbnailIcon!!
        }
        _galleryThumbnailIcon = ImageVector.Builder(
            name = "GalleryThumbnail",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
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
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(3f, 19f)
                quadTo(2.18f, 19f, 1.59f, 18.41f)
                reflectiveQuadTo(1f, 17f)
                verticalLineTo(7f)
                quadTo(1f, 6.18f, 1.59f, 5.59f)
                reflectiveQuadTo(3f, 5f)
                horizontalLineTo(13f)
                quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                quadTo(15f, 6.18f, 15f, 7f)
                verticalLineTo(17f)
                quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                reflectiveQuadTo(13f, 19f)
                horizontalLineTo(3f)
                close()
                moveTo(18f, 11f)
                quadToRelative(-0.43f, 0f, -0.71f, -0.29f)
                quadTo(17f, 10.43f, 17f, 10f)
                verticalLineTo(6f)
                quadTo(17f, 5.57f, 17.29f, 5.29f)
                reflectiveQuadTo(18f, 5f)
                horizontalLineToRelative(4f)
                quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                reflectiveQuadTo(23f, 6f)
                verticalLineToRelative(4f)
                quadToRelative(0f, 0.42f, -0.29f, 0.71f)
                reflectiveQuadTo(22f, 11f)
                horizontalLineTo(18f)
                close()
                moveTo(19f, 9f)
                horizontalLineToRelative(2f)
                verticalLineTo(7f)
                horizontalLineTo(19f)
                verticalLineTo(9f)
                close()
                moveTo(3f, 17f)
                horizontalLineTo(13f)
                verticalLineTo(7f)
                horizontalLineTo(3f)
                verticalLineTo(17f)
                close()
                moveTo(4f, 15f)
                horizontalLineToRelative(8f)
                lineTo(9.38f, 11.5f)
                lineTo(7.5f, 14f)
                lineTo(6.13f, 12.18f)
                lineTo(4f, 15f)
                close()
                moveToRelative(14f, 4f)
                quadToRelative(-0.43f, 0f, -0.71f, -0.29f)
                quadTo(17f, 18.43f, 17f, 18f)
                verticalLineTo(14f)
                quadToRelative(0f, -0.43f, 0.29f, -0.71f)
                reflectiveQuadTo(18f, 13f)
                horizontalLineToRelative(4f)
                quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                reflectiveQuadTo(23f, 14f)
                verticalLineToRelative(4f)
                quadToRelative(0f, 0.43f, -0.29f, 0.71f)
                reflectiveQuadTo(22f, 19f)
                horizontalLineTo(18f)
                close()
                moveToRelative(1f, -2f)
                horizontalLineToRelative(2f)
                verticalLineTo(15f)
                horizontalLineTo(19f)
                verticalLineToRelative(2f)
                close()
                moveTo(3f, 17f)
                verticalLineTo(7f)
                verticalLineTo(17f)
                close()
                moveTo(19f, 9f)
                verticalLineTo(7f)
                verticalLineTo(9f)
                close()
                moveToRelative(0f, 8f)
                verticalLineTo(15f)
                verticalLineToRelative(2f)
                close()
            }
        }.build()
        return _galleryThumbnailIcon!!
    }

private var _galleryThumbnailIcon: ImageVector? = null
