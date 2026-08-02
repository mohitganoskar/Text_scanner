package com.example.specscan

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles appending scanned label text to a CSV file, one row per scan,
 * numbered sequentially. Stored in app-specific external storage so no
 * runtime storage permission is required on modern Android versions.
 */
class CsvStore(context: Context) {

    private val file: File = File(
        context.getExternalFilesDir(null) ?: context.filesDir,
        "spectacles_labels.csv"
    )

    init {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
            FileWriter(file, true).use {
                it.append("SerialNo,Timestamp,ScannedText\n")
            }
        }
    }

    fun getFile(): File = file

    /** Returns number of data rows currently saved (excludes header). */
    fun rowCount(): Int {
        if (!file.exists()) return 0
        return (file.readLines().size - 1).coerceAtLeast(0)
    }

    /** Appends a new row with the next sequential serial number. Returns the new row count. */
    fun appendEntry(text: String): Int {
        val nextSerial = rowCount() + 1
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val row = "${nextSerial},${escape(timestamp)},${escape(text)}\n"
        FileWriter(file, true).use { it.append(row) }
        return nextSerial
    }

    /** Basic RFC-4180 style CSV escaping. */
    private fun escape(value: String): String {
        val needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n")
        val cleaned = value.replace("\"", "\"\"")
        return if (needsQuoting) "\"$cleaned\"" else cleaned
    }
}
