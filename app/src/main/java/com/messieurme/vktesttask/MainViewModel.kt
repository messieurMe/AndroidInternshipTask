package com.messieurme.vktesttask

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.messieurme.vktesttask.classes.UploadingProgress
import java.util.*
import kotlin.collections.ArrayList

class MainViewModel : ViewModel() {

    val accessToken = MutableLiveData<String>()

    val queue = MutableLiveData<ArrayList<UploadingProgress>>(ArrayList())

    var pause = MutableLiveData<Boolean?>()

}