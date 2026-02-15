package com.udyneos.animex.utils

import com.udyneos.animex.model.Anime
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

object XmlParser {
    
    fun parseAnimeList(inputStream: InputStream): List<Anime> {
        val animeList = mutableListOf<Anime>()
        
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        
        var eventType = parser.eventType
        var currentAnime: Anime.Builder? = null
        
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "anime" -> currentAnime = Anime.Builder()
                        "id" -> currentAnime?.id = parser.nextText().toInt()
                        "title" -> currentAnime?.title = parser.nextText()
                        "description" -> currentAnime?.description = parser.nextText()
                        "genre" -> currentAnime?.genre = parser.nextText()
                        "episode_count" -> currentAnime?.episodeCount = parser.nextText()
                        "thumbnail_url" -> currentAnime?.thumbnailUrl = parser.nextText()
                        "video_url" -> currentAnime?.videoUrl = parser.nextText()
                        "status" -> currentAnime?.status = parser.nextText()
                        "rating" -> currentAnime?.rating = parser.nextText().toDouble()
                        "release_year" -> currentAnime?.releaseYear = parser.nextText().toInt()
                        "studio" -> currentAnime?.studio = parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "anime") {
                        currentAnime?.let {
                            animeList.add(it.build())
                            currentAnime = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        
        return animeList
    }
}
