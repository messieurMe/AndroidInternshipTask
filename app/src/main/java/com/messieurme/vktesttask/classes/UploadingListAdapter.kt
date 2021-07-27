package com.messieurme.vktesttask.classes

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.messieurme.vktesttask.databinding.ItemQueuedBinding
import com.messieurme.vktesttask.databinding.ItemUploadingBinding
import com.messieurme.vktesttask.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class UploadingLisAdapter(
    private var items: ArrayList<UploadingItem>,
    private val dashboardViewModel: DashboardViewModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        CoroutineScope(Dispatchers.Main).launch {
            dashboardViewModel.queueChangedListener.onEach {
                updateList(it)
            }.launchIn(CoroutineScope(Dispatchers.Main))
        }
    }

    private fun updateList(it: Int) = kotlin.runCatching {
        Log.d("REMOVEITEM", "Updating list")
        val newList = dashboardViewModel.queueCopy()
        val diffResult = DiffUtil.calculateDiff(DiffUtilCallbackUploading(this.items, newList), true)
        diffResult.dispatchUpdatesTo(this)
        items = PriorityArrayList(newList)
        Log.d("REMOVEITEM", "Done")
    }.isSuccess

    inner class CustomViewHolderUploading(item: ItemUploadingBinding) :
        RecyclerView.ViewHolder(item.root) {
        var binding: ItemUploadingBinding = item

        fun bind(info: UploadingItem) {
            binding.name.text = info.name
            binding.progressBar.isIndeterminate = false
            binding.cancel.setOnClickListener {
                dashboardViewModel.removeAt(absoluteAdapterPosition)
            }

            dashboardViewModel.progress.onEach {
                binding.progressBar.setProgress(it, true)
                binding.progressInDigits.text = "$it%"
            }.launchIn(CoroutineScope(Dispatchers.Main))
        }
    }

    inner class CustomViewHolderQueued(item: ItemQueuedBinding) : RecyclerView.ViewHolder(item.root) {
        var binding: ItemQueuedBinding = item

        fun bind(info: UploadingItem) {
            binding.name.text = info.name
            binding.cancel.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    dashboardViewModel.removeAt(absoluteAdapterPosition)
                }
            }
        }
    }

    override fun getItemViewType(position: Int) = if (position == 0) 1 else 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        1 -> CustomViewHolderUploading(
            ItemUploadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        else -> CustomViewHolderQueued(
            ItemQueuedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            1 -> (holder as CustomViewHolderUploading).bind(items[position])
            else -> (holder as CustomViewHolderQueued).bind(items[position])
        }
    }

    override fun getItemCount() = items.size
}