package com.afrouzi.fakecall.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import com.afrouzi.fakecall.data.prefs.AppSettings
import com.afrouzi.fakecall.data.prefs.ThemeMode
import java.util.Locale

val LocalAppSettings = staticCompositionLocalOf { AppSettings() }
val LocalUiFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }
val LocalLatinFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    secondary = LightSecondary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    secondary = DarkSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
)


@Composable
fun FakeCallTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val isPersian = when (settings.language) {
        com.afrouzi.fakecall.data.prefs.AppLanguage.PERSIAN -> true
        com.afrouzi.fakecall.data.prefs.AppLanguage.ENGLISH -> false
        com.afrouzi.fakecall.data.prefs.AppLanguage.SYSTEM -> {
            val lang = java.util.Locale.getDefault().language
            lang == "fa" || lang == "ar"
        }
    }
    val uiFamily = resolveFontFamily(isPersian = isPersian)
    val latinFamily = SansXLatin
    val layoutDirection = if (isPersian) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr

    val currentContext = LocalContext.current
    val targetLocale = when (settings.language) {
        com.afrouzi.fakecall.data.prefs.AppLanguage.PERSIAN -> Locale("fa")
        com.afrouzi.fakecall.data.prefs.AppLanguage.ENGLISH -> Locale("en")
        com.afrouzi.fakecall.data.prefs.AppLanguage.SYSTEM -> Locale.getDefault()
    }
    val localizedConfiguration = remember(targetLocale, currentContext) {
        Configuration(currentContext.resources.configuration).apply {
            setLocale(targetLocale)
            setLayoutDirection(targetLocale)
        }
    }
    val localizedContext = remember(targetLocale, currentContext) {
        val configContext = currentContext.createConfigurationContext(localizedConfiguration)
        object : android.content.ContextWrapper(currentContext) {
            override fun getResources(): android.content.res.Resources = configContext.resources
            override fun getAssets(): android.content.res.AssetManager = configContext.assets
        }
    }

    CompositionLocalProvider(
        LocalAppSettings provides settings,
        LocalUiFontFamily provides uiFamily,
        LocalLatinFontFamily provides latinFamily,
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
        LocalLayoutDirection provides layoutDirection,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = appTypography(uiFamily),
            content = content,
        )
    }
}

