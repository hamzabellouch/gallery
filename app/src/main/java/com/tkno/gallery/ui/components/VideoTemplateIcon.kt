package com.tkno.gallery.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val VideoTemplateIcon: ImageVector
    get() {
        if (_videoTemplateIcon != null) {
            return _videoTemplateIcon!!
        }
        _videoTemplateIcon = ImageVector.Builder(
            name = "VideoTemplate",
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
                moveTo(5f, 23f)
                quadTo(4.18f, 23f, 3.59f, 22.41f)
                reflectiveQuadTo(3f, 21f)
                verticalLineTo(20f)
                horizontalLineTo(5f)
                verticalLineToRelative(1f)
                horizontalLineTo(19f)
                verticalLineTo(20f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(1f)
                quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                reflectiveQuadTo(19f, 23f)
                horizontalLineTo(5f)
                close()
                moveTo(4f, 18f)
                quadTo(3.18f, 18f, 2.59f, 17.41f)
                reflectiveQuadTo(2f, 16f)
                verticalLineTo(8f)
                quadTo(2f, 7.18f, 2.59f, 6.59f)
                reflectiveQuadTo(4f, 6f)
                horizontalLineTo(20f)
                quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                quadTo(22f, 7.18f, 22f, 8f)
                verticalLineToRelative(8f)
                quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                reflectiveQuadTo(20f, 18f)
                horizontalLineTo(4f)
                close()
                moveTo(3f, 4f)
                verticalLineTo(3f)
                quadTo(3f, 2.17f, 3.59f, 1.59f)
                reflectiveQuadTo(5f, 1f)
                horizontalLineTo(19f)
                quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                reflectiveQuadTo(21f, 3f)
                verticalLineTo(4f)
                horizontalLineTo(19f)
                verticalLineTo(3f)
                horizontalLineTo(5f)
                verticalLineTo(4f)
                horizontalLineTo(3f)
                close()
                moveTo(4f, 16f)
                horizontalLineTo(20f)
                verticalLineTo(8f)
                horizontalLineTo(4f)
                verticalLineToRelative(8f)
                close()
                moveToRelative(8f, -4f)
                close()
                moveToRelative(-2f, 3f)
                lineToRelative(5f, -3f)
                lineTo(10f, 9f)
                verticalLineToRelative(6f)
                close()
            }
        }.build()
        return _videoTemplateIcon!!
    }

private var _videoTemplateIcon: ImageVector? = null
