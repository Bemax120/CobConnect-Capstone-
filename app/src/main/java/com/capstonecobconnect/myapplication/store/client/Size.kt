package com.capstonecobconnect.myapplication.store.client

import android.os.Parcel
import android.os.Parcelable

data class Size(
    val size: String,
    val stock: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(size)
        parcel.writeInt(stock)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Size> {
        override fun createFromParcel(parcel: Parcel): Size = Size(parcel)
        override fun newArray(size: Int): Array<Size?> = arrayOfNulls(size)
    }
}
