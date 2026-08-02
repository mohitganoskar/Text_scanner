package com.example.specscan

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView

class EntriesAdapter(
    private val context: Context,
    private var entries: List<Entry>,
    private val onDeleteClicked: (Entry) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = entries.size
    override fun getItem(position: Int): Entry = entries[position]
    override fun getItemId(position: Int): Long = entries[position].serial.toLong()

    fun updateEntries(newEntries: List<Entry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_entry, parent, false)
        val entry = entries[position]

        view.findViewById<TextView>(R.id.serialText).text = entry.serial.toString()
        view.findViewById<TextView>(R.id.textText).text = entry.text
        view.findViewById<TextView>(R.id.timestampText).text = entry.timestamp
        view.findViewById<ImageButton>(R.id.deleteEntryButton).setOnClickListener {
            onDeleteClicked(entry)
        }

        return view
    }
}
