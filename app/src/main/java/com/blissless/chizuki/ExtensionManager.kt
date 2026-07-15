package com.blissless.chizuki

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.util.Log
import org.json.JSONObject

/**
 * Discovers installed streaming extensions and resolves stream URLs through them.
 *
 * Extensions are headless APKs whose manifest declares a BroadcastReceiver
 * listening for `com.blissless.movieclient.EXTENSION_BEACON` and whose
 * app label starts with "Chizuki: ". Each extension exposes a
 * ContentProvider at authority `"<packageName>.provider"` that accepts:
 *
 *   content://<authority>/scrape?title=<movie name>[&tmdbId=N&mediaType=movie|tv&season=N&episode=M]
 *
 * and returns a single-row cursor with a "data" column containing JSON:
 *   {"Auto": ["https://...m3u8..."]}    on success
 *   {"error": "..."}                     on failure
 */
data class InstalledExtension(
    val label: String,
    val packageName: String
) {
    val authority: String get() = "$packageName.provider"
}

class ExtensionManager(private val context: Context) {

    /**
     * Query the PackageManager for installed extensions.
     * Extensions are apps whose label starts with "Chizuki: " and that
     * respond to the EXTENSION_BEACON broadcast.
     */
    fun discover(): List<InstalledExtension> {
        val beaconIntent = Intent(BEACON_ACTION)
        val resolveInfoList = context.packageManager.queryBroadcastReceivers(beaconIntent, 0)
        Log.d(TAG, "discover: queryBroadcastReceivers returned ${resolveInfoList.size} result(s)")

        val extensions = resolveInfoList
            .filter { info ->
                val label = info.loadLabel(context.packageManager).toString()
                val matches = label.startsWith(LABEL_PREFIX, ignoreCase = true)
                Log.d(TAG, "discover:   ${info.activityInfo.packageName} label=$label matches=$matches")
                matches
            }
            .map { info ->
                InstalledExtension(
                    label = info.loadLabel(context.packageManager).toString(),
                    packageName = info.activityInfo.packageName
                )
            }
            .sortedBy { it.label.lowercase() }

        Log.d(TAG, "discover: found ${extensions.size} extension(s)")
        return extensions
    }

    /**
     * Call the selected extension's ContentProvider to resolve a stream URL.
     *
     * @param authority  the extension's ContentProvider authority
     *                   (e.g. "com.blissless.movies67.provider")
     * @param title      the movie/show title
     * @param tmdbId     the TMDB ID of the content (optional — extensions can
     *                   use it to skip their own TMDB search)
     * @param mediaType  "movie" or "tv" (optional — lets extensions construct
     *                   the correct embed URL without guessing)
     * @param season     optional season number (TV only)
     * @param episode    optional episode number (TV only)
     * @return the first stream URL on success, null on failure or empty result
     */
    /**
     * Result of fetching a stream from an extension.
     */
    data class StreamResult(
        /** The raw JSON string from the extension (may contain servers, subtitles, etc.) */
        val rawJson: String,
        /** The primary stream URL to play (from Auto[0] or simple url field) */
        val primaryUrl: String?
    )

    fun fetchStreamUrl(
        authority: String,
        title: String,
        tmdbId: Int? = null,
        mediaType: String? = null,
        season: Int? = null,
        episode: Int? = null
    ): StreamResult? {
        val builder = Uri.parse("content://$authority/$PATH_SCRAPE")
            .buildUpon()
            .appendQueryParameter("title", title)
        if (tmdbId != null) builder.appendQueryParameter("tmdbId", tmdbId.toString())
        if (mediaType != null) builder.appendQueryParameter("mediaType", mediaType)
        if (season != null) builder.appendQueryParameter("season", season.toString())
        if (episode != null) builder.appendQueryParameter("episode", episode.toString())
        val uri = builder.build()

        Log.d(TAG, "fetchStreamUrl: authority=$authority")
        Log.d(TAG, "fetchStreamUrl: title=$title tmdbId=$tmdbId mediaType=$mediaType season=$season episode=$episode")
        Log.d(TAG, "fetchStreamUrl: query URI=$uri")

        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, arrayOf("data"), null, null, null)
            if (cursor == null) {
                Log.e(TAG, "fetchStreamUrl: cursor is null — ContentProvider not found or crashed")
                return null
            }
            if (!cursor.moveToFirst()) {
                Log.e(TAG, "fetchStreamUrl: cursor has no rows")
                return null
            }

            val json = cursor.getString(0)
            Log.d(TAG, "fetchStreamUrl: raw JSON response = $json")

            if (json == null) {
                Log.e(TAG, "fetchStreamUrl: JSON string is null")
                return null
            }

            val obj = JSONObject(json)
            if (obj.has("error")) {
                Log.e(TAG, "fetchStreamUrl: extension returned error: ${obj.optString("error")}")
                return null
            }

            // Extract primary URL from "Auto" array or single fields
            val primaryUrl = extractPrimaryUrl(obj)

            Log.d(TAG, "fetchStreamUrl: primaryUrl=$primaryUrl")
            StreamResult(rawJson = json, primaryUrl = primaryUrl)
        } catch (e: SecurityException) {
            Log.e(TAG, "fetchStreamUrl: SecurityException — ContentProvider not exported?", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "fetchStreamUrl: exception", e)
            null
        } finally {
            cursor?.close()
        }
    }

    /**
     * Extract the primary stream URL from a parsed JSON response.
     * Checks "Auto" array first, then simple URL fields.
     */
    private fun extractPrimaryUrl(obj: JSONObject): String? {
        for (key in listOf("Auto", "auto", "default", "urls")) {
            val arr = obj.optJSONArray(key)
            if (arr != null && arr.length() > 0) {
                val url = arr.optString(0)
                if (url.isNotBlank()) {
                    Log.d(TAG, "extractPrimaryUrl: from '$key' array: $url")
                    return url
                }
            }
        }
        for (key in listOf("url", "stream", "m3u8", "playlist")) {
            val v = obj.optString(key, "")
            if (v.startsWith("http")) {
                Log.d(TAG, "extractPrimaryUrl: from '$key' field: $v")
                return v
            }
        }
        Log.e(TAG, "extractPrimaryUrl: no stream URL found in response JSON")
        return null
    }

    companion object {
        private const val TAG = "Chizuki/ExtensionMgr"
        const val BEACON_ACTION = "com.blissless.movieclient.EXTENSION_BEACON"
        const val LABEL_PREFIX = "Chizuki: "
        const val PATH_SCRAPE = "scrape"
    }
}
