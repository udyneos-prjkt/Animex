package com.udyneos.animex.model

import android.os.Parcel
import android.os.Parcelable

data class Episode(
    val id: Int,
    val animeId: Int,
    val number: Int,
    val title: String,
    val duration: String,
    val videoUrl: String,
    val thumbnailUrl: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(animeId)
        parcel.writeInt(number)
        parcel.writeString(title)
        parcel.writeString(duration)
        parcel.writeString(videoUrl)
        parcel.writeString(thumbnailUrl)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Episode> {
        override fun createFromParcel(parcel: Parcel): Episode = Episode(parcel)
        override fun newArray(size: Int): Array<Episode?> = arrayOfNulls(size)
    }
}
