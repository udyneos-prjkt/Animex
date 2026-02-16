package com.udyneos.animex.model

import com.google.gson.annotations.SerializedName

data class AnimeData(
    val id: Int,
    val title: String,
    val description: String,
    val genre: String,
    @SerializedName("episode_count") val episodeCount: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String,
    val status: String,
    val rating: Double,
    @SerializedName("release_year") val releaseYear: Int,
    val studio: String
) {
    fun toAnime(): Anime {
        return Anime(
            id = id,
            title = title,
            description = description,
            genre = genre,
            episodeCount = episodeCount,
            thumbnailUrl = thumbnailUrl,
            status = status,
            rating = rating,
            releaseYear = releaseYear,
            studio = studio,
            videoUrl = ""
        )
    }
}

data class AnimeListResponse(
    @SerializedName("anime_list") val animeList: List<AnimeData>
)
