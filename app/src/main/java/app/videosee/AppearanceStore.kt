package app.videosee

import android.content.Context

/** Small persistent store for choices that should survive process restarts. */
class AppearanceStore(context: Context) {
    private val preferences = context.getSharedPreferences("videosee_appearance", Context.MODE_PRIVATE)

    var theme: AppTheme
        get() = preferences.getString(THEME_KEY, AppTheme.Midnight.name)
            ?.let { name -> AppTheme.entries.firstOrNull { it.name == name } }
            ?: AppTheme.Midnight
        set(value) {
            preferences.edit().putString(THEME_KEY, value.name).apply()
        }

    private companion object {
        const val THEME_KEY = "theme"
    }
}
