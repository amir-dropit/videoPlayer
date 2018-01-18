
package com.pantos27.videoplayer.recycler

import android.support.annotation.IdRes
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.pantos27.videoplayer.data.MediaFileInfo
import java.util.*

class DefaultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  private val viewMap: MutableMap<Int, View> = HashMap()
  var mediaFile: MediaFileInfo? = null
  init {
    findViewItems(itemView)
  }

  fun setText(@IdRes id: Int, text: String) {
    val view = (viewMap[id] ?: throw IllegalArgumentException("View for $id not found")) as? TextView ?: throw IllegalArgumentException("View for $id is not a TextView")
    view.text = text
  }

  fun getImage(@IdRes id: Int): ImageView {
    return (viewMap[id] ?: throw IllegalArgumentException("View for $id not found")) as? ImageView ?: throw IllegalArgumentException("View for $id is not a ImageView")
  }

  private fun findViewItems(itemView: View) {
    addToMap(itemView)
    if (itemView is ViewGroup) {
      val childCount = itemView.childCount
      (0 until childCount)
          .map { itemView.getChildAt(it) }
          .forEach { findViewItems(it) }
    }
  }

  private fun addToMap(itemView: View) {
    if (itemView.id == View.NO_ID) {
      itemView.id = View.generateViewId()
    }
    viewMap.put(itemView.id, itemView)
  }
}
