package com.github.codeworkscreativehub.fork

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.TextUnit
import com.github.codeworkscreativehub.common.getLocalizedString
import com.github.codeworkscreativehub.mlauncher.R
import com.github.codeworkscreativehub.mlauncher.data.Prefs
import com.github.codeworkscreativehub.mlauncher.ui.components.DialogManager
import com.github.codeworkscreativehub.mlauncher.ui.compose.SettingsComposable.SettingsSelect

/**
 * Settings rows owned by this fork, kept in one composable so SettingsFragment.kt carries a
 * single call site instead of blocks that would need re-resolving on every upstream rebase.
 */
@Composable
fun ForkSettings(
    context: Context,
    prefs: Prefs,
    dialogBuilder: DialogManager,
    fontSize: TextUnit,
) {
    var aiSearchApp by remember { mutableStateOf(prefs.aiSearchApp) }

    SettingsSelect(
        title = getLocalizedString(R.string.ai_search_app),
        option = context.getAppLabel(aiSearchApp) ?: getLocalizedString(R.string.none),
        fontSize = fontSize,
        onClick = {
            // Any app that accepts shared text can take a query; that covers Claude, Gemini
            // and every other assistant without hardcoding a list of packages.
            val candidates = context.aiAppCandidates()
            val packages = listOf("") + candidates.map { it.first }
            val labels = listOf(getLocalizedString(R.string.none)) + candidates.map { it.second }

            dialogBuilder.showSingleChoiceBottomSheet(
                context = context,
                options = labels.toTypedArray(),
                title = getLocalizedString(R.string.ai_search_app),
                selectedIndex = packages.indexOf(aiSearchApp).takeIf { it >= 0 } ?: 0,
                onItemSelected = { selectedLabel ->
                    val index = labels.indexOfFirst { it == selectedLabel }
                    if (index != -1) {
                        aiSearchApp = packages[index]
                        prefs.aiSearchApp = packages[index]
                    }
                }
            )
        }
    )
}
