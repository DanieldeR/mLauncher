package com.github.codeworkscreativehub.fork

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.codeworkscreativehub.common.getLocalizedString
import com.github.codeworkscreativehub.common.isGestureNavigationEnabled
import com.github.codeworkscreativehub.common.showShortToast
import com.github.codeworkscreativehub.mlauncher.R
import com.github.codeworkscreativehub.mlauncher.data.Prefs
import com.github.codeworkscreativehub.mlauncher.databinding.FragmentAppDrawerBinding

/**
 * The fork's app drawer search row: moved to the bottom of the screen, tracking the keyboard,
 * with the result list stacked upwards so the best match sits next to the field being typed in.
 *
 * All of it lives here so that AppDrawerFragment.kt, which upstream edits often, only carries a
 * handful of call sites. When the search bar is configured at the top every method is a no-op and
 * the upstream layout and inset handling are left exactly as they are.
 */
class DrawerSearchBar(
    private val context: Context,
    private val binding: FragmentAppDrawerBinding,
    private val prefs: Prefs,
) {

    /** True while the search bar sits at the bottom, which also reverses the result lists. */
    val atBottom: Boolean = prefs.searchBarPosition == SearchBarPosition.Bottom

    /** Anchors the search row to the bottom of the screen and keeps it above the keyboard. */
    fun attach() {
        if (!atBottom) return

        val navBarMargin = context.resources.getDimensionPixelSize(
            if (isGestureNavigationEnabled(context)) R.dimen.bottom_margin_gesture_nav
            else R.dimen.bottom_margin_3_button_nav
        )

        anchorToBottom(navBarMargin)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainLayout) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            // The keyboard covers the navigation bar, so the search row rides on whichever of the
            // two is currently taller and the list keeps the rest of the screen.
            setBottomMargin(binding.searchContainer, maxOf(imeInsets.bottom, navBarMargin))
            setBottomMargin(binding.menuView, 0)

            insets
        }
    }

    private fun anchorToBottom(navBarMargin: Int) {
        val topMargin = context.resources.getDimensionPixelSize(R.dimen.fork_app_drawer_top_margin)

        val searchParams = binding.searchContainer.layoutParams as RelativeLayout.LayoutParams
        val clearHomeParams = binding.clearHomeButton.layoutParams as RelativeLayout.LayoutParams
        val menuParams = binding.menuView.layoutParams as RelativeLayout.LayoutParams

        searchParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
        searchParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        searchParams.topMargin = 0
        searchParams.bottomMargin = navBarMargin

        clearHomeParams.removeRule(RelativeLayout.BELOW)
        clearHomeParams.addRule(RelativeLayout.ABOVE, R.id.searchContainer)

        menuParams.removeRule(RelativeLayout.BELOW)
        menuParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        menuParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        menuParams.addRule(RelativeLayout.ABOVE, R.id.clearHomeButton)
        menuParams.topMargin = topMargin
        menuParams.bottomMargin = 0

        binding.searchContainer.layoutParams = searchParams
        binding.clearHomeButton.layoutParams = clearHomeParams
        binding.menuView.layoutParams = menuParams
    }

    private fun setBottomMargin(view: View, margin: Int) {
        val params = view.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = margin
        view.layoutParams = params
    }

    /** Lists grow upwards when the search bar is at the bottom, so item 0 sits next to it. */
    fun layoutManager(): LinearLayoutManager =
        LinearLayoutManager(context).apply { reverseLayout = atBottom }

    /** True when the first item of the list is in view, whichever way the list is stacked. */
    fun isAtListStart(recyclerView: RecyclerView): Boolean =
        !recyclerView.canScrollVertically(if (atBottom) 1 else -1)

    /** True when the last item of the list is in view, whichever way the list is stacked. */
    fun isAtListEnd(recyclerView: RecyclerView): Boolean =
        !recyclerView.canScrollVertically(if (atBottom) -1 else 1)

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
