package com.aquamanagers.aquamanage_app.models

import android.os.Parcel
import android.os.Parcelable


data class NotificationItem(
    val id: String = "",
    val deviceId: String = "",
    val notificationImage: Int = 0,
    val notificationName: String = "",
    val timeStamp: Long = 0L,
    var deviceName: String = "",
    var colorHex: String = "#FFFFFF"
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "#FFFFFF"
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(deviceId)
        parcel.writeInt(notificationImage)
        parcel.writeString(notificationName)
        parcel.writeLong(timeStamp)
        parcel.writeString(deviceName)
        parcel.writeString(colorHex)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<NotificationItem> {
        override fun createFromParcel(parcel: Parcel): NotificationItem = NotificationItem(parcel)
        override fun newArray(size: Int): Array<NotificationItem?> = arrayOfNulls(size)
    }
}