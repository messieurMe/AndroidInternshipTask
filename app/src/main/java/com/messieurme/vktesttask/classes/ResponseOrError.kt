package com.messieurme.vktesttask.classes

sealed class ResponseOrError {
    object Loading : ResponseOrError()

    object Nothing : ResponseOrError()

    class IsError(val error: Exception) : ResponseOrError()

    class IsSuccsess<out T>(val response: T) : ResponseOrError()
}