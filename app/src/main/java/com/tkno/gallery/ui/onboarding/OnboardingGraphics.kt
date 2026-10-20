package com.tkno.gallery.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tkno.gallery.R

// Official Gallery Brand Color
val GalleryBlue = Color(0xFF1A73E8)

/**
 * Screen 1 Illustration: Hero Vector Graphic
 * Spans full width from far right to far left.
 */
@Composable
fun WelcomeIllustration(
    modifier: Modifier = Modifier,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    val resId = if (isDark) R.drawable.ic_onboarding_welcome_hero_dark else R.drawable.ic_onboarding_welcome_hero_light
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = modifier.fillMaxWidth(),
        contentScale = ContentScale.FillWidth
    )
}

/**
 * Screen 2 Illustration: Features Vector Graphic
 * Centered illustration.
 */
@Composable
fun FeaturesIllustration(
    modifier: Modifier = Modifier,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    val resId = if (isDark) R.drawable.ic_onboarding_features_hero_dark else R.drawable.ic_onboarding_features_hero_light
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            modifier = Modifier.size(240.dp),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Screen 3 Illustration: Level Up Vector Graphic
 * Centered illustration.
 */
@Composable
fun LevelUpIllustration(
    modifier: Modifier = Modifier,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    val resId = if (isDark) R.drawable.ic_onboarding_level_up_hero_dark else R.drawable.ic_onboarding_level_up_hero_light
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            modifier = Modifier.size(240.dp),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Gallery Official Brand Badge - Circular app icon
 */
@Composable
fun GalleryBrandBadge(
    modifier: Modifier = Modifier,
    iconSize: Dp = 22.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_gallery_logo_round),
        contentDescription = null,
        modifier = modifier
            .size(iconSize)
            .clip(CircleShape)
    )
}
