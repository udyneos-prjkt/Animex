package com.udyneos.animex.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.udyneos.animex.model.Anime
import com.udyneos.animex.model.Episode

object SimpleCache {
    private const val PREFS_NAME = "animex_cache"
    private const val KEY_ANIME_LIST = "anime_list"
    private const val KEY_LAST_UPDATE = "last_update"
    private const val KEY_ANIME_COUNT = "anime_count"
    
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Simpan daftar anime
    fun saveAnimeList(animeList: List<Anime>) {
        val json = gson.toJson(animeList)
        prefs.edit().putString(KEY_ANIME_LIST, json).apply()
        prefs.edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()
        prefs.edit().putInt(KEY_ANIME_COUNT, animeList.size).apply()
    }
    
    // Ambil daftar anime
    fun getAnimeList(): List<Anime> {
        val json = prefs.getString(KEY_ANIME_LIST, null) ?: return emptyList()
        val type = object : TypeToken<List<Anime>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Cek apakah cache ada
    fun hasCache(): Boolean {
        return prefs.contains(KEY_ANIME_LIST) && prefs.getInt(KEY_ANIME_COUNT, 0) > 0
    }
    
    // Ambil waktu update terakhir
    fun getLastUpdateTime(): Long {
        return prefs.getLong(KEY_LAST_UPDATE, 0)
    }
    
    // Hapus cache
    fun clearCache() {
        prefs.edit().clear().apply()
    }
    
    // Cache untuk episode berdasarkan title
    fun saveEpisodesByTitle(title: String, episodes: List<Episode>) {
        val key = "episodes_${title.replace(" ", "_").replace("[", "").replace("]", "")}"
        val json = gson.toJson(episodes)
        prefs.edit().putString(key, json).apply()
    }
    
    fun getEpisodesByTitle(title: String): List<Episode> {
        val key = "episodes_${title.replace(" ", "_").replace("[", "").replace("]", "")}"
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<Episode>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun hasEpisodesByTitle(title: String): Boolean {
        val key = "episodes_${title.replace(" ", "_").replace("[", "").replace("]", "")}"
        return prefs.contains(key)
    }
}
