package com.afrouzi.fakecall.data.prefs

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    PERSIAN("fa"),
    ENGLISH("en")
}

enum class AppFont {
    IRAN_SANS, IRAN_YEKAN
}

enum class DigitStyle {
    PERSIAN, ENGLISH
}

enum class ViewMode {
    CASUAL, PRO
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val font: AppFont = AppFont.IRAN_SANS,
    val digitStyle: DigitStyle = DigitStyle.PERSIAN,
    val viewMode: ViewMode = ViewMode.CASUAL,
    val showSystemApps: Boolean = false,
)
