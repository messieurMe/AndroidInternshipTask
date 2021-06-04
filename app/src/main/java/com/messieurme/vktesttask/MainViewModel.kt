package com.messieurme.vktesttask

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.messieurme.vktesttask.classes.UploadingProgress
import java.util.*
import kotlin.collections.ArrayList

class MainViewModel : ViewModel() {

    val accessToken = MutableLiveData<String>()

    val queue = MutableLiveData<ArrayList<UploadingProgress>>(ArrayList())

    var progress = MutableLiveData<Int>(0)

    var notifyItemRangeChanged = MutableLiveData<Int?>()

    var newFileName = "VideoName"


    var cancelUploadForFirst = false

    var enqueueUpload = MutableLiveData<String?>()

    var userPause = MutableLiveData<Boolean?>()

    var recyclerViewItemRemoved = MutableLiveData<Int?>()
    var recyclerViewItemChanged = MutableLiveData<Int?>()

    var isChecked = false
}