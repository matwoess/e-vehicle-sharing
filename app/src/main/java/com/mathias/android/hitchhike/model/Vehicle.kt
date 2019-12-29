package com.mathias.android.hitchhike.model

import com.google.android.gms.maps.model.LatLng

class Vehicle {
    var type: VehicleType? = null
    var specifications: Specifications = Specifications()
    var description: String? = null
    var charge: Int = 0
    var location: LatLngDB = LatLngDB()
    var rented: Boolean = false
    var locked: Boolean = true
    var alarm: Boolean = false

    constructor()
    constructor(
        type: VehicleType?,
        specifications: Specifications,
        description: String?,
        charge: Int,
        location: LatLng
    ) {
        this.type = type
        this.specifications = specifications
        this.description = description
        this.charge = charge
        this.location = LatLngDB(location.latitude, location.longitude)
    }

    fun latLng(): LatLng {
        return LatLng(location.lat, location.lng)
    }

    fun updateValues(c: Vehicle) {
        this.type = c.type
        this.location = c.location
        this.description = c.description
        this.charge = c.charge
    }

    override fun toString(): String {
        return StringBuilder()
            .append("[")
            .append(type)
            .append(", ")
            .append(description)
            .append(", ")
            .append(charge)
            .append(", ")
            .append(location)
            .append("]")
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vehicle

        if (type != other.type) return false
        if (description != other.description) return false
        if (charge != other.charge) return false
        if (location != other.location) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type?.hashCode() ?: 0
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (charge.hashCode() ?: 0)
        result = 31 * result + location.hashCode()
        return result
    }
}
