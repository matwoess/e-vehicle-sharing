package com.mathias.android.carcass.model

import com.google.android.gms.maps.model.LatLng

class Carcass {
    var type: AnimalType? = null
    var description: String? = null
    var reportedAt: Long? = null
    var location: LatLngDB = LatLngDB()
    var url: String? = null
    var flagged: Boolean = false

    constructor()
    constructor(
        type: AnimalType?,
        description: String?,
        reportedAt: Long?,
        location: LatLng
    ) {
        this.type = type
        this.description = description
        this.reportedAt = reportedAt
        this.location = LatLngDB(location.latitude, location.longitude)
    }

    constructor(
        type: AnimalType?,
        description: String?,
        reportedAt: Long?,
        location: LatLng,
        image: String?
    ) : this(type, description, reportedAt, location) {
        this.url = image
    }


    fun latLng(): LatLng {
        return LatLng(location.lat, location.lng)
    }

    fun updateValues(c: Carcass) {
        this.type = c.type
        this.location = c.location
        this.description = c.description
        this.reportedAt = c.reportedAt
        this.flagged = c.flagged
    }

    override fun toString(): String {
        return StringBuilder()
            .append("[")
            .append(type)
            .append(", ")
            .append(description)
            .append(", ")
            .append(reportedAt)
            .append(", ")
            .append(location)
            .append("]")
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Carcass

        if (type != other.type) return false
        if (description != other.description) return false
        if (reportedAt != other.reportedAt) return false
        if (location != other.location) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type?.hashCode() ?: 0
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (reportedAt?.hashCode() ?: 0)
        result = 31 * result + location.hashCode()
        return result
    }
}
