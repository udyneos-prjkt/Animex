package com.udyneos.animex.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Anime(
    val id: Int,
    val title: String,
    val description: String,
    val genre: String,
    val episodeCount: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val status: String,
    val rating: Double,
    val releaseYear: Int,
    val studio: String
) : Parcelable
