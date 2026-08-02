package com.example.specscan

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class FilesAdapter(
    private val context: Context,
    private var files: List<String>,
    private val activeFile: String,
    private val entryCounts: Map<String, Int>
) : BaseAdapter() {

    override fun getCount(): Int = files.size
    override fun getItem(position: Int): String = files[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_file, parent, false)
        val name = files[position]

        val nameText = view.findViewById<TextView>(R.id.fileNameText)
        val countText = view.findViewById<TextView>(R.id.entryCountText)
        val activeLabel = view.findViewById<TextView>(R.id.activeLabel)

        nameText.text = name
        val count = entryCounts[name] ?: 0
        countText.text = if (count == 1) "1 entry" else "$count entries"
        activeLabel.visibility = if (name == activeFile) View.VISIBLE else View.GONE

        return view
    }
}
