package com.github.codeworkscreativehub.fork

import androidx.core.content.edit
import com.github.codeworkscreativehub.mlauncher.data.Prefs

/**
 * Settings owned by this fork.
 *
 * They live here, as extensions on [Prefs], rather than in Prefs.kt and PrefsKeys.kt so that
 * upstream can keep changing those files without this fork having to resolve conflicts in them.
 */

private const val AI_SEARCH_APP = "AI_SEARCH_APP"

/** Package name of the app the AI search button hands queries to; blank when unset. */
var Prefs.aiSearchApp: String
    get() = prefsNormal.getString(AI_SEARCH_APP, "") ?: ""
    set(value) = prefsNormal.edit { putString(AI_SEARCH_APP, value) }
