package com.pantos27.videoplayer

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlaybackControlView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.pantos27.videoplayer.data.MediaFileInfo
import kotlinx.android.synthetic.main.activity_player.*
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class PlayerActivity : AppCompatActivity(), PlaybackControlView.VisibilityListener, ExtractorMediaSource.EventListener, Player.EventListener {

    private val mHideHandler = Handler()
//    private var player: SimpleExoPlayer = null
    var audioManager: AudioManager? = null

    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        player_view.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        fullscreen_content_controls.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_player)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mVisible = true

        // Set up the user interaction to manually show or hide the system UI.
//        player_view.setOnClickListener { toggle() }
        player_view.setControllerVisibilityListener(this)
        player_view.requestFocus()
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
//        player_btn_volume_up.setOnTouchListener(mDelayHideTouchListener)

        mHidePart2Runnable.run()

        files = intent.getStringArrayExtra(EXTRA_FILES)
        if (files.isEmpty()){
            finish()
            return
        }
        initPlayer(files)

    }

    var files = arrayOf<String>()

    override fun onVisibilityChange(visibility: Int) {
        Log.d(TAG,"onVisibilityChange $visibility")

        fullscreen_content_controls.visibility = visibility
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000
        val TAG = "PlayerActivity"

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300

        val EXTRA_FILES = "extra_files_paths"

        fun startPlayerActivity(context: Context, items: List<MediaFileInfo>){
            Log.d(PlayerActivity.TAG,"startPlayerActivity with ${items.size} items")
            val intent = Intent(context, PlayerActivity::class.java)
                    .putExtra(PlayerActivity.EXTRA_FILES,items.map { it.path }.toTypedArray())

            context.startActivity(intent)
        }

    }

    override fun onStop() {
        Log.d(TAG,"onStop")
        super.onStop()

        if (Build.VERSION.SDK_INT>23){
            releasePlayer()
        }

        finish()
    }

    override fun onPause() {
        Log.d(TAG,"onPause")
        super.onPause()
        if (Build.VERSION.SDK_INT<=23){
            releasePlayer()
        }
    }


    private fun initPlayer(files: Array<String>) {
        val defaultDataSourceFactory = DefaultDataSourceFactory(this,TAG)
        val sources = files.map { ExtractorMediaSource(Uri.parse(it),defaultDataSourceFactory,DefaultExtractorsFactory(),mHideHandler,this)}
        val mediaSource = if (sources.size>1) ConcatenatingMediaSource(*sources.toTypedArray()) else sources[0]
        mediaSource.releaseSource()
        val player = ExoPlayerFactory.newSimpleInstance(this,DefaultTrackSelector())

        player.addListener(this)
        player.repeatMode = Player.REPEAT_MODE_ALL
        player_view.player = player
        player.playWhenReady = true
        player.prepare(mediaSource)

        (getSystemService(Context.AUDIO_SERVICE) as AudioManager).also {
            audioManager = it
            volume = it.getStreamVolume(AudioManager.STREAM_MUSIC)
            maxVolume = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        }
    }
    var volume = 0
    var maxVolume = 1
    private fun releasePlayer() {

        player_view.player?.let {
            it.release()
        }
    }

    fun onVolumeUpClick(view: View){
        Log.d(TAG,"volumeup")
        volume = min(maxVolume,++volume)
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC,volume ,AudioManager.FLAG_SHOW_UI)
    }
    fun onVolumeDownClick(view: View){
        Log.d(TAG,"volumeup")
        volume = max(0,--volume)
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC,volume, AudioManager.FLAG_SHOW_UI)
    }
    fun onSavePlaylist(view: View){
        Log.d(TAG,"volumeup")
        val editText = layoutInflater.inflate(R.layout.playlist_name,null) as EditText
        AlertDialog.Builder(this).setView(editText)
                .setTitle(R.string.save_this_playlist)
                .setPositiveButton(R.string.save,{_, _ ->
                    Log.d(TAG,"save with name ${editText.text}")
                    if (editText.text.toString().trim().isNotEmpty()){
                        savePlaylist(this,files,editText.text.toString().trim())
                        Toast.makeText(this, R.string.playlist_saved, Toast.LENGTH_SHORT).show()

                    }else{
                        Toast.makeText(this@PlayerActivity, R.string.empty_name, Toast.LENGTH_SHORT).show()
                    }
                    mHidePart2Runnable.run()
                }).setNegativeButton(R.string.cancel,{_,_->})
                .show()

    }

    fun onExit(view: View){
        Log.d(TAG,"onExit")
        finish()
    }
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
    }

    override fun onSeekProcessed() {
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        Log.e(TAG,"onPlayerError ${error?.message}")
        finish()
    }

    override fun onLoadingChanged(isLoading: Boolean) {
    }

    override fun onPositionDiscontinuity(reason: Int) {
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Log.d(TAG,"onPlayerStateChanged $playWhenReady $playbackState")
    }

    override fun onLoadError(error: IOException?) {
        Log.e(TAG,"onLoadError ${error?.message}")

        finish()
    }

}
