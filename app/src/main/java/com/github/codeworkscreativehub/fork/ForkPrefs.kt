package com.github.codeworkscreativehub.fork

import androidx.compose.runtime.Composable
import androidx.core.content.edit
import com.github.codeworkscreativehub.common.getLocalizedString
import com.github.codeworkscreativehub.mlauncher.R
import com.github.codeworkscreativehub.mlauncher.data.EnumOption
import com.github.codeworkscreativehub.mlauncher.data.Prefs

/**
 * Settings owned by this fork.
 *
 * They live here, as extensions on [Prefs], rather than in Prefs.kt and PrefsKeys.kt so that
 * upstream can keep changing those files without this fork having to resolve conflicts in them.
 */

private const val AI_SEARCH_APP = "AI_SEARCH_APP"
private const val SEARCH_BAR_POSITION = "SEARCH_BAR_POSITION"

/** Where the app drawer search bar sits. Bottom keeps it in thumb reach, above the keyboard. */
enum class SearchBarPosition : EnumOption {
    Top,
    Bottom;

    fun getString(): String {
        return when (this) {
            Top -> getLocalizedString(R.string.search_bar_position_top)
            Bottom -> getLocalizedString(R.string.search_bar_position_bottom)
        }
    }

    @Composable
    override fun string(): String {
        return when (this) {
            Top -> getLocalizedString(R.string.search_bar_position_top)
            Bottom -> getLocalizedString(R.string.search_bar_position_bottom)
        }
    }
}

var Prefs.searchBarPosition: SearchBarPosition
    get() = runCatching {
        SearchBarPosition.valueOf(
            prefsNormal.getString(SEARCH_BAR_POSITION, SearchBarPosition.Bottom.name)
                ?: SearchBarPosition.Bottom.name
        )
    }.getOrDefault(SearchBarPosition.Bottom)
    set(value) = prefsNormal.edit { putString(SEARCH_BAR_POSITION, value.name) }

/** Package name of the app the AI search button hands queries to; blank when unset. */
var Prefs.aiSearchApp: String
    get() = prefsNormal.getString(AI_SEARCH_APP, "") ?: ""
    set(value) = prefsNormal.edit { putString(AI_SEARCH_APP, value) }
