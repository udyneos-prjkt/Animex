package com.udyneos.animex.utils

import android.content.Context
import com.udyneos.animex.model.Anime
import com.udyneos.animex.model.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.CharacterIterator
import java.text.StringCharacterIterator

object CacheManager {
    
    private const val CACHE_VERSION = 1
    private const val CACHE_MAX_AGE = 24 * 60 * 60 * 1000 // 24 jam dalam milliseconds
    
    data class CacheData<T>(
        val version: Int,
        val timestamp: Long,
        val data: T
    )
    
    // Dapatkan informasi cache
    suspend fun getCacheInfo(context: Context): Pair<String, Long> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            var totalSize = 0L
            var latestTimestamp = 0L
            
            cacheDir.listFiles()?.forEach { file ->
                totalSize += file.length()
                if (file.lastModified() > latestTimestamp) {
                    latestTimestamp = file.lastModified()
                }
            }
            
            val sizeStr = if (totalSize > 0) {
                humanReadableByteCount(totalSize)
            } else {
                "No cache"
            }
            
            return@withContext Pair(sizeStr, latestTimestamp)
        } catch (e: Exception) {
            return@withContext Pair("Error", 0L)
        }
    }
    
    // Konversi byte ke format human readable
    private fun humanReadableByteCount(bytes: Long): String {
        if (-1000 < bytes && bytes < 1000) {
            return "$bytes B"
        }
        val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
        var bytes = bytes
        var i = 40
        while (i >= 0 && bytes > -1000) {
            bytes = bytes shr 10
            ci.next()
            i -= 10
        }
        bytes *= 1000
        bytes = bytes shr 10
        return String.format("%.1f %cB", bytes / 1000.0, ci.current())
    }
    
    // Cache untuk daftar anime
    suspend fun saveAnimeList(context: Context, animeList: List<Anime>) {
        withContext(Dispatchers.IO) {
            try {
                val cacheFile = File(context.cacheDir, "anime_list_cache.dat")
                val cacheData = CacheData(
                    version = CACHE_VERSION,
                    timestamp = System.currentTimeMillis(),
                    data = animeList
                )
                
                ObjectOutputStream(cacheFile.outputStream()).use { it.writeObject(cacheData) }
                println("✅ Anime list saved to cache: ${animeList.size} items")
            } catch (e: Exception) {
                println("❌ Failed to save anime list to cache: ${e.message}")
            }
        }
    }
    
    // Load cache daftar anime
    suspend fun loadAnimeList(context: Context): List<Anime>? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(context.cacheDir, "anime_list_cache.dat")
            if (!cacheFile.exists()) {
                println("📁 Cache file not found")
                return@withContext null
            }
            
            // Cek umur cache
            val lastModified = cacheFile.lastModified()
            val age = System.currentTimeMillis() - lastModified
            if (age > CACHE_MAX_AGE) {
                println("⏰ Cache expired (age: ${age / 1000 / 60} minutes)")
                cacheFile.delete()
                return@withContext null
            }
            
            ObjectInputStream(cacheFile.inputStream()).use { stream ->
                val cacheData = stream.readObject() as CacheData<*>
                
                // Cek versi cache
                if (cacheData.version != CACHE_VERSION) {
                    println("🔄 Cache version mismatch")
                    cacheFile.delete()
                    return@withContext null
                }
                
                @Suppress("UNCHECKED_CAST")
                val animeList = cacheData.data as List<Anime>
                println("✅ Loaded anime list from cache: ${animeList.size} items")
                return@withContext animeList
            }
        } catch (e: Exception) {
            println("❌ Failed to load anime list from cache: ${e.message}")
            return@withContext null
        }
    }
    
    // Cache untuk episode per anime
    suspend fun saveEpisodes(context: Context, animeId: Int, episodes: List<Episode>) {
        withContext(Dispatchers.IO) {
            try {
                val cacheFile = File(context.cacheDir, "episodes_${animeId}_cache.dat")
                val cacheData = CacheData(
                    version = CACHE_VERSION,
                    timestamp = System.currentTimeMillis(),
                    data = episodes
                )
                
                ObjectOutputStream(cacheFile.outputStream()).use { it.writeObject(cacheData) }
                println("✅ Episodes for anime $animeId saved to cache: ${episodes.size} items")
            } catch (e: Exception) {
                println("❌ Failed to save episodes to cache: ${e.message}")
            }
        }
    }
    
    // Load cache episode per anime
    suspend fun loadEpisodes(context: Context, animeId: Int): List<Episode>? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(context.cacheDir, "episodes_${animeId}_cache.dat")
            if (!cacheFile.exists()) {
                return@withContext null
            }
            
            // Cek umur cache
            val lastModified = cacheFile.lastModified()
            val age = System.currentTimeMillis() - lastModified
            if (age > CACHE_MAX_AGE) {
                cacheFile.delete()
                return@withContext null
            }
            
            ObjectInputStream(cacheFile.inputStream()).use { stream ->
                val cacheData = stream.readObject() as CacheData<*>
                
                if (cacheData.version != CACHE_VERSION) {
                    cacheFile.delete()
                    return@withContext null
                }
                
                @Suppress("UNCHECKED_CAST")
                val episodes = cacheData.data as List<Episode>
                println("✅ Loaded episodes for anime $animeId from cache: ${episodes.size} items")
                return@withContext episodes
            }
        } catch (e: Exception) {
            println("❌ Failed to load episodes from cache: ${e.message}")
            return@withContext null
        }
    }
    
    // Hapus semua cache
    suspend fun clearAllCache(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                context.cacheDir.listFiles()?.forEach { it.delete() }
                println("✅ All cache cleared")
            } catch (e: Exception) {
                println("❌ Failed to clear cache: ${e.message}")
            }
        }
    }
    
    // Hapus cache expired
    suspend fun clearExpiredCache(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                context.cacheDir.listFiles()?.forEach { file ->
                    val age = now - file.lastModified()
                    if (age > CACHE_MAX_AGE) {
                        file.delete()
                        println("🗑️ Deleted expired cache: ${file.name}")
                    }
                }
            } catch (e: Exception) {
                println("❌ Failed to clear expired cache: ${e.message}")
            }
        }
    }
}
