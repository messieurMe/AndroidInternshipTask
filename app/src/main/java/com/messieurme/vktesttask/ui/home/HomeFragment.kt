package com.messieurme.vktesttask.ui.home

import java.io.*
import okhttp3.*
import androidx.work.*
import android.os.Looper
import android.os.Bundle
import android.view.View
import com.vk.api.sdk.VK
import android.os.Handler
import android.widget.Toast
import android.view.ViewGroup
import retrofit2.awaitResponse
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import com.vk.api.sdk.auth.VKScope
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import androidx.fragment.app.Fragment
import java.lang.NullPointerException
import kotlinx.coroutines.CoroutineScope
import androidx.lifecycle.ViewModelProvider
import com.messieurme.vktesttask.MainActivity
import com.messieurme.vktesttask.retrofit.Get
import com.messieurme.vktesttask.MainViewModel
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.messieurme.vktesttask.R
import com.messieurme.vktesttask.databinding.FragmentHomeBinding
import com.messieurme.vktesttask.recyclerViews.UploadedLisAdapter
import com.messieurme.vktesttask.classes.SharedFunctions.Companion.retrofit
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var mainViewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        mainViewModel.accessToken.onEach {
            if (it.isEmpty()) return@onEach
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val uploadedId = getAlbumId(it)  //Id for uploaded is -1, but i'll download it
                    val uploadedVideos = getVideosByAlbumId(it, uploadedId)
                    activity?.runOnUiThread {
                        binding.uploadedList.apply {
                            adapter = UploadedLisAdapter(uploadedVideos!!.response.items)
                            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                        }
                    }
                } catch (e: UnknownHostException) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            requireContext(), getString(R.string.connection_problem), Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: NullPointerException) {
                    //If token expired
                    VK.login(requireParentFragment().activity as MainActivity, arrayListOf(VKScope.VIDEO))
                }
            }
        }.launchIn(CoroutineScope(Dispatchers.Main))
        return binding.root
    }

    private suspend fun getVideosByAlbumId(accessToken: String, uploadedId: Int?): Get? {
        val response = retrofit.get(uploadedId!!, accessToken).awaitResponse()
        return response.body()
    }

    private suspend fun getAlbumId(accessToken: String): Int? {
        val response = retrofit.getAlbums(accessToken, 1).awaitResponse()
        return response.body()?.response?.items?.find { it.title == "Загруженные" }?.id
    }
}