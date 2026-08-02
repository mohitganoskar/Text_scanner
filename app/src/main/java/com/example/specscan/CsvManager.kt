package com.example.specscan

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages one or more CSV "sessions" (e.g. Scan_1.csv, MorningBatch.csv, ...),
 * all stored in the app's private external storage under csv_files/.
 * Exactly one file is "active" at a time (tracked in SharedPreferences) —
 * that's the one MainActivity appends new scans to.
 */
class CsvManager(context: Context) {

    private val dir: File = File(context.getExternalFilesDir(null), "csv_files").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("specscan_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENT_FILE = "current_file"
        private const val HEADER = "SerialNo,Timestamp,ScannedText\n"
    }

    /** Returns the active file name, creating a first default file if none exists yet. */
    fun getCurrentFileName(): String {
        val saved = prefs.getString(KEY_CURRENT_FILE, null)
        if (saved != null && File(dir, saved).exists()) return saved

        val existing = listFileNames()
        if (existing.isNotEmpty()) {
            setCurrentFileName(existing.first())
            return existing.first()
        }
        return createNewFile("Scan_1")
    }

    fun setCurrentFileName(name: String) {
        prefs.edit().putString(KEY_CURRENT_FILE, name).apply()
    }

    /** All CSV file names, most recently modified first. */
    fun listFileNames(): List<String> {
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".csv") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.name }
            ?: emptyList()
    }

    fun getFile(name: String): File = File(dir, name)

    /**
     * Creates a new, empty CSV file from a user-provided name and makes it active.
     * Sanitizes the name and avoids collisions. Returns the actual file name used.
     */
    fun createNewFile(baseName: String): String {
        val safeBase = baseName.trim()
            .replace(Regex("[^A-Za-z0-9 _-]"), "")
            .replace(" ", "_")
            .ifEmpty { "Scan" }

        var candidate = "$safeBase.csv"
        var counter = 1
        while (File(dir, candidate).exists()) {
            counter++
            candidate = "${safeBase}_$counter.csv"
        }

        val file = File(dir, candidate)
        FileWriter(file, false).use { it.append(HEADER) }
        setCurrentFileName(candidate)
        return candidate
    }

    /** Deletes a whole file. If it was the active one, another file becomes active automatically. */
    fun deleteFile(name: String): Boolean {
        val deleted = File(dir, name).delete()
        if (deleted && prefs.getString(KEY_CURRENT_FILE, null) == name) {
            val remaining = listFileNames()
            if (remaining.isNotEmpty()) setCurrentFileName(remaining.first())
            else prefs.edit().remove(KEY_CURRENT_FILE).apply()
        }
        return deleted
    }

    fun rowCount(name: String): Int {
        val file = File(dir, name)
        if (!file.exists()) return 0
        return (file.readLines().size - 1).coerceAtLeast(0)
    }

    /** Appends a new sequentially-numbered row. Newlines in the text are flattened to spaces. */
    fun appendEntry(name: String, rawText: String): Int {
        val file = File(dir, name)
        if (!file.exists()) FileWriter(file, false).use { it.append(HEADER) }

        val text = rawText.replace("\r\n", " ").replace("\n", " ").trim()
        val nextSerial = rowCount(name) + 1
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        FileWriter(file, true).use {
            it.append("${nextSerial},${escape(timestamp)},${escape(text)}\n")
        }
        return nextSerial
    }

    fun getEntries(name: String): List<Entry> {
        val file = File(dir, name)
        if (!file.exists()) return emptyList()
        val lines = file.readLines()
        if (lines.size <= 1) return emptyList()
        return lines.drop(1).mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val fields = parseCsvLine(line)
            if (fields.size < 3) return@mapNotNull null
            val serial = fields[0].toIntOrNull() ?: return@mapNotNull null
            Entry(serial, fields[1], fields[2])
        }
    }

    /** Deletes one row by its serial number and renumbers the remaining rows 1..N. */
    fun deleteEntry(name: String, serial: Int) {
        val remaining = getEntries(name).filter { it.serial != serial }
        val file = File(dir, name)
        FileWriter(file, false).use { writer ->
            writer.append(HEADER)
            remaining.forEachIndexed { index, entry ->
                writer.append("${index + 1},${escape(entry.timestamp)},${escape(entry.text)}\n")
            }
        }
    }

    /** Convenience: removes the most recently saved row, e.g. right after a bad scan. */
    fun deleteLastEntry(name: String): Boolean {
        val entries = getEntries(name)
        if (entries.isEmpty()) return false
        deleteEntry(name, entries.last().serial)
        return true
    }

    private fun escape(value: String): String {
        val needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n")
        val cleaned = value.replace("\"", "\"\"")
        return if (needsQuoting) "\"$cleaned\"" else cleaned
    }

    /** Minimal RFC-4180 parser: handles quoted fields, escaped quotes, and commas inside quotes. */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"'); i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    sb.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> { result.add(sb.toString()); sb.clear() }
                    else -> sb.append(c)
                }
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
