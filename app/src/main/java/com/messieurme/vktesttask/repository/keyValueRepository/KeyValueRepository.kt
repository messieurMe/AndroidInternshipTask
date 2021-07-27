package com.messieurme.vktesttask.repository.keyValueRepository

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject

class KeyValueRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    fun <T> put(key: String, value: T): Boolean {
        var isSuccess = true
        sharedPreferences.edit {
            when (value) {
                is Int -> this.putInt(key, value)
                is String -> this.putString(key, value)
                is Boolean -> this.putBoolean(key, value)
                else -> isSuccess = false
            }
        }
        return isSuccess
    }

    fun getInt(key: String, defValue: Int) = sharedPreferences.getInt(key, defValue)
    fun getString(key: String, defValue: String) = sharedPreferences.getString(key, defValue)
    fun getBoolean(key: String, defValue: Boolean) = sharedPreferences.getBoolean(key, defValue)

}