package com.messieurme.vktesttask.ui.notifications

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.work.*
import com.messieurme.vktesttask.R
import com.messieurme.vktesttask.classes.UploadingProgress
import com.messieurme.vktesttask.databinding.FragmentHomeBinding
import com.messieurme.vktesttask.databinding.FragmentNotificationsBinding
import com.messieurme.vktesttask.recyclerViews.UploadedLisAdapter
import com.messieurme.vktesttask.retrofit.Get
import com.messieurme.vktesttask.room.UploadsDao
import com.messieurme.vktesttask.room.UploadsDatabase
import com.messieurme.vktesttask.service.UploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL

class NotificationsFragment : Fragment() {

    //TODO: DONT FORGET TO REMOVE THIS

    private lateinit var notificationsViewModel: NotificationsViewModel
    private lateinit var binding: FragmentNotificationsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        var list = Array<Get.Response.Items>(20) {
            Get.Response.Items(
                it,
                10,
                100,
                100,
                "ItisIt$it",
                200,
                "What",
                2000,
                listOf(Get.Response.Items.Image("https://picsum.photos/200/300", 200,300,0)),
                true
            )
        }.asList()
        binding.recyclerView.apply {
            adapter = UploadedLisAdapter(list)
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }


        return binding.root
    }
}