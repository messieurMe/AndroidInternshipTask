package com.messieurme.vktesttask.classes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class CoroutineScopes @Inject constructor() {
    fun io() = CoroutineScope(Dispatchers.IO)
    fun main() = CoroutineScope(Dispatchers.Main)
    fun default() = CoroutineScope(Dispatchers.Default)
}