package com.messieurme.vktesttask.ui.dashboard

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.messieurme.vktesttask.MainViewModel
import com.messieurme.vktesttask.classes.UploadingProgress
import com.messieurme.vktesttask.classes.UrlFromUri
import com.messieurme.vktesttask.databinding.FragmentDashboardBinding
import com.messieurme.vktesttask.databinding.ItemUploadingBinding
import okio.ByteString.Companion.encodeUtf8
import java.util.*


class DashboardFragment : Fragment() {


    private lateinit var binding: FragmentDashboardBinding
    private lateinit var mainViewModel: MainViewModel


    private val chooseFile: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
            if (result != null) {
                val fileUrl = UrlFromUri.getFilePath(requireContext(), result)
                mainViewModel.enqueueUpload.postValue(fileUrl!!)
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        binding.apply {
            switchUploadMode.setOnCheckedChangeListener { _, isChecked ->
                mainViewModel.isChecked = isChecked
            }
            recyclerView.apply {
                adapter = UploadingLisAdapter(mainViewModel.queue.value!!)
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            }

            addToUpload.setOnClickListener {
                askPermission()
                if (permission) {
                    mainViewModel.newFileName = binding.searchQuery.text.toString()
                        .let { name -> if (name.isNotEmpty()) name.encodeUtf8().utf8() else "VideoName" }
                    //I can add textField for description, post on wall and all this, but...
                    //It's few minutes in code, but with this I must change layout,
                    //there will be too many fields, I'll convert it to MotionLayout to make
                    //appearance of them. MotionLayout is cool, but Android Studio is slows down.
                    //And I'll spend  too much time on waiting. Just... let's imagine I did it

                    chooseFile.launch("video/*")
                }
            }

            pause.setOnClickListener {
                val currentValue = mainViewModel.userPause.value ?: false
                mainViewModel.userPause.postValue(!currentValue)
            }
        }

        mainViewModel.userPause.observe(viewLifecycleOwner) {
            binding.isPause = it
        }

        mainViewModel.recyclerViewItemRemoved.observe(viewLifecycleOwner, { toRemove ->
            toRemove?.let { binding.recyclerView.adapter?.notifyItemRemoved(it) }
        })
        mainViewModel.notifyItemRangeChanged.observe(viewLifecycleOwner) { range ->
            range?.let {
                binding.recyclerView.adapter!!.notifyItemRangeChanged(
                    0,
                    it
                )
            }
        }
        mainViewModel.recyclerViewItemChanged.observe(viewLifecycleOwner, { toChange ->
            toChange?.let { binding.recyclerView.adapter?.notifyItemChanged(it) }
        })
        return binding.root
    }


    inner class UploadingLisAdapter(
        private val titles: ArrayList<UploadingProgress>
    ) :
        RecyclerView.Adapter<UploadingLisAdapter.CustomViewHolder>() {

        inner class CustomViewHolder(item: ItemUploadingBinding) :
            RecyclerView.ViewHolder(item.root) {
            var binding: ItemUploadingBinding = item

            fun bind(info: UploadingProgress) {
                binding.name.text = info.name
                binding.cancel.setOnClickListener {
                    val position = adapterPosition
                    if (position > 0) {
                        mainViewModel.queue.value!!.removeAt(position)
                        notifyItemRemoved(position)
                    } else {
                        mainViewModel.cancelUploadForFirst = true
                    }
                }
                if (adapterPosition == 0) {
                    mainViewModel.progress.observe(viewLifecycleOwner) {
                        binding.progressBar.progress = it
                    }
                }

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CustomViewHolder(
            ItemUploadingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            holder.bind(titles[position])
        }

        override fun getItemCount() = titles.size
    }


    private var permission = false
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            permission = isGranted
        }

    private fun askPermission(): Boolean {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                permission = true
            }
            shouldShowRequestPermissionRationale("READ F...N STORAGE") -> {
                println("WHAT")
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        return true
    }

}