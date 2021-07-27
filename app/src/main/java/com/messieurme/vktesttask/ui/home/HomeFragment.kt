package com.messieurme.vktesttask.ui.home

import android.os.Bundle
import android.view.View
import com.vk.api.sdk.VK
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.Toast
import com.vk.api.sdk.auth.VKScope
import kotlinx.coroutines.Dispatchers
import androidx.fragment.app.viewModels
import java.lang.NullPointerException
import kotlinx.coroutines.CoroutineScope
import androidx.lifecycle.ViewModelProvider
import com.messieurme.vktesttask.ui.main.MainActivity
import com.messieurme.vktesttask.retrofit.Get
import com.messieurme.vktesttask.classes.ResponseOrError
import com.messieurme.vktesttask.databinding.FragmentHomeBinding
import com.messieurme.vktesttask.classes.UploadedListAdapter
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.net.UnknownHostException
import javax.inject.Inject


class HomeFragment : DaggerFragment() {

    private lateinit var binding: FragmentHomeBinding

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val homeViewModel: HomeViewModel by viewModels { viewModelFactory }
//    private val mainViewModel: MainViewModel by viewModels { viewModelFactory }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.viewModel = homeViewModel
        binding.executePendingBindings()

        val adapter = UploadedListAdapter()
        binding.uploadedList.adapter = adapter

        homeViewModel.uploaded.onEach { uploadedVideos ->
            @Suppress("UNCHECKED_CAST")
            when (uploadedVideos) {
                is ResponseOrError.Loading -> {}
                is ResponseOrError.Nothing -> homeViewModel.refreshData()
                is ResponseOrError.IsError -> handleError(uploadedVideos.error)
                is ResponseOrError.IsSuccsess<*> -> createRecyclerView(
                    uploadedVideos.response as List<Get.Response.Items>,
                    adapter
                )
            }
        }.launchIn(CoroutineScope(Dispatchers.Main))

        return binding.root
    }


    override fun onResume() {
        super.onResume()

    }

    private fun handleError(error: Exception) {
        when (error) {
            is NullPointerException -> callAuthentication()
            is UnknownHostException -> makeToast()
        }
    }

    private fun makeToast() {
        Toast.makeText(requireContext(), "Connection problems", Toast.LENGTH_SHORT).show()
    }

    private fun callAuthentication() {
        VK.login(requireParentFragment().activity as MainActivity, arrayListOf(VKScope.VIDEO))
    }

    private fun createRecyclerView(videos: List<Get.Response.Items>, adapter: UploadedListAdapter) {
        adapter.submitList(videos)
    }
}