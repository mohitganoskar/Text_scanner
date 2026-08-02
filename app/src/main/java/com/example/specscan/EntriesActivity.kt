package com.example.specscan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider

class EntriesActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_NAME = "file_name"
    }

    private lateinit var csvManager: CsvManager
    private lateinit var fileName: String
    private lateinit var listView: ListView
    private lateinit var emptyText: TextView
    private lateinit var adapter: EntriesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entries)

        fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: run { finish(); return }
        csvManager = CsvManager(this)
        listView = findViewById(R.id.entriesListView)
        emptyText = findViewById(R.id.emptyText)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = fileName
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = EntriesAdapter(this, emptyList()) { entry -> confirmDeleteEntry(entry) }
        listView.adapter = adapter

        findViewById<View>(R.id.setActiveButton).setOnClickListener {
            csvManager.setCurrentFileName(fileName)
            Toast.makeText(this, "\"$fileName\" is now the active file", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.shareFileButton).setOnClickListener { shareFile() }

        findViewById<View>(R.id.deleteFileButton).setOnClickListener { confirmDeleteFile() }

        refreshEntries()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun refreshEntries() {
        val entries = csvManager.getEntries(fileName).sortedByDescending { it.serial }
        adapter.updateEntries(entries)
        emptyText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDeleteEntry(entry: Entry) {
        AlertDialog.Builder(this)
            .setTitle("Delete entry #${entry.serial}?")
            .setMessage(entry.text)
            .setPositiveButton("Delete") { _, _ ->
                csvManager.deleteEntry(fileName, entry.serial)
                refreshEntries()
                Toast.makeText(this, "Deleted — you can scan that pair again", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteFile() {
        AlertDialog.Builder(this)
            .setTitle("Delete \"$fileName\"?")
            .setMessage("This permanently deletes the whole file and all its entries. This can't be undone.")
            .setPositiveButton("Delete") { _, _ ->
                csvManager.deleteFile(fileName)
                Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareFile() {
        val file = csvManager.getFile(fileName)
        if (!file.exists() || csvManager.rowCount(fileName) == 0) {
            Toast.makeText(this, "No entries to share yet", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share $fileName"))
    }
}
