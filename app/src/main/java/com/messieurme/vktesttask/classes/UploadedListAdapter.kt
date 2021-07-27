package com.messieurme.vktesttask.classes

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.messieurme.vktesttask.R
import com.messieurme.vktesttask.databinding.ItemUploadedBinding
import com.messieurme.vktesttask.retrofit.Get
import com.squareup.picasso.Picasso


class UploadedListAdapter :
    ListAdapter<Get.Response.Items, UploadedListAdapter.CustomViewHolder>(DiffUtilCallbackUploaded()) {

    inner class CustomViewHolder(item: ItemUploadedBinding) :
        RecyclerView.ViewHolder(item.root) {
        private var binding: ItemUploadedBinding = item

        fun bind(info: Get.Response.Items) {
            binding.textView.text = info.title

            Picasso.get()
                .load(Uri.parse(info.image?.random()?.url))
                .fit().centerCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(binding.imageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CustomViewHolder(
        ItemUploadedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}


class DiffUtilCallbackUploaded : DiffUtil.ItemCallback<Get.Response.Items>() {
    override fun areContentsTheSame(oldItem: Get.Response.Items, newItem: Get.Response.Items) =
        oldItem.id == newItem.id

    override fun areItemsTheSame(oldItem: Get.Response.Items, newItem: Get.Response.Items) =
        oldItem.title == newItem.title

}
