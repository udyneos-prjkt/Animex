package com.udyneos.animex.network

import android.content.Context
import android.util.Log
import com.udyneos.animex.model.Anime
import com.udyneos.animex.model.Episode
import com.udyneos.animex.utils.AssetManager
import com.udyneos.animex.utils.SimpleCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object NetworkService {

    private const val GITHUB_RAW_URL = "https://raw.githubusercontent.com/udyneos-prjkt/Animex-data/main/"
    private const val TAG = "NetworkService"
    
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context
        SimpleCache.init(context)
    }

    // Load anime list dari assets (anime.json)
    suspend fun loadAnimeList(forceRefresh: Boolean = false): List<Anime> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext emptyList()
        
        // Cek cache dulu
        if (!forceRefresh && SimpleCache.hasCache()) {
            val cached = SimpleCache.getAnimeList()
            if (cached.isNotEmpty()) {
                Log.d(TAG, "📦 Using cache: ${cached.size} anime")
                return@withContext cached
            }
        }
        
        Log.d(TAG, "📖 Loading anime from assets...")
        
        try {
            // Load dari assets
            val animeList = AssetManager.loadAnimeFromAssets(ctx)
            Log.d(TAG, "✅ Loaded ${animeList.size} anime from assets")
            
            // Simpan ke cache
            SimpleCache.saveAnimeList(animeList)
            
            return@withContext animeList
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading from assets: ${e.message}")
            return@withContext emptyList()
        }
    }

    // Download episodes dari GitHub berdasarkan judul anime
    suspend fun downloadEpisodes(animeTitle: String, forceRefresh: Boolean = false): List<Episode> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext emptyList()
        
        // Cek cache episode
        if (!forceRefresh) {
            val cached = SimpleCache.getEpisodesByTitle(animeTitle)
            if (cached.isNotEmpty()) {
                Log.d(TAG, "📦 Using cached episodes for $animeTitle")
                return@withContext cached
            }
        }
        
        // Konversi judul ke format nama file GitHub
        val fileName = titleToFileName(animeTitle)
        val urlString = "${GITHUB_RAW_URL}${fileName}"
        
        Log.d(TAG, "🌐 Downloading episodes from: $urlString")
        
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "AnimeX-App")
            
            return@withContext if (connection.responseCode == 200) {
                val xmlString = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                
                val episodes = parseEpisodesFromXml(xmlString, animeTitle)
                Log.d(TAG, "✅ Downloaded ${episodes.size} episodes for $animeTitle")
                
                // Simpan ke cache
                if (episodes.isNotEmpty()) {
                    SimpleCache.saveEpisodesByTitle(animeTitle, episodes)
                }
                
                episodes
            } else {
                Log.w(TAG, "⚠️ HTTP ${connection.responseCode} for $fileName")
                connection.disconnect()
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error downloading episodes: ${e.message}")
            emptyList()
        }
    }
    
    // Konversi judul ke format nama file GitHub
    private fun titleToFileName(title: String): String {
        // Format: anime_[Title].xml
        // Contoh: "One Piece" -> "anime_One_Piece.xml"
        //         "[Oshi no Ko]" -> "anime_Oshi_no_Ko.xml"
        
        // Hapus karakter khusus dan ganti spasi dengan underscore
        val safeTitle = title
            .replace("[", "")
            .replace("]", "")
            .replace(":", "")
            .replace("'", "")
            .replace(".", "")
            .trim()
            .replace(" ", "_")
        
        return "anime_${safeTitle}.xml"
    }
    
    // Parse XML episode
    private fun parseEpisodesFromXml(xmlString: String, animeTitle: String): List<Episode> {
        val episodeList = mutableListOf<Episode>()
        
        try {
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
                                try { epsId = parser.nextText().toInt() } catch (e: Exception) { epsId = 0 }
                            }
                            "video_url" -> if (inEpisode) {
                                videoUrl = parser.nextText() ?: ""
                                videoUrl = videoUrl.trim()
                            }
                            "epstitle" -> if (inEpisode) {
                                episodeTitle = parser.nextText() ?: ""
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "episode" && inEpisode && epsId > 0 && videoUrl.isNotEmpty()) {
                            val finalTitle = if (episodeTitle.isNotEmpty()) episodeTitle else "Episode $epsId"
                            episodeList.add(
                                Episode(
                                    id = epsId,
                                    animeId = 0, // ID tidak terlalu penting
                                    number = epsId,
                                    title = finalTitle,
                                    videoUrl = videoUrl
                                )
                            )
                            inEpisode = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing episodes XML: ${e.message}")
        }
        
        return episodeList.sortedBy { it.number }
    }
}
