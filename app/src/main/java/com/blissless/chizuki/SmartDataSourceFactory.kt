package com.blissless.chizuki

import android.net.Uri
import android.util.Log
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import okhttp3.OkHttpClient

/**
 * Delegates to OkHttp for most URLs, but to Android's DefaultHttpDataSource
 * for hosts with "ironbubble" in their domain, because these CDNs check TLS
 * fingerprints and may reject OkHttp's native TLS stack.
 */
class SmartDataSourceFactory(
    private val okHttpClient: OkHttpClient
) : DataSource.Factory {
    private val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
    private val defaultFactory = DefaultHttpDataSource.Factory()

    fun setUserAgent(u: String) = apply {
        okHttpFactory.setUserAgent(u)
        defaultFactory.setUserAgent(u)
    }

    fun setDefaultRequestProperties(properties: Map<String, String>) = apply {
        okHttpFactory.setDefaultRequestProperties(properties)
        defaultFactory.setDefaultRequestProperties(properties)
    }

    override fun createDataSource(): DataSource {
        return SmartDataSource(
            okHttpFactory.createDataSource(),
            defaultFactory.createDataSource()
        )
    }

    class SmartDataSource(
        private val okHttpDataSource: OkHttpDataSource,
        private val defaultDataSource: DefaultHttpDataSource
    ) : DataSource {
        private var activeSource: DataSource? = null

        override fun addTransferListener(transferListener: TransferListener) {
            okHttpDataSource.addTransferListener(transferListener)
            defaultDataSource.addTransferListener(transferListener)
        }

        override fun open(dataSpec: DataSpec): Long {
            val uri = dataSpec.uri
            val host = uri.host ?: ""
            val path = uri.path ?: ""
            val scheme = uri.scheme ?: ""
            Log.d("SmartDataSource", "open: scheme=$scheme host=$host path=$path")
            Log.d("SmartDataSource", "open: full URI=$uri")
            Log.d("SmartDataSource", "open: dataSpec.position=${dataSpec.position} dataSpec.length=${dataSpec.length}")
            if (host.contains("ironbubble")) {
                Log.d("SmartDataSource", "open: -> using DefaultHttpDataSource (ironbubble detected)")
                activeSource = defaultDataSource
            } else {
                Log.d("SmartDataSource", "open: -> using OkHttpDataSource (default)")
                activeSource = okHttpDataSource
            }
            val startTime = System.currentTimeMillis()
            try {
                val result = activeSource!!.open(dataSpec)
                val openTime = System.currentTimeMillis() - startTime
                Log.d("SmartDataSource", "open: SUCCESS in ${openTime}ms, contentLength=$result")
                return result
            } catch (e: Exception) {
                val openTime = System.currentTimeMillis() - startTime
                Log.e("SmartDataSource", "open: FAILED in ${openTime}ms: ${e::class.java.simpleName}: ${e.message}")
                throw e
            }
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val result = activeSource?.read(buffer, offset, length) ?: 0
            if (result == -1) {
                Log.d("SmartDataSource", "read: EOF (-1) for ${activeSource?.uri?.host}")
            }
            return result
        }

        override fun getUri(): Uri? {
            return activeSource?.uri
        }

        override fun close() {
            activeSource?.close()
        }
    }
}
