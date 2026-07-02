package com.blissless.chizuki

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Checks GitHub for a newer release of Chizuki.
 *
 * Uses raw HttpURLConnection + org.json (matches the rest of Chizuki's
 * networking — no OkHttp / Gson dependency).
 *
 * The "check for updates on start" feature calls [checkSilently] from
 * MainViewModel.init. The Settings screen can call [checkManually] to
 * surface a toast / open the releases page in a browser.
 */
data class GitHubRelease(
    val tagName: String,
    val name: String,
    val htmlUrl: String,
    val releaseNotes: String
)

class UpdateManager(private val context: Context) {

    /**
     * Silent check — returns the latest release if it's newer than the
     * currently-installed version, or null otherwise. Never throws.
     */
    suspend fun checkSilently(): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val release = fetchLatestRelease() ?: return@withContext null
            val currentVersion = getCurrentVersionName()
            if (compareVersions(release.tagName.removePrefix("v"), currentVersion) > 0) {
                release
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Open the GitHub releases page in the system browser. Used by the
     * Settings screen when an update is available — we don't auto-download
     * APKs, we just deep-link the user to the release page.
     */
    fun openReleasesPage() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // No browser installed — silently ignore.
        }
    }

    fun getCurrentVersionName(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }

    private fun fetchLatestRelease(): GitHubRelease? {
        val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github.v3+json")
            setRequestProperty("User-Agent", "Chizuki-Update-Check")
        }
        try {
            if (conn.responseCode !in 200..299) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val obj = JSONObject(body)
            return GitHubRelease(
                tagName = obj.optString("tag_name", ""),
                name = obj.optString("name", ""),
                htmlUrl = obj.optString("html_url", RELEASES_URL),
                releaseNotes = obj.optString("body", "")
            )
        } finally {
            conn.disconnect()
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }

    companion object {
        // Change these if Chizuki moves to a different GitHub org/repo.
        private const val OWNER = "Suntrax"
        private const val REPO = "chizuki"
        private const val API_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
        private const val RELEASES_URL = "https://github.com/$OWNER/$REPO/releases/latest"
    }
}
