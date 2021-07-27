package com.messieurme.vktesttask.exceptions

class NoInternetException : Exception() {
    override val message: String
        get() = "No internet connection"
}