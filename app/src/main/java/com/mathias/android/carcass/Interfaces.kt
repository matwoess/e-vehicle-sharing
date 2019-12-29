package com.mathias.android.carcass

internal interface IBottomSheetAnimalTypeListener {
    fun onAnimalTypeSaved(name: String)
    fun onDismiss(added: Boolean)
}