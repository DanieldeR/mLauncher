package com.github.codeworkscreativehub.fork

import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import com.github.codeworkscreativehub.common.getLocalizedString
import com.github.codeworkscreativehub.common.showShortToast
import com.github.codeworkscreativehub.mlauncher.R
import com.github.codeworkscreativehub.mlauncher.data.Prefs
import com.github.codeworkscreativehub.mlauncher.databinding.FragmentAppDrawerBinding

/**
 * The fork's additions to the app drawer search row.
 *
 * All of it lives here so that AppDrawerFragment.kt, which upstream edits often, only carries a
 * handful of call sites.
 */
class DrawerSearchBar(
    private val context: Context,
    private val binding: FragmentAppDrawerBinding,
    private val prefs: Prefs,
) {

    /**
     * Adds the AI hand-off button to the search row when the third app list button flag is set.
     * Built in code rather than in fragment_app_drawer.xml to keep that layout free of fork edits.
     */
    fun attachAiSearchButton(query: () -> String) {
        if (!prefs.getMenuFlags(APPLIST_BUTTON_FLAGS, "000").getOrElse(AI_BUTTON_FLAG) { false }) return

        val density = context.resources.displayMetrics.density
        val horizontalPadding = (5 * density).toInt()
        val verticalPadding = (8 * density).toInt()

        val button = ImageView(context).apply {
            adjustViewBounds = true
            contentDescription = getLocalizedString(R.string.applist_button_ai)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            setImageResource(R.drawable.ic_ai_search)
            setOnClickListener {
                val searchQuery = query().trim()
                if (searchQuery.isNotEmpty()) handOffToAiApp(searchQuery)
            }
        }

        // Sits with the other search row buttons, ahead of the contacts switcher.
        val position = binding.searchContainer.indexOfChild(binding.searchSwitcher)
            .takeIf { it >= 0 } ?: binding.searchContainer.childCount

        binding.searchContainer.addView(
            button,
            position,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    /** Hands the query over, saying what to fix when the target app is unset or refuses it. */
    private fun handOffToAiApp(query: String) {
        val aiApp = prefs.aiSearchApp
        if (aiApp.isBlank()) {
            context.showShortToast(getLocalizedString(R.string.ai_search_app_unset))
            return
        }
        if (!context.searchWithAiApp(query, prefs)) {
            context.showShortToast(
                getLocalizedString(R.string.ai_search_app_failed, context.getAppLabel(aiApp) ?: aiApp)
            )
        }
    }

    companion object {
        const val APPLIST_BUTTON_FLAGS = "APPLIST_BUTTON_FLAGS"

        /** Index of this fork's button in APPLIST_BUTTON_FLAGS, after web and contacts. */
        const val AI_BUTTON_FLAG = 2
    }
}
