package com.tkno.gallery.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CustomIcons {
    private var _leftPanelOpen: ImageVector? = null
    val LeftPanelOpen: ImageVector
        get() {
            if (_leftPanelOpen != null) return _leftPanelOpen!!
            _leftPanelOpen = ImageVector.Builder(
                name = "left_panel_open",
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
                    pathFillType = PathFillType.Companion.NonZero
                ) {
                    moveTo(12.5f, 8f)
                    verticalLineToRelative(8f)
                    lineToRelative(4f, -4f)
                    lineToRelative(-4f, -4f)
                    close()
                    moveTo(5f, 21f)
                    quadTo(4.18f, 21f, 3.59f, 20.41f)
                    reflectiveQuadTo(3f, 19f)
                    verticalLineTo(5f)
                    quadTo(3f, 4.17f, 3.59f, 3.59f)
                    reflectiveQuadTo(5f, 3f)
                    horizontalLineTo(19f)
                    quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                    reflectiveQuadTo(21f, 5f)
                    verticalLineTo(19f)
                    quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                    reflectiveQuadTo(19f, 21f)
                    horizontalLineTo(5f)
                    close()
                    moveTo(8f, 19f)
                    verticalLineTo(5f)
                    horizontalLineTo(5f)
                    verticalLineTo(19f)
                    horizontalLineTo(8f)
                    close()
                    moveToRelative(2f, 0f)
                    horizontalLineToRelative(9f)
                    verticalLineTo(5f)
                    horizontalLineTo(10f)
                    verticalLineTo(19f)
                    close()
                    moveTo(8f, 19f)
                    horizontalLineTo(5f)
                    horizontalLineTo(8f)
                    close()
                }
            }.build()
            return _leftPanelOpen!!
        }

    private var _delete: ImageVector? = null
    val Delete: ImageVector
        get() {
            if (_delete != null) return _delete!!
            _delete = ImageVector.Builder(
                name = "delete",
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
                    pathFillType = PathFillType.Companion.NonZero
                ) {
                    moveTo(7f, 21f)
                    quadTo(6.18f, 21f, 5.59f, 20.41f)
                    reflectiveQuadTo(5f, 19f)
                    verticalLineTo(6f)
                    horizontalLineTo(4f)
                    verticalLineTo(4f)
                    horizontalLineTo(9f)
                    verticalLineTo(3f)
                    horizontalLineToRelative(6f)
                    verticalLineTo(4f)
                    horizontalLineToRelative(5f)
                    verticalLineTo(6f)
                    horizontalLineTo(19f)
                    verticalLineTo(19f)
                    quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                    reflectiveQuadTo(17f, 21f)
                    horizontalLineTo(7f)
                    close()
                    moveTo(17f, 6f)
                    horizontalLineTo(7f)
                    verticalLineTo(19f)
                    horizontalLineTo(17f)
                    verticalLineTo(6f)
                    close()
                    moveTo(9f, 17f)
                    horizontalLineToRelative(2f)
                    verticalLineTo(8f)
                    horizontalLineTo(9f)
                    verticalLineToRelative(9f)
                    close()
                    moveToRelative(4f, 0f)
                    horizontalLineToRelative(2f)
                    verticalLineTo(8f)
                    horizontalLineTo(13f)
                    verticalLineToRelative(9f)
                    close()
                    moveTo(7f, 6f)
                    verticalLineTo(19f)
                    verticalLineTo(6f)
                    close()
                }
            }.build()
            return _delete!!
        }

    private var _edit: ImageVector? = null
    val Edit: ImageVector
        get() {
            if (_edit != null) return _edit!!
            _edit = ImageVector.Builder(
                name = "edit",
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
                    pathFillType = PathFillType.Companion.NonZero
                ) {
                    moveTo(5f, 19f)
                    horizontalLineTo(6.43f)
                    lineTo(16.2f, 9.23f)
                    lineTo(14.78f, 7.8f)
                    lineTo(5f, 17.58f)
                    verticalLineTo(19f)
                    close()
                    moveTo(3f, 21f)
                    verticalLineTo(16.75f)
                    lineTo(16.2f, 3.57f)
                    quadTo(16.5f, 3.3f, 16.86f, 3.15f)
                    reflectiveQuadTo(17.63f, 3f)
                    quadToRelative(0.4f, 0f, 0.78f, 0.15f)
                    reflectiveQuadTo(19.05f, 3.6f)
                    lineTo(20.43f, 5f)
                    quadToRelative(0.3f, 0.27f, 0.44f, 0.65f)
                    reflectiveQuadTo(21f, 6.4f)
                    quadToRelative(0f, 0.4f, -0.14f, 0.76f)
                    reflectiveQuadTo(20.43f, 7.82f)
                    lineTo(7.25f, 21f)
                    horizontalLineTo(3f)
                    close()
                    moveTo(19f, 6.4f)
                    lineTo(17.6f, 5f)
                    lineTo(19f, 6.4f)
                    close()
                    moveTo(15.48f, 8.52f)
                    lineTo(14.78f, 7.8f)
                    lineTo(16.2f, 9.23f)
                    lineTo(15.48f, 8.52f)
                    close()
                }
            }.build()
            return _edit!!
        }

    private var _wandStars: ImageVector? = null
    val WandStars: ImageVector
        get() {
            if (_wandStars != null) return _wandStars!!
            _wandStars = ImageVector.Builder(
                name = "wand_stars",
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
                    pathFillType = PathFillType.Companion.NonZero
                ) {
                    moveTo(4.4f, 21f)
                    lineTo(3f, 19.6f)
                    lineToRelative(7.53f, -7.55f)
                    lineTo(6f, 10.93f)
                    lineTo(10.95f, 7.85f)
                    lineTo(10.53f, 2f)
                    lineTo(15f, 5.77f)
                    lineToRelative(5.4f, -2.2f)
                    lineTo(18.23f, 9f)
                    lineTo(22f, 13.45f)
                    lineToRelative(-5.85f, -0.4f)
                    lineTo(13.05f, 18f)
                    lineTo(11.93f, 13.48f)
                    lineTo(4.4f, 21f)
                    close()
                    moveTo(5f, 8f)
                    lineTo(3f, 6f)
                    lineTo(5f, 4f)
                    lineTo(7f, 6f)
                    lineTo(5f, 8f)
                    close()
                    moveToRelative(8.88f, 4.92f)
                    lineToRelative(1.2f, -1.97f)
                    lineToRelative(2.33f, 0.18f)
                    lineTo(15.9f, 9.35f)
                    lineTo(16.78f, 7.2f)
                    lineTo(14.63f, 8.07f)
                    lineTo(12.85f, 6.6f)
                    lineToRelative(0.17f, 2.3f)
                    lineToRelative(-1.97f, 1.23f)
                    lineToRelative(2.25f, 0.55f)
                    lineToRelative(0.57f, 2.25f)
                    close()
                    moveTo(18f, 21f)
                    lineTo(16f, 19f)
                    lineToRelative(2f, -2f)
                    lineToRelative(2f, 2f)
                    lineToRelative(-2f, 2f)
                    close()
                    moveTo(14.23f, 9.75f)
                    close()
                }
            }.build()
            return _wandStars!!
        }

    private var _share: ImageVector? = null
    val Share: ImageVector
        get() {
            if (_share != null) return _share!!
            _share = ImageVector.Builder(
                name = "share",
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
                    pathFillType = PathFillType.Companion.NonZero
                ) {
                    moveTo(17f, 22f)
                    quadToRelative(-1.25f, 0f, -2.13f, -0.88f)
                    reflectiveQuadTo(14f, 19f)
                    quadToRelative(0f, -0.15f, 0.08f, -0.7f)
                    lineTo(7.05f, 14.2f)
                    quadToRelative(-0.4f, 0.38f, -0.93f, 0.59f)
                    reflectiveQuadTo(5f, 15f)
                    quadTo(3.75f, 15f, 2.88f, 14.13f)
                    reflectiveQuadTo(2f, 12f)
                    reflectiveQuadTo(2.88f, 9.88f)
                    reflectiveQuadTo(5f, 9f)
                    quadTo(5.6f, 9f, 6.13f, 9.21f)
                    reflectiveQuadTo(7.05f, 9.8f)
                    lineTo(14.08f, 5.7f)
                    quadTo(14.03f, 5.52f, 14.01f, 5.36f)
                    reflectiveQuadTo(14f, 5f)
                    quadTo(14f, 3.75f, 14.88f, 2.88f)
                    reflectiveQuadTo(17f, 2f)
                    reflectiveQuadToRelative(2.13f, 0.88f)
                    reflectiveQuadTo(20f, 5f)
                    reflectiveQuadTo(19.13f, 7.13f)
                    reflectiveQuadTo(17f, 8f)
                    quadTo(16.4f, 8f, 15.88f, 7.79f)
                    reflectiveQuadTo(14.95f, 7.2f)
                    lineTo(7.93f, 11.3f)
                    quadToRelative(0.05f, 0.18f, 0.06f, 0.34f)
                    reflectiveQuadTo(8f, 12f)
                    reflectiveQuadTo(7.99f, 12.36f)
                    reflectiveQuadTo(7.93f, 12.7f)
                    lineToRelative(7.03f, 4.1f)
                    quadToRelative(0.4f, -0.38f, 0.92f, -0.59f)
                    reflectiveQuadTo(17f, 16f)
                    quadToRelative(1.25f, 0f, 2.13f, 0.88f)
                    reflectiveQuadTo(20f, 19f)
                    reflectiveQuadToRelative(-0.88f, 2.13f)
                    reflectiveQuadTo(17f, 22f)
                    close()
                    moveToRelative(0f, -2f)
                    quadToRelative(0.43f, 0f, 0.71f, -0.29f)
                    quadTo(18f, 19.43f, 18f, 19f)
                    reflectiveQuadTo(17.71f, 18.29f)
                    reflectiveQuadTo(17f, 18f)
                    reflectiveQuadToRelative(-0.71f, 0.29f)
                    reflectiveQuadTo(16f, 19f)
                    reflectiveQuadToRelative(0.29f, 0.71f)
                    reflectiveQuadTo(17f, 20f)
                    close()
                    moveTo(5f, 13f)
                    quadToRelative(0.43f, 0f, 0.71f, -0.29f)
                    quadTo(6f, 12.43f, 6f, 12f)
                    reflectiveQuadTo(5.71f, 11.29f)
                    reflectiveQuadTo(5f, 11f)
                    quadTo(4.58f, 11f, 4.29f, 11.29f)
                    reflectiveQuadTo(4f, 12f)
                    reflectiveQuadToRelative(0.29f, 0.71f)
                    reflectiveQuadTo(5f, 13f)
                    close()
                    moveTo(17.71f, 5.71f)
                    quadTo(18f, 5.43f, 18f, 5f)
                    reflectiveQuadTo(17.71f, 4.29f)
                    reflectiveQuadTo(17f, 4f)
                    reflectiveQuadTo(16.29f, 4.29f)
                    reflectiveQuadTo(16f, 5f)
                    reflectiveQuadToRelative(0.29f, 0.71f)
                    reflectiveQuadTo(17f, 6f)
                    reflectiveQuadTo(17.71f, 5.71f)
                    close()
                    moveTo(17f, 19f)
                    close()
                    moveTo(5f, 12f)
                    close()
                    moveTo(17f, 5f)
                    close()
                }
            }.build()
            return _share!!
        }

    private var _pictureInPicture: ImageVector? = null
    val PictureInPicture: ImageVector
        get() {
            if (_pictureInPicture != null) return _pictureInPicture!!
            _pictureInPicture = ImageVector.Builder(
                name = "picture_in_picture",
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
                    pathFillType = PathFillType.Companion.NonZero,
                ) {
                    moveTo(10f, 17f)
                    horizontalLineToRelative(9f)
                    verticalLineTo(11f)
                    horizontalLineTo(10f)
                    verticalLineToRelative(6f)
                    close()
                    moveTo(4f, 20f)
                    quadTo(3.18f, 20f, 2.59f, 19.41f)
                    reflectiveQuadTo(2f, 18f)
                    verticalLineTo(6f)
                    quadTo(2f, 5.18f, 2.59f, 4.59f)
                    reflectiveQuadTo(4f, 4f)
                    horizontalLineTo(20f)
                    quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                    quadTo(22f, 5.18f, 22f, 6f)
                    verticalLineTo(18f)
                    quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                    reflectiveQuadTo(20f, 20f)
                    horizontalLineTo(4f)
                    close()
                    moveTo(4f, 18f)
                    horizontalLineTo(20f)
                    verticalLineTo(6f)
                    horizontalLineTo(4f)
                    verticalLineTo(18f)
                    close()
                    moveToRelative(0f, 0f)
                    verticalLineTo(6f)
                    verticalLineTo(18f)
                    close()
                }
            }.build()
            return _pictureInPicture!!
        }

    private var _forward10: ImageVector? = null
    val Forward10: ImageVector
        get() {
            if (_forward10 != null) return _forward10!!
            _forward10 = ImageVector.Builder(
                name = "forward_10",
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
                    pathFillType = PathFillType.Companion.NonZero,
                ) {
                    moveTo(9f, 16f)
                    verticalLineTo(11.5f)
                    horizontalLineTo(7.5f)
                    verticalLineTo(10f)
                    horizontalLineToRelative(3f)
                    verticalLineToRelative(6f)
                    horizontalLineTo(9f)
                    close()
                    moveToRelative(3.5f, 0f)
                    quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
                    reflectiveQuadTo(11.5f, 15f)
                    verticalLineTo(11f)
                    quadToRelative(0f, -0.43f, 0.29f, -0.71f)
                    reflectiveQuadTo(12.5f, 10f)
                    horizontalLineToRelative(2f)
                    quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                    reflectiveQuadTo(15.5f, 11f)
                    verticalLineToRelative(4f)
                    quadToRelative(0f, 0.42f, -0.29f, 0.71f)
                    reflectiveQuadTo(14.5f, 16f)
                    horizontalLineToRelative(-2f)
                    close()
                    moveTo(13f, 14.5f)
                    horizontalLineToRelative(1f)
                    verticalLineToRelative(-3f)
                    horizontalLineTo(13f)
                    verticalLineToRelative(3f)
                    close()
                    moveTo(8.49f, 21.29f)
                    quadTo(6.85f, 20.58f, 5.64f, 19.36f)
                    reflectiveQuadTo(3.71f, 16.51f)
                    reflectiveQuadTo(3f, 13f)
                    reflectiveQuadTo(3.71f, 9.49f)
                    reflectiveQuadTo(5.64f, 6.64f)
                    reflectiveQuadTo(8.49f, 4.71f)
                    reflectiveQuadTo(12f, 4f)
                    horizontalLineToRelative(0.15f)
                    lineTo(10.6f, 2.45f)
                    lineTo(12f, 1f)
                    lineToRelative(4f, 4f)
                    lineTo(12f, 9f)
                    lineTo(10.6f, 7.55f)
                    lineTo(12.15f, 6f)
                    horizontalLineTo(12f)
                    quadTo(9.08f, 6f, 7.04f, 8.04f)
                    reflectiveQuadTo(5f, 13f)
                    reflectiveQuadToRelative(2.04f, 4.96f)
                    reflectiveQuadTo(12f, 20f)
                    reflectiveQuadToRelative(4.96f, -2.04f)
                    quadTo(19f, 15.93f, 19f, 13f)
                    horizontalLineToRelative(2f)
                    quadToRelative(0f, 1.88f, -0.71f, 3.51f)
                    reflectiveQuadToRelative(-1.93f, 2.85f)
                    reflectiveQuadToRelative(-2.85f, 1.93f)
                    reflectiveQuadTo(12f, 22f)
                    reflectiveQuadTo(8.49f, 21.29f)
                    close()
                }
            }.build()
            return _forward10!!
        }

    private var _replay10: ImageVector? = null
    val Replay10: ImageVector
        get() {
            if (_replay10 != null) return _replay10!!
            _replay10 = ImageVector.Builder(
                name = "replay_10",
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
                    pathFillType = PathFillType.Companion.NonZero,
                ) {
                    moveTo(8.49f, 21.29f)
                    quadTo(6.85f, 20.58f, 5.64f, 19.36f)
                    reflectiveQuadTo(3.71f, 16.51f)
                    reflectiveQuadTo(3f, 13f)
                    horizontalLineTo(5f)
                    quadToRelative(0f, 2.92f, 2.04f, 4.96f)
                    reflectiveQuadTo(12f, 20f)
                    reflectiveQuadToRelative(4.96f, -2.04f)
                    quadTo(19f, 15.93f, 19f, 13f)
                    quadTo(19f, 10.07f, 16.96f, 8.04f)
                    reflectiveQuadTo(12f, 6f)
                    horizontalLineTo(11.85f)
                    lineTo(13.4f, 7.55f)
                    lineTo(12f, 9f)
                    lineTo(8f, 5f)
                    lineTo(12f, 1f)
                    lineToRelative(1.4f, 1.45f)
                    lineTo(11.85f, 4f)
                    horizontalLineTo(12f)
                    quadToRelative(1.88f, 0f, 3.51f, 0.71f)
                    quadToRelative(1.64f, 0.71f, 2.85f, 1.93f)
                    reflectiveQuadToRelative(1.93f, 2.85f)
                    reflectiveQuadTo(21f, 13f)
                    reflectiveQuadToRelative(-0.71f, 3.51f)
                    reflectiveQuadToRelative(-1.93f, 2.85f)
                    reflectiveQuadToRelative(-2.85f, 1.93f)
                    reflectiveQuadTo(12f, 22f)
                    reflectiveQuadTo(8.49f, 21.29f)
                    close()
                    moveTo(9f, 16f)
                    verticalLineTo(11.5f)
                    horizontalLineTo(7.5f)
                    verticalLineTo(10f)
                    horizontalLineToRelative(3f)
                    verticalLineToRelative(6f)
                    horizontalLineTo(9f)
                    close()
                    moveToRelative(3.5f, 0f)
                    quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
                    reflectiveQuadTo(11.5f, 15f)
                    verticalLineTo(11f)
                    quadToRelative(0f, -0.43f, 0.29f, -0.71f)
                    reflectiveQuadTo(12.5f, 10f)
                    horizontalLineToRelative(2f)
                    quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                    reflectiveQuadTo(15.5f, 11f)
                    verticalLineToRelative(4f)
                    quadToRelative(0f, 0.42f, -0.29f, 0.71f)
                    reflectiveQuadTo(14.5f, 16f)
                    horizontalLineToRelative(-2f)
                    close()
                    moveTo(13f, 14.5f)
                    horizontalLineToRelative(1f)
                    verticalLineToRelative(-3f)
                    horizontalLineTo(13f)
                    verticalLineToRelative(3f)
                    close()
                }
            }.build()
            return _replay10!!
        }

    private var _playArrow: ImageVector? = null
    val PlayArrow: ImageVector
        get() {
            if (_playArrow != null) return _playArrow!!
            _playArrow = ImageVector.Builder(
                name = "play_arrow",
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
                    pathFillType = PathFillType.Companion.NonZero,
                ) {
                    moveTo(8f, 19f)
                    verticalLineTo(5f)
                    lineToRelative(11f, 7f)
                    lineTo(8f, 19f)
                    close()
                    moveToRelative(2f, -7f)
                    close()
                    moveToRelative(0f, 3.35f)
                    lineTo(15.25f, 12f)
                    lineTo(10f, 8.65f)
                    verticalLineToRelative(6.7f)
                    close()
                }
            }.build()
            return _playArrow!!
        }

    private var _pauseIcon: ImageVector? = null
    val Pause: ImageVector
        get() {
            if (_pauseIcon != null) return _pauseIcon!!
            _pauseIcon = ImageVector.Builder(
                name = "pause",
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
                    pathFillType = PathFillType.Companion.NonZero,
                ) {
                    moveTo(13f, 19f)
                    verticalLineTo(5f)
                    horizontalLineToRelative(6f)
                    verticalLineTo(19f)
                    horizontalLineTo(13f)
                    close()
                    moveTo(5f, 19f)
                    verticalLineTo(5f)
                    horizontalLineToRelative(6f)
                    verticalLineTo(19f)
                    horizontalLineTo(5f)
                    close()
                    moveTo(15f, 17f)
                    horizontalLineToRelative(2f)
                    verticalLineTo(7f)
                    horizontalLineTo(15f)
                    verticalLineTo(17f)
                    close()
                    moveTo(7f, 17f)
                    horizontalLineTo(9f)
                    verticalLineTo(7f)
                    horizontalLineTo(7f)
                    verticalLineTo(17f)
                    close()
                    moveTo(7f, 7f)
                    verticalLineTo(17f)
                    verticalLineTo(7f)
                    close()
                    moveToRelative(8f, 0f)
                    verticalLineTo(17f)
                    verticalLineTo(7f)
                    close()
                }
            }.build()
            return _pauseIcon!!
        }

    private var _chevronLeft: ImageVector? = null
    val ChevronLeft: ImageVector
        get() {
            if (_chevronLeft != null) return _chevronLeft!!
            _chevronLeft = ImageVector.Builder(
                name = "chevron_left",
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
                    pathFillType = PathFillType.Companion.NonZero,
                ) {
                    moveTo(14f, 18f)
                    lineTo(8f, 12f)
                    lineTo(14f, 6f)
                    lineToRelative(1.4f, 1.4f)
                    lineTo(10.8f, 12f)
                    lineToRelative(4.6f, 4.6f)
                    lineTo(14f, 18f)
                    close()
                }
            }.build()
            return _chevronLeft!!
        }

    private var _fitScreen: ImageVector? = null
    val FitScreen: ImageVector
        get() {
            if (_fitScreen != null) return _fitScreen!!
            _fitScreen = ImageVector.Builder(
                name = "fit_screen",
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
                    pathFillType = PathFillType.Companion.NonZero,
                ) {
                    moveTo(20f, 9f)
                    verticalLineTo(6f)
                    horizontalLineTo(17f)
                    verticalLineTo(4f)
                    horizontalLineToRelative(3f)
                    quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                    quadTo(22f, 5.18f, 22f, 6f)
                    verticalLineTo(9f)
                    horizontalLineTo(20f)
                    close()
                    moveTo(2f, 9f)
                    verticalLineTo(6f)
                    quadTo(2f, 5.18f, 2.59f, 4.59f)
                    reflectiveQuadTo(4f, 4f)
                    horizontalLineTo(7f)
                    verticalLineTo(6f)
                    horizontalLineTo(4f)
                    verticalLineTo(9f)
                    horizontalLineTo(2f)
                    close()
                    moveTo(17f, 20f)
                    verticalLineTo(18f)
                    horizontalLineToRelative(3f)
                    verticalLineTo(15f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(3f)
                    quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                    reflectiveQuadTo(20f, 20f)
                    horizontalLineTo(17f)
                    close()
                    moveTo(4f, 20f)
                    quadTo(3.18f, 20f, 2.59f, 19.41f)
                    reflectiveQuadTo(2f, 18f)
                    verticalLineTo(15f)
                    horizontalLineTo(4f)
                    verticalLineToRelative(3f)
                    horizontalLineTo(7f)
                    verticalLineToRelative(2f)
                    horizontalLineTo(4f)
                    close()
                    moveTo(6f, 16f)
                    verticalLineTo(8f)
                    horizontalLineTo(18f)
                    verticalLineToRelative(8f)
                    horizontalLineTo(6f)
                    close()
                    moveTo(8f, 14f)
                    horizontalLineToRelative(8f)
                    verticalLineTo(10f)
                    horizontalLineTo(8f)
                    verticalLineToRelative(4f)
                    close()
                    moveToRelative(0f, 0f)
                    verticalLineTo(10f)
                    verticalLineToRelative(4f)
                    close()
                }
            }.build()
            return _fitScreen!!
        }

    private var _leftPanelClose: ImageVector? = null
    val LeftPanelClose: ImageVector
        get() {
            if (_leftPanelClose != null) return _leftPanelClose!!
            _leftPanelClose = ImageVector.Builder(
                name = "left_panel_close",
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
                    pathFillType = PathFillType.Companion.NonZero,
                ) {
                    moveTo(16.5f, 16f)
                    verticalLineTo(8f)
                    lineToRelative(-4f, 4f)
                    lineToRelative(4f, 4f)
                    close()
                    moveTo(5f, 21f)
                    quadTo(4.18f, 21f, 3.59f, 20.41f)
                    reflectiveQuadTo(3f, 19f)
                    verticalLineTo(5f)
                    quadTo(3f, 4.17f, 3.59f, 3.59f)
                    reflectiveQuadTo(5f, 3f)
                    horizontalLineTo(19f)
                    quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                    reflectiveQuadTo(21f, 5f)
                    verticalLineTo(19f)
                    quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                    reflectiveQuadTo(19f, 21f)
                    horizontalLineTo(5f)
                    close()
                    moveTo(8f, 19f)
                    verticalLineTo(5f)
                    horizontalLineTo(5f)
                    verticalLineTo(19f)
                    horizontalLineTo(8f)
                    close()
                    moveToRelative(2f, 0f)
                    horizontalLineToRelative(9f)
                    verticalLineTo(5f)
                    horizontalLineTo(10f)
                    verticalLineTo(19f)
                    close()
                    moveTo(8f, 19f)
                    horizontalLineTo(5f)
                    horizontalLineTo(8f)
                    close()
                }
            }.build()
            return _leftPanelClose!!
        }

    private var _mobileRotate: ImageVector? = null
    val MobileRotate: ImageVector
        get() {
            if (_mobileRotate != null) return _mobileRotate!!
            _mobileRotate = ImageVector.Builder(
                name = "mobile_rotate",
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
                    moveTo(12.4f, 19.45f)
                    lineTo(4.58f, 11.6f)
                    quadTo(4.3f, 11.33f, 4.15f, 10.98f)
                    reflectiveQuadTo(4f, 10.25f)
                    reflectiveQuadTo(4.15f, 9.52f)
                    reflectiveQuadTo(4.58f, 8.9f)
                    lineTo(8.9f, 4.57f)
                    quadTo(9.18f, 4.3f, 9.53f, 4.16f)
                    reflectiveQuadTo(10.25f, 4.02f)
                    reflectiveQuadToRelative(0.73f, 0.14f)
                    reflectiveQuadTo(11.6f, 4.57f)
                    lineToRelative(7.82f, 7.82f)
                    quadToRelative(0.28f, 0.28f, 0.43f, 0.63f)
                    quadTo(20f, 13.38f, 20f, 13.75f)
                    reflectiveQuadToRelative(-0.15f, 0.72f)
                    reflectiveQuadTo(19.43f, 15.1f)
                    lineTo(15.1f, 19.45f)
                    quadToRelative(-0.28f, 0.28f, -0.63f, 0.41f)
                    reflectiveQuadTo(13.75f, 20f)
                    reflectiveQuadTo(13.03f, 19.86f)
                    reflectiveQuadTo(12.4f, 19.45f)
                    close()
                    moveTo(13.75f, 18f)
                    lineTo(18f, 13.75f)
                    lineTo(10.25f, 6f)
                    lineTo(6f, 10.25f)
                    lineTo(13.75f, 18f)
                    close()
                    moveTo(12f, 24f)
                    quadTo(9.53f, 24f, 7.34f, 23.06f)
                    reflectiveQuadTo(3.51f, 20.49f)
                    reflectiveQuadTo(0.94f, 16.66f)
                    reflectiveQuadTo(0f, 12f)
                    horizontalLineTo(2f)
                    quadToRelative(0f, 1.77f, 0.6f, 3.4f)
                    quadToRelative(0.6f, 1.62f, 1.66f, 2.92f)
                    reflectiveQuadTo(6.8f, 20.54f)
                    reflectiveQuadToRelative(3.22f, 1.29f)
                    lineTo(7.4f, 19.2f)
                    lineTo(8.8f, 17.8f)
                    lineToRelative(5.9f, 5.9f)
                    quadToRelative(-0.65f, 0.15f, -1.34f, 0.23f)
                    reflectiveQuadTo(12f, 24f)
                    close()
                    moveTo(22f, 12f)
                    quadTo(22f, 10.23f, 21.4f, 8.6f)
                    quadTo(20.8f, 6.97f, 19.74f, 5.68f)
                    reflectiveQuadTo(17.2f, 3.46f)
                    quadTo(15.73f, 2.55f, 13.98f, 2.17f)
                    lineTo(16.6f, 4.8f)
                    lineTo(15.2f, 6.2f)
                    lineTo(9.3f, 0.3f)
                    quadTo(9.95f, 0.15f, 10.64f, 0.07f)
                    reflectiveQuadTo(12f, 0f)
                    quadToRelative(2.48f, 0f, 4.66f, 0.94f)
                    reflectiveQuadToRelative(3.82f, 2.57f)
                    reflectiveQuadToRelative(2.57f, 3.82f)
                    reflectiveQuadTo(24f, 12f)
                    horizontalLineTo(22f)
                    close()
                    moveTo(12f, 12f)
                    close()
                    moveTo(9.33f, 10.1f)
                    quadToRelative(0.32f, 0f, 0.54f, -0.22f)
                    reflectiveQuadTo(10.08f, 9.35f)
                    quadToRelative(0f, -0.33f, -0.21f, -0.54f)
                    reflectiveQuadTo(9.33f, 8.6f)
                    quadTo(9.03f, 8.6f, 8.8f, 8.81f)
                    quadTo(8.58f, 9.02f, 8.58f, 9.35f)
                    quadToRelative(0f, 0.3f, 0.22f, 0.53f)
                    reflectiveQuadTo(9.33f, 10.1f)
                    close()
                }
            }.build()
            return _mobileRotate!!
        }

    private var _favorite: ImageVector? = null
    val Favorite: ImageVector
        get() {
            if (_favorite != null) return _favorite!!
            _favorite = ImageVector.Builder(
                name = "favorite",
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
                    moveTo(12f, 21f)
                    lineTo(10.55f, 19.7f)
                    quadTo(8.03f, 17.43f, 6.38f, 15.78f)
                    quadTo(4.73f, 14.13f, 3.75f, 12.81f)
                    quadTo(2.78f, 11.5f, 2.39f, 10.4f)
                    reflectiveQuadTo(2f, 8.15f)
                    quadTo(2f, 5.8f, 3.58f, 4.22f)
                    reflectiveQuadTo(7.5f, 2.65f)
                    quadToRelative(1.3f, 0f, 2.48f, 0.55f)
                    reflectiveQuadTo(12f, 4.75f)
                    quadToRelative(0.85f, -1f, 2.03f, -1.55f)
                    reflectiveQuadTo(16.5f, 2.65f)
                    quadToRelative(2.35f, 0f, 3.93f, 1.57f)
                    reflectiveQuadTo(22f, 8.15f)
                    quadTo(22f, 9.3f, 21.61f, 10.4f)
                    reflectiveQuadToRelative(-1.36f, 2.41f)
                    quadToRelative(-0.97f, 1.31f, -2.63f, 2.96f)
                    quadToRelative(-1.65f, 1.65f, -4.17f, 3.92f)
                    lineTo(12f, 21f)
                    close()
                    moveToRelative(0f, -2.7f)
                    quadToRelative(2.4f, -2.15f, 3.95f, -3.69f)
                    reflectiveQuadTo(18.4f, 11.94f)
                    reflectiveQuadTo(19.65f, 9.91f)
                    quadTo(20f, 9.02f, 20f, 8.15f)
                    quadToRelative(0f, -1.5f, -1f, -2.5f)
                    reflectiveQuadToRelative(-2.5f, -1f)
                    quadToRelative(-1.17f, 0f, -2.17f, 0.66f)
                    quadTo(13.33f, 5.97f, 12.95f, 7f)
                    horizontalLineToRelative(-1.9f)
                    quadTo(10.68f, 5.97f, 9.68f, 5.31f)
                    reflectiveQuadTo(7.5f, 4.65f)
                    quadTo(6f, 4.65f, 5f, 5.65f)
                    reflectiveQuadTo(4f, 8.15f)
                    quadTo(4f, 9.02f, 4.35f, 9.91f)
                    reflectiveQuadTo(5.6f, 11.94f)
                    reflectiveQuadToRelative(2.45f, 2.67f)
                    reflectiveQuadTo(12f, 18.3f)
                    close()
                    moveToRelative(0f, -6.83f)
                    close()
                }
            }.build()
            return _favorite!!
        }

    private var _skipNext: ImageVector? = null
    val SkipNext: ImageVector
        get() {
            if (_skipNext != null) return _skipNext!!
            _skipNext = ImageVector.Builder(
                name = "skip_next",
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
                    moveTo(16.5f, 18f)
                    verticalLineTo(6f)
                    horizontalLineToRelative(2f)
                    verticalLineTo(18f)
                    horizontalLineToRelative(-2f)
                    close()
                    moveToRelative(-11f, 0f)
                    verticalLineTo(6f)
                    lineToRelative(9f, 6f)
                    lineToRelative(-9f, 6f)
                    close()
                    moveToRelative(2f, -6f)
                    close()
                    moveToRelative(0f, 2.25f)
                    lineTo(10.9f, 12f)
                    lineTo(7.5f, 9.75f)
                    verticalLineToRelative(4.5f)
                    close()
                }
            }.build()
            return _skipNext!!
        }

    private var _skipPrevious: ImageVector? = null
    val SkipPrevious: ImageVector
        get() {
            if (_skipPrevious != null) return _skipPrevious!!
            _skipPrevious = ImageVector.Builder(
                name = "skip_previous",
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
                    moveTo(5.5f, 18f)
                    verticalLineTo(6f)
                    horizontalLineToRelative(2f)
                    verticalLineTo(18f)
                    horizontalLineToRelative(-2f)
                    close()
                    moveToRelative(13f, 0f)
                    lineToRelative(-9f, -6f)
                    lineToRelative(9f, -6f)
                    verticalLineTo(18f)
                    close()
                    moveToRelative(-2f, -6f)
                    close()
                    moveToRelative(0f, 2.25f)
                    verticalLineTo(9.75f)
                    lineTo(13.1f, 12f)
                    lineToRelative(3.4f, 2.25f)
                    close()
                }
            }.build()
            return _skipPrevious!!
        }

    private var _icon2k: ImageVector? = null
    val Icon2K: ImageVector
        get() {
            if (_icon2k != null) return _icon2k!!
            _icon2k = ImageVector.Builder(
                name = "icon_2k",
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
                    moveTo(13f, 15f)
                    horizontalLineToRelative(1.5f)
                    verticalLineTo(12.75f)
                    lineTo(16.25f, 15f)
                    horizontalLineToRelative(1.82f)
                    lineTo(15.75f, 12f)
                    lineTo(18.08f, 9f)
                    horizontalLineTo(16.25f)
                    lineTo(14.5f, 11.25f)
                    verticalLineTo(9f)
                    horizontalLineTo(13f)
                    verticalLineToRelative(6f)
                    close()
                    moveTo(6.5f, 15f)
                    horizontalLineTo(11f)
                    verticalLineTo(13.5f)
                    horizontalLineTo(8f)
                    verticalLineToRelative(-1f)
                    horizontalLineToRelative(2f)
                    quadToRelative(0.43f, 0f, 0.71f, -0.29f)
                    quadTo(11f, 11.93f, 11f, 11.5f)
                    verticalLineTo(10f)
                    quadTo(11f, 9.57f, 10.71f, 9.29f)
                    reflectiveQuadTo(10f, 9f)
                    horizontalLineTo(6.5f)
                    verticalLineToRelative(1.5f)
                    horizontalLineToRelative(3f)
                    verticalLineToRelative(1f)
                    horizontalLineToRelative(-2f)
                    quadToRelative(-0.42f, 0f, -0.71f, 0.29f)
                    reflectiveQuadTo(6.5f, 12.5f)
                    verticalLineTo(15f)
                    close()
                    moveTo(5f, 21f)
                    quadTo(4.18f, 21f, 3.59f, 20.41f)
                    reflectiveQuadTo(3f, 19f)
                    verticalLineTo(5f)
                    quadTo(3f, 4.17f, 3.59f, 3.59f)
                    reflectiveQuadTo(5f, 3f)
                    horizontalLineTo(19f)
                    quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                    reflectiveQuadTo(21f, 5f)
                    verticalLineTo(19f)
                    quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                    reflectiveQuadTo(19f, 21f)
                    horizontalLineTo(5f)
                    close()
                    moveTo(5f, 19f)
                    horizontalLineTo(19f)
                    verticalLineTo(5f)
                    horizontalLineTo(5f)
                    verticalLineTo(19f)
                    close()
                    moveTo(5f, 5f)
                    verticalLineTo(19f)
                    verticalLineTo(5f)
                    close()
                }
            }.build()
            return _icon2k!!
        }

    private var _repeat: ImageVector? = null
    val Repeat: ImageVector
        get() {
            if (_repeat != null) return _repeat!!
            _repeat = ImageVector.Builder(
                name = "repeat",
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
                    moveTo(7f, 22f)
                    lineTo(3f, 18f)
                    lineTo(7f, 14f)
                    lineToRelative(1.4f, 1.45f)
                    lineTo(6.85f, 17f)
                    horizontalLineTo(17f)
                    verticalLineTo(13f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(6f)
                    horizontalLineTo(6.85f)
                    lineTo(8.4f, 20.55f)
                    lineTo(7f, 22f)
                    close()
                    moveTo(5f, 11f)
                    verticalLineTo(5f)
                    horizontalLineTo(17.15f)
                    lineTo(15.6f, 3.45f)
                    lineTo(17f, 2f)
                    lineToRelative(4f, 4f)
                    lineToRelative(-4f, 4f)
                    lineTo(15.6f, 8.55f)
                    lineTo(17.15f, 7f)
                    horizontalLineTo(7f)
                    verticalLineToRelative(4f)
                    horizontalLineTo(5f)
                    close()
                }
            }.build()
            return _repeat!!
        }

    private var _sdCard: ImageVector? = null
    val SdCard: ImageVector
        get() {
            if (_sdCard != null) return _sdCard!!
            _sdCard = ImageVector.Builder(
                name = "sd_card",
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
                    pathFillType = PathFillType.Companion.NonZero
                ) {
                    moveTo(18f, 2f)
                    horizontalLineToRelative(-8f)
                    lineTo(4f, 8f)
                    verticalLineToRelative(12f)
                    quadToRelative(0f, 1.1f, 0.9f, 2f)
                    horizontalLineToRelative(12f)
                    quadToRelative(1.1f, 0f, 2f, -0.9f)
                    verticalLineTo(4f)
                    quadToRelative(0f, -1.1f, -0.9f, -2f)
                    close()
                    moveTo(9f, 7f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(4f)
                    horizontalLineTo(9f)
                    verticalLineTo(7f)
                    close()
                    moveToRelative(3f, 0f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(4f)
                    horizontalLineToRelative(-2f)
                    verticalLineTo(7f)
                    close()
                    moveToRelative(3f, 0f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(4f)
                    horizontalLineToRelative(-2f)
                    verticalLineTo(7f)
                    close()
                }
            }.build()
            return _sdCard!!
        }
}
