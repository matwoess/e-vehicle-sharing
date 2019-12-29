package com.mathias.android.hitchhike

interface IRentVehicle {
    fun onVehicleRented(key: String)
    fun onVehicleReleased(key: String)
}