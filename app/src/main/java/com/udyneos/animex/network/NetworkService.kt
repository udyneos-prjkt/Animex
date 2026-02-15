package com.udyneos.animex.network

import android.content.Context
import com.udyneos.animex.model.Anime
import com.udyneos.animex.model.Episode
import com.udyneos.animex.utils.CacheManager
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object NetworkService {

    private const val GITHUB_BASE_URL = "https://raw.githubusercontent.com/udyneos-prjkt/Animex-data/refs/heads/main/"
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    suspend fun fetchAnimeList(forceRefresh: Boolean = false): List<Anime> = withContext(Dispatchers.IO) {
        val ctx = context ?: throw IllegalStateException("NetworkService not initialized")
        
        // Coba load dari cache dulu (kecuali force refresh)
        if (!forceRefresh) {
            val cached = CacheManager.loadAnimeList(ctx)
            if (cached != null) {
                println("📦 Using cached anime list")
                return@withContext cached
            }
        }
        
        println("🌐 Fetching anime list from GitHub...")
        val animeList = mutableListOf<Anime>()
        
        try {
            // Coba ambil untuk anime id 1 sampai 50
            for (animeId in 1..20000) {
                try {
                    val url = URL("${GITHUB_BASE_URL}animelist_${animeId}.xml")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 1000
                    connection.readTimeout = 1000
                    connection.requestMethod = "GET"
                    
                    if (connection.responseCode == 200) {
                        val xmlString = connection.inputStream.bufferedReader().use { it.readText() }
                        val anime = parseAnimeFromXml(xmlString, animeId)
                        if (anime != null) {
                            animeList.add(anime)
                            println("✅ Loaded anime ID $animeId: ${anime.title}")
                        }
                    } else {
                        connection.disconnect()
                        // Jika 3 kali berturut-turut tidak ditemukan, berhenti
                        if (animeId > 7 && animeList.isEmpty()) break
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    println("⚠️ Error loading anime ID $animeId: ${e.message}")
                }
            }
            
            // Simpan ke cache
            if (animeList.isNotEmpty()) {
                CacheManager.saveAnimeList(ctx, animeList)
            }
            
        } catch (e: Exception) {
            println("❌ Error: ${e.message}")
        }
        
        println("📊 Total anime loaded: ${animeList.size}")
        return@withContext animeList.sortedBy { it.id }
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
                        "animetitle" -> if (foundAnime) title = parser.nextText() ?: ""
                        "description" -> if (foundAnime) description = parser.nextText() ?: ""
                        "genre" -> if (foundAnime) genre = parser.nextText() ?: ""
                        "episode_count" -> if (foundAnime) episodeCount = parser.nextText() ?: ""
                        "thumbnail_url" -> if (foundAnime) {
                            thumbnailUrl = parser.nextText() ?: ""
                            thumbnailUrl = thumbnailUrl.trim()
                        }
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
            null
        }
    }

    suspend fun fetchEpisodes(animeId: Int, forceRefresh: Boolean = false): List<Episode> = withContext(Dispatchers.IO) {
        val ctx = context ?: throw IllegalStateException("NetworkService not initialized")
        
        // Coba load dari cache dulu (kecuali force refresh)
        if (!forceRefresh) {
            val cached = CacheManager.loadEpisodes(ctx, animeId)
            if (cached != null) {
                println("📦 Using cached episodes for anime $animeId")
                return@withContext cached
            }
        }
        
        println("🌐 Fetching episodes for anime $animeId from GitHub...")
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
                var episodeTitle = ""
                var inEpisode = false

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name) {
                                "episode" -> {
                                    inEpisode = true
                                    epsId = 0
                                    videoUrl = ""
                                    episodeTitle = ""
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
                                    videoUrl = videoUrl.replace("<", "").replace(">", "").trim()
                                }
                                "epstitle" -> if (inEpisode) {
                                    episodeTitle = parser.nextText() ?: ""
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "episode" && inEpisode && epsId > 0 && videoUrl.isNotEmpty()) {
                                val finalTitle = if (episodeTitle.isNotEmpty()) {
                                    episodeTitle
                                } else {
                                    "Episode $epsId"
                                }
                                
                                episodeList.add(
                                    Episode(
                                        id = epsId,
                                        animeId = animeId,
                                        number = epsId,
                                        title = finalTitle,
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
                
                // Simpan ke cache
                if (episodeList.isNotEmpty()) {
                    CacheManager.saveEpisodes(ctx, animeId, episodeList)
                }
                
                println("✅ Loaded ${episodeList.size} episodes for anime ID $animeId")
            }
            connection.disconnect()
        } catch (e: Exception) {
            println("❌ Error loading episodes for anime ID $animeId: ${e.message}")
        }
        
        return@withContext episodeList.sortedBy { it.number }
    }
}
