package com.pantos27.videoplayer.recycler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import com.bumptech.glide.Glide
import com.pantos27.videoplayer.PlayerActivity
import com.pantos27.videoplayer.PlayerActivity.Companion.startPlayerActivity
import com.pantos27.videoplayer.R
import com.pantos27.videoplayer.data.MediaFileInfo
import com.pantos27.videoplayer.viewmodels.VideosViewModel
import java.text.DateFormat
import java.util.*

/**
 * Created by pantos27 on 15/12/17.
 */

class VideoAdapter(context: Context, cursor: Cursor, flags: Int, viewModel: VideosViewModel) : CursorAdapter(context, cursor, flags), View.OnClickListener, View.OnLongClickListener {
    val TAG = "VideoAdapter"

    private val viewModel = viewModel
    private val inflater = LayoutInflater.from(context)
    private val colName = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
    private val colDuration = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
    private val colAdded = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
    private val colData = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

    interface VideoAdapterListener{
        fun onClick(video: MediaFileInfo)
        fun onLongClick(video: MediaFileInfo)
    }
    override fun newView(context: Context, cursor: Cursor?, vg: ViewGroup): View {
        val view =  inflater.inflate(R.layout.row_video,vg,false)
        view.tag = DefaultViewHolder(view)
        view.setOnClickListener(this)
        view.setOnLongClickListener(this)
        return view

    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val holder = view.tag as DefaultViewHolder
        val mediaFile = MediaFileInfo(cursor.getString(colName),cursor.getString(colData),
                cursor.getLong(colDuration),(cursor.getLong(colAdded)*1000))
        holder.setText(R.id.row_name,mediaFile.name)
        holder.setText(R.id.row_text_duration,mediaFile.duration.toTimeString())
        holder.setText(R.id.row_added,mediaFile.added.toDateString())

        Glide.with(context)
                .load(mediaFile.path)
                .fallback(R.drawable.ic_movie_black_24dp)
                .placeholder(R.drawable.ic_movie_black_24dp)
                .error(R.drawable.ic_movie_black_24dp)
                .into(holder.getImage(R.id.row_thumb))

        holder.mediaFile = mediaFile
        //set background is selected
        view.isActivated = viewModel.selectedItems.any { it.path==mediaFile.path }

    }



    override fun onLongClick(view: View): Boolean {
        Log.d(TAG,"onLongClick row")
        val holder = view.tag as DefaultViewHolder
        holder.mediaFile?.let { mediaFile ->
            val exists = viewModel.selectedItems.find { it.path==mediaFile.path  }
            //check if already selected
            if (exists!=null){
                viewModel.selectedItems.remove(exists)
                view.isActivated = false
                if (viewModel.selectedItems.isEmpty()) viewModel.multiLiveData.postValue(false)
                return true
            }else{

                viewModel.selectedItems.add(mediaFile)
                view.isActivated = true
                viewModel.multiLiveData.postValue(true)
            }
        }

        return true
    }

    override fun onClick(view: View) {
        Log.d(TAG,"onclick row")
        //clear selected if any
        viewModel.multiLiveData.postValue(false)
        viewModel.selectedItems.clear()

        val holder = view.tag as DefaultViewHolder

        holder.mediaFile?.let { startPlayerActivity(context = view.context, items = listOf(it)) }

    }


    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG,"broadcast received")
            notifyDataSetChanged()
        }
    }

    fun dispose(){
        Log.d(TAG,"dispose")
        try {
            inflater.context.unregisterReceiver(broadcastReceiver)
        }catch (e: IllegalArgumentException){

        }
    }
}


 fun Long.toTimeString(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val h = if(hours>0) "$hours:" else ""
    return h + String.format("%02d:%02d", minutes % 60, seconds % 60)
}
val DATE_FORMAT = DateFormat.getInstance()
fun Long.toDateString() = DATE_FORMAT.format(Date(this))

