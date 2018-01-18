package com.pantos27.videoplayer.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import com.pantos27.videoplayer.data.MediaFileInfo



/**
 * Created by pantos27 on 15/12/17.
 */
const val TAG = "VideoViewModel"
class VideosViewModel: ViewModel(){
    init {
    }
    var videocursor: Cursor? = null
    val multiLiveData = MutableLiveData<Boolean>()
    val selectedItems = mutableListOf<MediaFileInfo>()
    var playlist = false

    init {
        multiLiveData.postValue(false)
    }

    fun getCursor(context: Context,items: List<String>? = null) : Cursor?{
        try {
            var selection: String? = null
            var selectionArgs: Array<String>? = null
            if (items!=null && items.isNotEmpty()){
                selection = "_data REGEXP ?"
                selectionArgs = arrayOf(items.joinToString("|"))
            }
            val columns = arrayOf(MediaStore.Video.Thumbnails.DATA, MediaStore.Video.Media.DURATION,MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,MediaStore.Video.Media.DATE_ADDED)
            videocursor = context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    columns, selection, selectionArgs, null)
//            videocursor?.columnNames?.forEach { Log.d(TAG,"getCursor: $it") }
            return videocursor
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onCleared() {
        super.onCleared()
        videocursor?.close()
    }
}