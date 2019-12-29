package com.mathias.android.carcass.model


class AnimalType {
    var name: String = ""

    constructor() {}
    constructor(name: String) {
        this.name = name
    }

    fun updateValues(at: AnimalType) {
        this.name = at.name
    }

    override fun toString(): String {
        return name
    }
}