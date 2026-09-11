package com.dockeredly.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.dockeredly.app.R

/**
 * Google Sans Flex (OFL-licensed, see /OFL-GoogleSansFlex.txt), bundled as a single
 * regular-weight static instance. Compose synthesizes bold for the heavier weights the
 * type scale below requests, since only one weight instance is bundled - a standard,
 * non-fake fallback, not a placeholder font.
 */
private val googleSansFlex = FontFamily(Font(R.font.google_sans_flex, FontWeight.Normal))

val DockeredlyTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.withFamily(FontWeight.Normal),
        displayMedium = base.displayMedium.withFamily(FontWeight.Normal),
        displaySmall = base.displaySmall.withFamily(FontWeight.Normal),
        headlineLarge = base.headlineLarge.withFamily(FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.withFamily(FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.withFamily(FontWeight.SemiBold),
        titleLarge = base.titleLarge.withFamily(FontWeight.Medium),
        titleMedium = base.titleMedium.withFamily(FontWeight.Medium),
        titleSmall = base.titleSmall.withFamily(FontWeight.Medium),
        bodyLarge = base.bodyLarge.withFamily(FontWeight.Normal),
        bodyMedium = base.bodyMedium.withFamily(FontWeight.Normal),
        bodySmall = base.bodySmall.withFamily(FontWeight.Normal),
        labelLarge = base.labelLarge.withFamily(FontWeight.Medium),
        labelMedium = base.labelMedium.withFamily(FontWeight.Medium),
        labelSmall = base.labelSmall.withFamily(FontWeight.Medium),
    )
}

private fun TextStyle.withFamily(weight: FontWeight): TextStyle =
    copy(fontFamily = googleSansFlex, fontWeight = weight)
