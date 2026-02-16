package com.udyneos.animex.utils

import android.content.Context
import com.google.gson.Gson
import com.udyneos.animex.model.Anime
import com.udyneos.animex.model.AnimeListResponse

object AssetManager {
    
    private const val ANIME_JSON_FILE = "anime.json"
    private val gson = Gson()
    
    // Baca anime.json dari assets
    fun loadAnimeFromAssets(context: Context): List<Anime> {
        return try {
            val jsonString = context.assets.open(ANIME_JSON_FILE).bufferedReader().use { it.readText() }
            val response = gson.fromJson(jsonString, AnimeListResponse::class.java)
            response.animeList.map { it.toAnime() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
