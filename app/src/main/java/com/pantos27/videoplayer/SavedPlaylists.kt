package com.pantos27.videoplayer

import android.content.Context
import android.util.Log

/**
 * Created by pantos27 on 16/12/17.
 */
const val PREF_SAVED = "saved"

fun savePlaylist(context: Context, files: Array<String>,name: String){
//    files.forEach { Log.d("Playlists",it) }
    context.getSharedPreferences(PREF_SAVED, Context.MODE_PRIVATE)
            .edit().putString(name,files.joinToString("|")).apply()

}

fun getPlaylist(context: Context,name: String) = context.getSharedPreferences(PREF_SAVED, Context.MODE_PRIVATE).getString(name,"").split("|")

fun getAllPlaylists(context: Context) = context.getSharedPreferences(PREF_SAVED, Context.MODE_PRIVATE).all.map { it.key }