package com.himel.xkcdviewer.downloader

data class Comic(
    val title: String,
    val num: Int,
    val imgURL: String,
    val transcript: String,
    val alt: String,
    val special: Boolean
)