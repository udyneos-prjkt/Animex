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
        var id = 0
        var title = ""
        var description = ""
        var genre = ""
        var episodeCount = ""
        var thumbnailUrl = ""
        var videoUrl = ""
        var status = ""
        var rating = 0.0
        var releaseYear = 0
        var studio = ""
        
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "id" -> id = parser.nextText().toInt()
                    "title" -> title = parser.nextText()
                    "description" -> description = parser.nextText()
                    "genre" -> genre = parser.nextText()
                    "episode_count" -> episodeCount = parser.nextText()
                    "thumbnail_url" -> thumbnailUrl = parser.nextText()
                    "video_url" -> videoUrl = parser.nextText()
                    "status" -> status = parser.nextText()
                    "rating" -> rating = parser.nextText().toDouble()
                    "release_year" -> releaseYear = parser.nextText().toInt()
                    "studio" -> studio = parser.nextText()
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.name == "anime") {
                animeList.add(
                    Anime(
                        id, title, description, genre, episodeCount,
                        thumbnailUrl, videoUrl, status, rating, releaseYear, studio
                    )
                )
            }
            eventType = parser.next()
        }
        
        return animeList
    }
}
