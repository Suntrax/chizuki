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
        Log.d(TAG, "===== discover START =====")
        val beaconIntent = Intent(BEACON_ACTION)
        Log.d(TAG, "discover: querying for broadcast receivers with action=$BEACON_ACTION")
        val resolveInfoList = context.packageManager.queryBroadcastReceivers(beaconIntent, 0)
        Log.d(TAG, "discover: queryBroadcastReceivers returned ${resolveInfoList.size} result(s)")
        for (info in resolveInfoList) {
            Log.d(TAG, "discover:   pkg=${info.activityInfo.packageName} label=${info.loadLabel(context.packageManager)}")
        }

        val extensions = resolveInfoList
            .filter { info ->
                val label = info.loadLabel(context.packageManager).toString()
                val matches = label.startsWith(LABEL_PREFIX, ignoreCase = true)
                Log.d(TAG, "discover:   ${info.activityInfo.packageName} label='$label' matches=$matches")
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
        for (ext in extensions) {
            Log.d(TAG, "discover:   -> ${ext.label} (pkg=${ext.packageName} authority=${ext.authority})")
        }
        Log.d(TAG, "===== discover END =====")
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
        val startTime = System.currentTimeMillis()
        val builder = Uri.parse("content://$authority/$PATH_SCRAPE")
            .buildUpon()
            .appendQueryParameter("title", title)
        if (tmdbId != null) builder.appendQueryParameter("tmdbId", tmdbId.toString())
        if (mediaType != null) builder.appendQueryParameter("mediaType", mediaType)
        if (season != null) builder.appendQueryParameter("season", season.toString())
        if (episode != null) builder.appendQueryParameter("episode", episode.toString())
        val uri = builder.build()

        Log.d(TAG, "===== fetchStreamUrl START =====")
        Log.d(TAG, "fetchStreamUrl: authority=$authority")
        Log.d(TAG, "fetchStreamUrl: title=$title tmdbId=$tmdbId mediaType=$mediaType season=$season episode=$episode")
        Log.d(TAG, "fetchStreamUrl: full query URI=$uri")
        Log.d(TAG, "fetchStreamUrl: uri.authority=${uri.authority} uri.path=${uri.path}")
        Log.d(TAG, "fetchStreamUrl: uri.queryParameterNames=${uri.queryParameterNames}")

        var cursor: Cursor? = null
        return try {
            Log.d(TAG, "fetchStreamUrl: calling contentResolver.query...")
            cursor = context.contentResolver.query(uri, arrayOf("data"), null, null, null)
            val queryTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "fetchStreamUrl: contentResolver.query returned in ${queryTime}ms")

            if (cursor == null) {
                Log.e(TAG, "fetchStreamUrl: cursor is NULL — ContentProvider not found or crashed")
                Log.e(TAG, "fetchStreamUrl: check that the extension APK is installed and its authority is '$authority'")
                return null
            }

            Log.d(TAG, "fetchStreamUrl: cursor.columnCount=${cursor.columnCount}")
            Log.d(TAG, "fetchStreamUrl: cursor.columnNames=${cursor.columnNames?.toList()}")
            Log.d(TAG, "fetchStreamUrl: cursor.count=${cursor.count}")

            if (!cursor.moveToFirst()) {
                Log.e(TAG, "fetchStreamUrl: cursor has NO ROWS (count=0)")
                return null
            }

            Log.d(TAG, "fetchStreamUrl: cursor has rows, reading 'data' column...")
            val json = cursor.getString(0)
            Log.d(TAG, "fetchStreamUrl: raw JSON response (${json?.length} chars) = $json")

            if (json == null) {
                Log.e(TAG, "fetchStreamUrl: JSON string is NULL")
                return null
            }

            if (json.isBlank()) {
                Log.e(TAG, "fetchStreamUrl: JSON string is BLANK")
                return null
            }

            val obj = JSONObject(json)
            Log.d(TAG, "fetchStreamUrl: parsed JSON keys = ${obj.keys().asSequence().toList()}")

            if (obj.has("error")) {
                Log.e(TAG, "fetchStreamUrl: extension returned ERROR: ${obj.optString("error")}")
                Log.e(TAG, "fetchStreamUrl: full error JSON = $json")
                return null
            }

            // Log ALL top-level keys and their types
            for (key in obj.keys()) {
                val value = obj.get(key)
                Log.d(TAG, "fetchStreamUrl:   key='$key' type=${value?.javaClass?.simpleName} value=${value?.toString()?.take(200)}")
            }

            // Log if "servers" object exists and its structure
            if (obj.has("servers")) {
                val servers = obj.optJSONObject("servers")
                Log.d(TAG, "fetchStreamUrl: 'servers' object found, keys=${servers?.keys()?.asSequence()?.toList()}")
                if (servers != null) {
                    for (serverKey in servers.keys()) {
                        val srvObj = servers.optJSONObject(serverKey)
                        if (srvObj != null) {
                            val m3u8 = srvObj.optString("m3u8", "")
                            val mp4 = srvObj.optString("mp4", "")
                            val dash = srvObj.optString("dash", "")
                            val error = srvObj.optString("error", "")
                            val sources = srvObj.optJSONArray("sources")
                            Log.d(TAG, "fetchStreamUrl:   server '$serverKey': m3u8=${m3u8.take(120)} mp4=${mp4.take(120)} dash=${dash.take(120)} error=$error sources=${sources?.length()}")
                        } else {
                            Log.d(TAG, "fetchStreamUrl:   server '$serverKey': NOT an object (value=$srvObj)")
                        }
                    }
                }
            }

            // Log if "Auto" array exists
            if (obj.has("Auto") || obj.has("auto")) {
                val autoArr = obj.optJSONArray("Auto") ?: obj.optJSONArray("auto")
                Log.d(TAG, "fetchStreamUrl: 'Auto' array found, length=${autoArr?.length()}")
                if (autoArr != null) {
                    for (i in 0 until autoArr.length()) {
                        Log.d(TAG, "fetchStreamUrl:   Auto[$i] = ${autoArr.optString(i)?.take(200)}")
                    }
                }
            }

            // Extract primary URL from "Auto" array or single fields
            val primaryUrl = extractPrimaryUrl(obj)

            Log.d(TAG, "fetchStreamUrl: FINAL primaryUrl=$primaryUrl")
            val totalTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "===== fetchStreamUrl END (${totalTime}ms) =====")
            StreamResult(rawJson = json, primaryUrl = primaryUrl)
        } catch (e: SecurityException) {
            Log.e(TAG, "fetchStreamUrl: SecurityException — ContentProvider not exported or permission denied", e)
            Log.e(TAG, "fetchStreamUrl: authority was '$authority', check AndroidManifest of extension")
            null
        } catch (e: Exception) {
            Log.e(TAG, "fetchStreamUrl: EXCEPTION: ${e::class.java.simpleName}: ${e.message}", e)
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
        Log.d(TAG, "extractPrimaryUrl: searching for stream URL in JSON keys=${obj.keys().asSequence().toList()}")
        for (key in listOf("Auto", "auto", "default", "urls")) {
            val arr = obj.optJSONArray(key)
            if (arr != null && arr.length() > 0) {
                Log.d(TAG, "extractPrimaryUrl: found '$key' array with ${arr.length()} entries")
                for (i in 0 until arr.length()) {
                    Log.d(TAG, "extractPrimaryUrl:   $key[$i] = ${arr.optString(i)?.take(200)}")
                }
                val url = arr.optString(0)
                if (url.isNotBlank()) {
                    Log.d(TAG, "extractPrimaryUrl: using '$key[0]' = $url")
                    return url
                } else {
                    Log.w(TAG, "extractPrimaryUrl: '$key[0]' is blank, skipping")
                }
            }
        }
        for (key in listOf("url", "stream", "m3u8", "playlist")) {
            val v = obj.optString(key, "")
            if (v.startsWith("http")) {
                Log.d(TAG, "extractPrimaryUrl: using '$key' field = $v")
                return v
            }
        }
        // Fallback: look inside "servers" object for the first server's URL
        val servers = obj.optJSONObject("servers")
        if (servers != null) {
            for (key in listOf("Hydrogen", "Titanium", "Oxygen", "Lithium", "Helium")) {
                val srv = servers.optJSONObject(key) ?: continue
                val url = srv.optString("m3u8", "")
                    .ifBlank { srv.optString("mp4", "") }
                    .ifBlank { srv.optString("dash", "") }
                if (url.isNotBlank()) {
                    Log.d(TAG, "extractPrimaryUrl: using servers.$key URL = $url")
                    return url
                }
            }
            // Last resort: first key in servers
            val firstKey = servers.keys().let { if (it.hasNext()) it.next() else null }
            if (firstKey != null) {
                val srv = servers.optJSONObject(firstKey)
                val url = srv?.optString("m3u8", "")
                    ?.ifBlank { srv.optString("mp4", "") }
                    ?.ifBlank { srv.optString("dash", "") }
                if (!url.isNullOrBlank()) {
                    Log.d(TAG, "extractPrimaryUrl: using servers.$firstKey URL = $url")
                    return url
                }
            }
        }
        Log.e(TAG, "extractPrimaryUrl: NO STREAM URL FOUND in response JSON")
        Log.e(TAG, "extractPrimaryUrl: all keys=${obj.keys().asSequence().toList()}")
        // Dump entire JSON for debugging
        Log.e(TAG, "extractPrimaryUrl: full JSON = $obj")
        return null
    }

    companion object {
        private const val TAG = "Chizuki/ExtensionMgr"
        const val BEACON_ACTION = "com.blissless.movieclient.EXTENSION_BEACON"
        const val LABEL_PREFIX = "Chizuki: "
        const val PATH_SCRAPE = "scrape"
    }
}
