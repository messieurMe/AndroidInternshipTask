package com.messieurme.vktesttask.recyclerViews

import android.net.Uri
import android.view.ViewGroup
import android.view.LayoutInflater
import com.messieurme.vktesttask.R
import com.squareup.picasso.Picasso
import com.messieurme.vktesttask.retrofit.Get
import androidx.recyclerview.widget.RecyclerView
import com.messieurme.vktesttask.databinding.ItemUploadedBinding

class UploadedLisAdapter(private val titles: List<Get.Response.Items>) :
    RecyclerView.Adapter<UploadedLisAdapter.CustomViewHolder>() {

    inner class CustomViewHolder(item: ItemUploadedBinding) :
        RecyclerView.ViewHolder(item.root) {
        private var binding: ItemUploadedBinding = item

        fun bind(info: Get.Response.Items) {
            binding.textView.text = info.title

            Picasso.get()
                .load(Uri.parse(info.image?.random()?.url)) //random available size. It's no matter what it is
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
        holder.bind(titles[position])
    }

    override fun getItemCount() = titles.size
}
