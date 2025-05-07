package com.aquamanagers.aquamanage_app.models

import android.os.Parcel
import android.os.Parcelable

data class DeviceItem(val id: String,
                      val phValue: String,
                      val tdsValue: String,
                      val turbidityValue: String,
                      var deviceName: String,
                      var colorHex: String,
):Parcelable {
    constructor(parcel: Parcel): this(
        parcel.readString() ?: "ESP32",
        parcel.readString() ?: "0",
        parcel.readString()?:"0",
        parcel.readString()?:"0",
        parcel.readString()?:"Device 1",
        parcel.readString()?:"#FFFFFFF"
    )

    override fun writeToParcel(parcel:Parcel, flags:Int){
        parcel.writeString(id)
        parcel.writeString(phValue)
        parcel.writeString(tdsValue)
        parcel.writeString(turbidityValue)
        parcel.writeString(deviceName)
        parcel.writeString(colorHex)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR:Parcelable.Creator<DeviceItem> {
        override fun createFromParcel(parcel: Parcel): DeviceItem = DeviceItem(parcel)

        override fun newArray(size: Int): Array<DeviceItem?> = arrayOfNulls(size)
    }
}