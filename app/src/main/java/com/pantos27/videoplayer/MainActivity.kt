package com.pantos27.videoplayer

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.exoplayer2.util.MimeTypes
import com.pantos27.videoplayer.data.MediaFileInfo
import com.pantos27.videoplayer.recycler.VideoAdapter
import com.pantos27.videoplayer.viewmodels.VideosViewModel

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.content.Intent
import android.net.Uri
import android.os.Build




const val MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1100
const val TAG = "MainActivity"
class MainActivity : AppCompatActivity(), VideoAdapter.VideoAdapterListener {
    override fun onClick(video: MediaFileInfo) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onLongClick(video: MediaFileInfo) {

    }

    private var adapter : VideoAdapter? = null
    var videoViewModel: VideosViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            videoViewModel?.let {
                PlayerActivity.startPlayerActivity(context = view.context, items = it.selectedItems)
                clearSelected(it)

            }
        }

        main_list.emptyView = main_empty

        onAllowClick(null)


    }

    fun onAllowClick(view: View?){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            main_empty.visibility = View.GONE
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS)

            return
        }else{
            loadData()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (MY_PERMISSIONS_REQUEST_READ_CONTACTS == requestCode){
            if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadData()

            } else {

                Toast.makeText(this, "Bummer", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun loadData() {
        Log.d(TAG,"loadData")
        main_header.visibility = View.GONE
        main_button_allow.visibility = View.GONE

        ViewModelProviders.of(this).get(VideosViewModel::class.java).run {
            videoViewModel = this
            val cursor = if (this.videocursor ==null) this.getCursor(this@MainActivity) else this.videocursor
            if (cursor==null){
                Toast.makeText(this@MainActivity, "Can't display videos right now. Try later", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            adapter = VideoAdapter(this@MainActivity,cursor,0,this)

            main_list.adapter  = adapter
            this.multiLiveData.observe(this@MainActivity, Observer { multi->
                if (multi == true) fab.show() else fab.hide()
            })
        }



    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_saved -> { showPlaylistsDialog(); return true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showPlaylistsDialog() {
        val items = getAllPlaylists(this)
        val builder = AlertDialog.Builder(this)
                .setTitle(R.string.load_playlist)
                .setIcon(R.drawable.ic_movie_black_24dp)

        if (items.isEmpty()){
            builder.setMessage(R.string.no_playlists_found)
        }else {
            val adapter = ArrayAdapter<String>(this, android.R.layout.simple_selectable_list_item, items)


            builder.setAdapter(adapter, { _, i -> setPLaylist(adapter.getItem(i)) })
        }
         builder.show()
    }

    private fun setPLaylist(item: String) {
        val playlist = getPlaylist(this,item)
        videoViewModel?.let { videoViewModel->
            val cursor = videoViewModel.getCursor(this,playlist)
            cursor?.let {
                videoViewModel.videocursor = cursor
                adapter?.swapCursor(it)
                videoViewModel.playlist = true
            }
        }
    }

    fun scanForNewMedia(view: View){
        val baseFolder = Environment.getExternalStorageDirectory()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(baseFolder))
            sendBroadcast(scanIntent)
        } else {
            val intent = Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + baseFolder))
            sendBroadcast(intent)
        }

        MediaScannerConnection.scanFile(this, arrayOf(baseFolder.absolutePath),
                arrayOf(MimeTypes.BASE_TYPE_VIDEO),{ _, _ ->
            view.post {   loadData() }
        })
    }

    override fun onStop() {
        super.onStop()

        adapter?.dispose()
    }

    override fun onBackPressed() {
        //check if items are selected
        videoViewModel?.let {
            if (it.multiLiveData.value == true){
                clearSelected(it)
                return
            }
            if (it.playlist){
                it.playlist = false
                it.videocursor = it.getCursor(this)
                adapter?.swapCursor(it.videocursor)
                clearSelected(it)
                return
            }
        }
        //exit
        super.onBackPressed()
    }

    fun clearSelected(viewModel: VideosViewModel){
        viewModel.multiLiveData.postValue(false)
        viewModel.selectedItems.clear()
        adapter?.notifyDataSetChanged()
    }
}
