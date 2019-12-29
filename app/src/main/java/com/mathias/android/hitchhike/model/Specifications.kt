package com.mathias.android.hitchhike.model

class Specifications {
    var length: Float = .0f
    var width: Float = .0f
    var height: Float = .0f
    var weight: Float = .0f

    constructor() {}
    constructor(l: Float, w: Float, h: Float, weight: Float) {
        length = l
        width = w
        height = h
        this.weight = weight
    }

    override fun toString(): String {
        return String.format("length: %d, width: %d, height: %d, weight: %d")
    }
}
