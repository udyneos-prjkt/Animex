package com.udyneos.animex.model

import android.os.Parcel
import android.os.Parcelable

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
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readInt(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(genre)
        parcel.writeString(episodeCount)
        parcel.writeString(thumbnailUrl)
        parcel.writeString(videoUrl)
        parcel.writeString(status)
        parcel.writeDouble(rating)
        parcel.writeInt(releaseYear)
        parcel.writeString(studio)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Anime> {
        override fun createFromParcel(parcel: Parcel): Anime = Anime(parcel)
        override fun newArray(size: Int): Array<Anime?> = arrayOfNulls(size)
    }
}
