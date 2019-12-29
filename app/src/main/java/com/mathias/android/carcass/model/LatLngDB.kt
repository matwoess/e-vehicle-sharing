package com.mathias.android.carcass.model

class LatLngDB {
    var lat: Double = .0
    var lng: Double = .0

    constructor() {}
    constructor(lat: Double, lng: Double) {
        this.lat = lat
        this.lng = lng
    }
}