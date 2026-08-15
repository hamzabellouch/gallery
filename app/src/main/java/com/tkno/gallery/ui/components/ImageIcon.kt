package com.tkno.gallery.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ImageIcon: ImageVector
    get() {
        if (_imageIcon != null) {
            return _imageIcon!!
        }
        _imageIcon = ImageVector.Builder(
            name = "Image",
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
                moveTo(4f, 22f)
                quadTo(3.18f, 22f, 2.59f, 21.41f)
                reflectiveQuadTo(2f, 20f)
                verticalLineTo(8f)
                quadTo(2f, 7.18f, 2.59f, 6.59f)
                reflectiveQuadTo(4f, 6f)
                horizontalLineTo(8f)
                lineTo(12f, 2f)
                lineToRelative(4f, 4f)
                horizontalLineToRelative(4f)
                quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                quadTo(22f, 7.18f, 22f, 8f)
                verticalLineTo(20f)
                quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                reflectiveQuadTo(20f, 22f)
                horizontalLineTo(4f)
                close()
                moveTo(4f, 20f)
                horizontalLineTo(20f)
                verticalLineTo(8f)
                horizontalLineTo(4f)
                verticalLineTo(20f)
                close()
                moveTo(6f, 18f)
                horizontalLineTo(18f)
                lineTo(14.25f, 13f)
                lineToRelative(-3f, 4f)
                lineTo(9f, 14f)
                lineTo(6f, 18f)
                close()
                moveTo(18.56f, 12.56f)
                quadTo(19f, 12.13f, 19f, 11.5f)
                reflectiveQuadTo(18.56f, 10.44f)
                reflectiveQuadTo(17.5f, 10f)
                reflectiveQuadToRelative(-1.06f, 0.44f)
                reflectiveQuadTo(16f, 11.5f)
                reflectiveQuadToRelative(0.44f, 1.06f)
                reflectiveQuadTo(17.5f, 13f)
                reflectiveQuadToRelative(1.06f, -0.44f)
                close()
                moveTo(10.1f, 6f)
                horizontalLineToRelative(3.8f)
                lineTo(12f, 4.1f)
                lineTo(10.1f, 6f)
                close()
                moveTo(4f, 20f)
                verticalLineTo(8f)
                verticalLineTo(20f)
                close()
            }
        }.build()
        return _imageIcon!!
    }

private var _imageIcon: ImageVector? = null
