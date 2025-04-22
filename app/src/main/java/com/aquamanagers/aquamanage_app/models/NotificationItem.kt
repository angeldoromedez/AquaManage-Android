package com.aquamanagers.aquamanage_app.models

import android.os.Parcel
import android.os.Parcelable


data class NotificationItem(
    val id: String,
    val notificationImage: Int,
    val notificationName: String,
    var deviceName: String,
    var colorHex: String,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "Device",
        parcel.readInt(),
        parcel.readString() ?: "Treatment Complete",
        parcel.readString() ?: "Device 1",
        parcel.readString() ?: "#FFFFFFF"
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeInt(notificationImage)
        parcel.writeString(notificationName)
        parcel.writeString(deviceName)
        parcel.writeString(colorHex)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<NotificationItem> {
        override fun createFromParcel(parcel: Parcel): NotificationItem = NotificationItem(parcel)
        override fun newArray(size: Int): Array<NotificationItem?> = arrayOfNulls(size)
    }
}