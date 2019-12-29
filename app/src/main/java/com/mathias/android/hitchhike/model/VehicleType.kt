package com.mathias.android.hitchhike.model


class VehicleType {
    var name: String = ""

    constructor() {}
    constructor(name: String) {
        this.name = name
    }

    fun updateValues(vt: VehicleType) {
        this.name = vt.name
    }

    override fun toString(): String {
        return name
    }
}