package com.mathias.android.hitchhike.model

class Size {
    var length: Float = .0f
    var width: Float = .0f
    var height: Float = .0f

    constructor() {}
    constructor(l: Float, w: Float, h: Float) {
        length = l
        width = w
        height = h
    }
}
