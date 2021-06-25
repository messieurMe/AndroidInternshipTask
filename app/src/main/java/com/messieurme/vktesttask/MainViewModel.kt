package com.messieurme.vktesttask

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.messieurme.vktesttask.classes.PriorityArrayList
import com.messieurme.vktesttask.classes.UploadingProgress
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.util.*
import kotlin.collections.ArrayList

class MainViewModel : ViewModel() {

    val accessToken = MutableStateFlow<String>("")

    val queue = MutableStateFlow<PriorityArrayList<UploadingProgress>>(PriorityArrayList())

    var progress = MutableStateFlow<Int>(0)

    var newFileName = "VideoName"
    var description = ""

    var cancelUploadForFirst = false


    var enqueueUpload =Channel<String?>(5)

    var isChecked = MutableStateFlow<Boolean?>(null)
    var userPause = MutableStateFlow<Boolean?>(null)

    var kostylForUI = MutableStateFlow<Boolean?>(null)

    //SharedFlow?
    var notifyQueueChanged = Channel<Int>(5)
}