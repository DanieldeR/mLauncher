package com.github.codeworkscreativehub.fork

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri
import com.github.codeworkscreativehub.common.AppLogger
import com.github.codeworkscreativehub.common.CrashHandler
import com.github.codeworkscreativehub.mlauncher.data.Prefs

/**
 * Hands a search query to a locally installed AI app instead of a search engine.
 *
 * Kept out of ContextExtensions.kt so this fork does not carry a permanent diff in a file
 * upstream edits regularly.
 */

/**
 * Deep links that open a known assistant app on a fresh conversation seeded with the query.
 * Tried before a plain text share, which every assistant app accepts but which usually needs
 * one more tap inside the app.
 */
private val aiAppQueryLinks = mapOf(
    "com.anthropic.claude" to "https://claude.ai/new?q=",
    "com.openai.chatgpt" to "https://chatgpt.com/?q=",
    "ai.perplexity.app.android" to "https://www.perplexity.ai/search?q=",
)

/**
 * Hands [searchQuery] to the app configured in [Prefs.aiSearchApp]. Tries that app's own
 * "new conversation" deep link first and falls back to sharing the text to it.
 * Returns false when no app is configured or the query could not be delivered.
 */
fun Context.searchWithAiApp(searchQuery: String, prefs: Prefs): Boolean {
    val packageName = prefs.aiSearchApp
    if (packageName.isBlank() || searchQuery.isBlank()) return false

    aiAppQueryLinks[packageName]?.let { linkPrefix ->
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, "$linkPrefix${Uri.encode(searchQuery)}".toUri()).apply {
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (startAiAppIntent(deepLinkIntent, packageName)) return true
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, searchQuery)
        setPackage(packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return startAiAppIntent(shareIntent, packageName)
}

private fun Context.startAiAppIntent(intent: Intent, packageName: String): Boolean {
    return try {
        if (intent.resolveActivity(packageManager) == null) return false
        startActivity(intent)
        CrashHandler.logUserAction("AI App Search Launched")
        true
    } catch (e: Exception) {
        AppLogger.d("searchWithAiApp", "$packageName could not handle the query: $e")
        false
    }
}

/** Apps that can receive a query as shared text, which is every assistant app worth listing. */
fun Context.aiAppCandidates(): List<Pair<String, String>> {
    val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "text/plain" }
    return packageManager.queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY)
        .distinctBy { it.activityInfo.packageName }
        .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
        .sortedBy { it.second.lowercase() }
}

/** Human readable label for [packageName], or null when it is blank or not installed. */
fun Context.getAppLabel(packageName: String): String? {
    if (packageName.isBlank()) return null
    return try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        AppLogger.d("getAppLabel", "$packageName is not installed: $e")
        null
    }
}
