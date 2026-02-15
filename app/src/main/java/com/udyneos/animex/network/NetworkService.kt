package com.udyneos.animex.network

import com.udyneos.animex.model.Anime
import com.udyneos.animex.model.Episode
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NetworkService {

    private const val GITHUB_BASE_URL = "https://raw.githubusercontent.com/udyneos-prjkt/Animex-data/refs/heads/main/"

    suspend fun fetchAnimeList(): List<Anime> = withContext(Dispatchers.IO) {
        val animeList = mutableListOf<Anime>()
        
        // Coba ambil untuk anime id 1 sampai 5
        for (animeId in 1..5) {
            try {
                val url = URL("${GITHUB_BASE_URL}animelist_${animeId}.xml")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                
                if (connection.responseCode == 200) {
                    val xmlString = connection.inputStream.bufferedReader().use { it.readText() }
                    val anime = parseAnimeFromXml(xmlString, animeId)
                    if (anime != null) {
                        animeList.add(anime)
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                // Skip jika error
                continue
            }
        }
        
        return@withContext animeList
    }
    
    private fun parseAnimeFromXml(xmlString: String, animeId: Int): Anime? {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlString))
            
            var eventType = parser.eventType
            var title = ""
            var description = ""
            var genre = ""
            var episodeCount = ""
            var thumbnailUrl = ""
            var status = ""
            var rating = 0.0
            var releaseYear = 0
            var studio = ""
            var foundAnime = false
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "anime" -> foundAnime = true
                        "title" -> if (foundAnime) title = parser.nextText() ?: ""
                        "description" -> if (foundAnime) description = parser.nextText() ?: ""
                        "genre" -> if (foundAnime) genre = parser.nextText() ?: ""
                        "episode_count" -> if (foundAnime) episodeCount = parser.nextText() ?: ""
                        "thumbnail_url" -> if (foundAnime) thumbnailUrl = parser.nextText() ?: ""
                        "status" -> if (foundAnime) status = parser.nextText() ?: ""
                        "rating" -> if (foundAnime) {
                            try {
                                rating = parser.nextText().toDouble()
                            } catch (e: Exception) {
                                rating = 0.0
                            }
                        }
                        "release_year" -> if (foundAnime) {
                            try {
                                releaseYear = parser.nextText().toInt()
                            } catch (e: Exception) {
                                releaseYear = 0
                            }
                        }
                        "studio" -> if (foundAnime) studio = parser.nextText() ?: ""
                    }
                }
                eventType = parser.next()
            }
            
            if (title.isNotEmpty()) {
                Anime(
                    id = animeId,
                    title = title,
                    description = description,
                    genre = genre,
                    episodeCount = episodeCount,
                    thumbnailUrl = thumbnailUrl,
                    videoUrl = "",
                    status = status,
                    rating = rating,
                    releaseYear = releaseYear,
                    studio = studio
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchEpisodes(animeId: Int): List<Episode> = withContext(Dispatchers.IO) {
        val episodeList = mutableListOf<Episode>()
        try {
            val url = URL("${GITHUB_BASE_URL}animelist_${animeId}.xml")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val xmlString = connection.inputStream.bufferedReader().use { it.readText() }
                
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(StringReader(xmlString))

                var eventType = parser.eventType
                var epsId = 0
                var videoUrl = ""
                var inEpisode = false

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name) {
                                "episode" -> {
                                    inEpisode = true
                                    epsId = 0
                                    videoUrl = ""
                                }
                                "eps_id" -> if (inEpisode) {
                                    try {
                                        epsId = parser.nextText().toInt()
                                    } catch (e: Exception) {
                                        epsId = 0
                                    }
                                }
                                "video_url" -> if (inEpisode) {
                                    videoUrl = parser.nextText() ?: ""
                                    // Bersihkan URL dari karakter < dan >
                                    videoUrl = videoUrl.replace("<", "").replace(">", "").trim()
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "episode" && inEpisode && epsId > 0 && videoUrl.isNotEmpty()) {
                                episodeList.add(
                                    Episode(
                                        id = epsId,
                                        animeId = animeId,
                                        number = epsId,
                                        title = "Episode $epsId",
                                        duration = "24:30",
                                        videoUrl = videoUrl,
                                        thumbnailUrl = ""
                                    )
                                )
                                inEpisode = false
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext episodeList
    }
}
