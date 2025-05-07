package com.aquamanagers.aquamanage_app.models

import android.os.Parcel
import android.os.Parcelable

data class DeviceData(
    val ph: Double,
    val tds: Double,
    val turbidity: Double
): Parcelable {
    constructor(parcel: Parcel): this(
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags:Int){
        parcel.writeDouble(ph)
        parcel.writeDouble(tds)
        parcel.writeDouble(turbidity)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR: Parcelable.Creator<DeviceItem> {
        override fun createFromParcel(parcel: Parcel): DeviceItem = DeviceItem(parcel)

        override fun newArray(size: Int): Array<DeviceItem?> = arrayOfNulls(size)
    }
}