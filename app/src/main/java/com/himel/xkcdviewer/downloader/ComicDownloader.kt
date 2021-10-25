package com.himel.xkcdviewer.downloader

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class ComicDownloader {
    companion object {
        private var latest: Int? = null
        private val cache = mutableMapOf<Int, Comic>()

        fun fetch(num: Int?): Comic? {
            var comicNumber = num ?: latest
            if (comicNumber != null && comicNumber in cache) {
                return cache[comicNumber]
            }

            val url = "https://xkcd.com/${comicNumber?.toString() ?: ""}/info.0.json"

            try {
                val connection = URL(url).openConnection() as HttpsURLConnection

                val json = JSONObject(
                    connection.inputStream.bufferedReader(charset = Charsets.UTF_8).readText()
                )

                val comic = Comic(
                    json.getString("title"),
                    json.getInt("num"),
                    json.getString("img"),
                    json.getString("transcript"),
                    json.getString("alt"),
                    json.has("extra_parts")
                )

                if (comicNumber == null) {
                    latest = comic.num
                    comicNumber = latest
                }

                synchronized(cache) {
                    cache[comicNumber!!] = comic
                }

                return comic

            } catch (e: IOException) {
            } catch (e: JSONException) {
            }

            return null
        }
    }
}