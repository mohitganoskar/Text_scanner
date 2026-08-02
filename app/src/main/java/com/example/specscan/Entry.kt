package com.example.specscan

/** One saved row: sequential number within its file, when it was saved, and the label text. */
data class Entry(
    val serial: Int,
    val timestamp: String,
    val text: String
)
