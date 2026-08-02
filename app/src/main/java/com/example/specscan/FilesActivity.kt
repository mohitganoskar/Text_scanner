package com.example.specscan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class FilesActivity : AppCompatActivity() {

    private lateinit var csvManager: CsvManager
    private lateinit var listView: ListView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files)

        csvManager = CsvManager(this)
        listView = findViewById(R.id.filesListView)
        emptyText = findViewById(R.id.emptyText)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<View>(R.id.newFileButton).setOnClickListener { showNewFileDialog() }

        listView.setOnItemClickListener { _, _, position, _ ->
            val name = csvManager.listFileNames()[position]
            startActivity(Intent(this, EntriesActivity::class.java).apply {
                putExtra(EntriesActivity.EXTRA_FILE_NAME, name)
            })
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun refreshList() {
        val files = csvManager.listFileNames()
        val active = csvManager.getCurrentFileName()
        val counts = files.associateWith { csvManager.rowCount(it) }

        listView.adapter = FilesAdapter(this, files, active, counts)
        emptyText.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showNewFileDialog() {
        val input = EditText(this).apply {
            hint = "e.g. MorningBatch"
        }
        AlertDialog.Builder(this)
            .setTitle("New file name")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString()
                if (name.isNotBlank()) {
                    csvManager.createNewFile(name)
                    refreshList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
