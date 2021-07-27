package com.messieurme.vktesttask.classes

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessTokenClass @Inject constructor() {
    private val _accessToken = MutableLiveData("")
    private val _accessTokenF = MutableStateFlow("")

    private var wasUpdated = false

    fun submitAccessToken(accessToken: String) {
        CoroutineScope(Dispatchers.Default).launch {
            _accessToken.postValue(accessToken)
            _accessTokenF.emit(accessToken)
            if (accessToken.isNotEmpty()) {
                wasUpdated = true
            }
        }
    }

    suspend fun getAccessToken(wait: Boolean =  true): String {
        while (!wasUpdated && wait) {
            delay(100)
        }
        return _accessTokenF.value
    }

}

