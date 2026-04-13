package com.stx.meimo.log

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

object ApiLogger {
    private const val TAG = "ApiLogger"
    private val executor = Executors.newSingleThreadExecutor()
    private val pendingLogs = ConcurrentLinkedQueue<LogEntry>()
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var logDir: File? = null

    data class LogEntry(
        val timestamp: String,
        val type: String,
        val url: String,
        val method: String = "GET",
        val requestHeaders: Map<String, String>? = null,
        val requestBody: String? = null,
        val responseStatus: Int? = null,
        val responseHeaders: Map<String, String>? = null,
        val responseBody: String? = null,
        val pageUrl: String? = null,
        val extra: String? = null
    )

    fun init(context: Context) {
        logDir = File(context.getExternalFilesDir(null), "logs").apply { mkdirs() }
        Log.i(TAG, "Log dir: $logDir")
    }

    fun logRequest(entry: LogEntry) {
        pendingLogs.add(entry)
        executor.execute { flush() }
        Log.d(TAG, "[${entry.type}] ${entry.method} ${entry.url}")
    }

    fun logDom(pageUrl: String, domJson: String) {
        executor.execute {
            val dir = logDir ?: return@execute
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val safeName = pageUrl.replace(Regex("[^a-zA-Z0-9]"), "_").take(80)
            val file = File(dir, "dom_${ts}_${safeName}.json")
            try {
                val pretty = try {
                    gson.toJson(JsonParser.parseString(domJson))
                } catch (_: Exception) {
                    domJson
                }
                file.writeText(pretty)
                Log.i(TAG, "DOM saved: ${file.name}")
            } catch (e: Exception) {
                Log.e(TAG, "DOM save failed", e)
            }
        }
    }

    private fun flush() {
        val dir = logDir ?: return
        val entries = mutableListOf<LogEntry>()
        while (true) {
            val entry = pendingLogs.poll() ?: break
            entries.add(entry)
        }
        if (entries.isEmpty()) return

        val ts = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val file = File(dir, "api_$ts.jsonl")
        try {
            file.appendText(entries.joinToString("\n") { gson.toJson(it) } + "\n")
        } catch (e: Exception) {
            Log.e(TAG, "Log write failed", e)
        }
    }
}
