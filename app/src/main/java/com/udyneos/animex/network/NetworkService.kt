package com.udyneos.animex.network

import com.udyneos.animex.model.Anime
import com.udyneos.animex.model.Episode
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

object NetworkService {

    private const val GITHUB_BASE_URL = "https://raw.githubusercontent.com/udyneos-prjkt/Animex-data/refs/heads/main/"
    private const val GITHUB_API_URL = "https://api.github.com/repos/udyneos-prjkt/Animex-data/contents/"

    suspend fun fetchAnimeList(): List<Anime> = withContext(Dispatchers.IO) {
        val animeList = mutableListOf<Anime>()
        
        try {
            // 1. Dapatkan daftar semua file dari GitHub API
            val fileList = getAnimeFileList()
            println("📁 Found ${fileList.size} anime files")
            
            // 2. Download semua file secara parallel
            val results = fileList.map { fileName ->
                async {
                    try {
                        val animeId = fileName.replace("animelist_", "").replace(".xml", "").toIntOrNull()
                        if (animeId != null) {
                            val url = URL("${GITHUB_BASE_URL}${fileName}")
                            val connection = url.openConnection() as HttpURLConnection
                            connection.connectTimeout = 5000
                            connection.readTimeout = 5000
                            connection.requestMethod = "GET"
                            
                            if (connection.responseCode == 200) {
                                val xmlString = connection.inputStream.bufferedReader().use { it.readText() }
                                connection.disconnect()
                                
                                val anime = parseAnimeFromXml(xmlString, animeId)
                                if (anime != null) {
                                    println("✅ Loaded: ${fileName} - ${anime.title}")
                                    anime
                                } else {
                                    println("❌ Failed to parse: ${fileName}")
                                    null
                                }
                            } else {
                                connection.disconnect()
                                null
                            }
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        println("❌ Error loading ${fileName}: ${e.message}")
                        null
                    }
                }
            }
            
            // 3. Kumpulkan semua hasil yang berhasil
            animeList.addAll(results.awaitAll().filterNotNull())
            
        } catch (e: Exception) {
            println("❌ Error fetching file list: ${e.message}")
            
            // Fallback: coba manual untuk id 1-50 jika API gagal
            for (animeId in 1..50) {
                try {
                    val url = URL("${GITHUB_BASE_URL}animelist_${animeId}.xml")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
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
                        if (animeId > 10) break // Stop jika sudah lewat id 10 dan tidak ditemukan
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    if (animeId > 10) break
                }
            }
        }
        
        println("📊 Total anime loaded: ${animeList.size}")
        return@withContext animeList.sortedBy { it.id }
    }
    
    private suspend fun getAnimeFileList(): List<String> = withContext(Dispatchers.IO) {
        val fileList = mutableListOf<String>()
        
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            if (connection.responseCode == 200) {
                val jsonResponse = connection.inputStream.bufferedReader().use { it.readText() }
                
                // Parse JSON sederhana untuk mendapatkan nama file
                val pattern = "\"name\":\"(animelist_\\d+\\.xml)\"".toRegex()
                val matches = pattern.findAll(jsonResponse)
                
                matches.forEach { matchResult ->
                    matchResult.groupValues.getOrNull(1)?.let { fileName ->
                        fileList.add(fileName)
                    }
                }
                
                println("📁 Found ${fileList.size} files via GitHub API")
            } else {
                println("⚠️ GitHub API returned ${connection.responseCode}")
            }
            connection.disconnect()
        } catch (e: Exception) {
            println("⚠️ GitHub API error: ${e.message}")
        }
        
        // Jika API gagal, gunakan range manual
        if (fileList.isEmpty()) {
            println("📁 Using fallback: scanning IDs 1-50")
            for (id in 1..50) {
                fileList.add("animelist_${id}.xml")
            }
        }
        
        return@withContext fileList
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
            println("❌ Error loading episodes for anime ID $animeId: ${e.message}")
        }
        return@withContext episodeList.sortedBy { it.number }
    }
}
