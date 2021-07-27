package com.messieurme.vktesttask.classes

import androidx.recyclerview.widget.DiffUtil


class DiffUtilCallbackUploading(private val oldList: List<UploadingItem>,
                                private val newList: List<UploadingItem>
) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].sessionID == newList[newItemPosition].sessionID
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldProduct = oldList[oldItemPosition]
        val newProduct = newList[newItemPosition]
        return (oldProduct.name == newProduct.name && oldProduct.progress == newProduct.progress)
    }
}