package com.github.estivensh4.androidcodeeditor

interface ResultReceivedListener {
    fun onReceived(FLAG_VALUE: Int, vararg results: String?)
}