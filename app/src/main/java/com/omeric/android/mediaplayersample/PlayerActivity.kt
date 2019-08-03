package com.omeric.android.mediaplayersample

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class PlayerActivity : AppCompatActivity(), MediaPlayer.OnErrorListener/*, MediaPlayer.OnPreparedListener*/
{
//class PlayerActivity : Service(), MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {

    companion object
    {
        private val TAG = "omerD:" + this::class.java.name
        private const val SKIP_TIME_DURATION_MS = 5000
        private const val GRAYED_OUT = .5f
        private const val UNGRAYED_OUT = 1.0f
    }

    enum class MediaPlayerState
    {
        IDLE, INITIALIZED, PREPARING, PREPARED, STARTED, PAUSED, COMPLETED, STOPPED, ERROR, END
    }

    private lateinit var imageButtonPrevious : ImageButton
    private lateinit var imageButtonRewind : ImageButton
    private lateinit var mediaPlayer : MediaPlayer
    private lateinit var imageButtonPlay : ImageButton
    private lateinit var imageButtonPause : ImageButton
    private lateinit var imageButtonForwards : ImageButton
    private lateinit var imageButtonNext : ImageButton

    private lateinit var textViewPass: TextView
    private lateinit var textViewDuration: TextView
    private lateinit var textViewDue: TextView
    private lateinit var mediaPlayerSeekBar: SeekBar
    private lateinit var runnable: Runnable
    private var handler: Handler = Handler()
    private var mediaPlayerState: MediaPlayerState = MediaPlayerState.IDLE

    // your URL here
//        val url = """https://audio.clyp.it/ckrgpyvc.mp3?Expires=1540153092&Signature=QVPyyw~4N9aAutkc-nE3-fzl~xZQ45sSgw1YqI6gBlLYLGlobkXl8j5916X98k8Ab6FGD6MIewF8N5ojNKLCnVY2YTctrC-ZB4jPyppbcqJqa-SY1B1E0~SF7RYXsTp7jEMdggp4CMvI7Vw8yQ5ExuX01heoi~YzD7yTWk~w1ks_&Key-Pair-Id=APKAJ4AMQB3XYIRCZ5PA"""
//        val url = """http://www.mobiles24.co/downloads/d/g2tgKz2TZg"""
    private val dataSourceUrl = """https://www.ssaurel.com/tmp/mymusic.mp3"""

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        Log.d(TAG, ":onCreate")

        imageButtonPrevious = findViewById(R.id.imgBtn_media_previous)
        imageButtonRewind   = findViewById(R.id.imgBtn_media_rewind)
        imageButtonPlay = findViewById(R.id.imgBtn_media_play)
        imageButtonPause = findViewById(R.id.imgBtn_media_pause)
        imageButtonForwards = findViewById(R.id.imgBtn_media_forwards)
        imageButtonNext     = findViewById(R.id.imgBtn_media_next)
        mediaPlayerSeekBar = findViewById(R.id.seek_bar)
        textViewPass = findViewById(R.id.textVw_passed)
        textViewDuration = findViewById(R.id.textVw_duration)
        textViewDue = findViewById(R.id.textVw_remaining)

        initMediaPlayer()
        initMediaPlayerListeners()
    }


    private fun initMediaPlayer()
    {
        Log.d(TAG, ":initMediaPlayer")

        mediaPlayer = MediaPlayer().apply {
            /* NOTE: 'setAudioStreamType()' is deprecated, but setAudioAttributes is only available from API 21 */
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP)
            {
                @Suppress("DEPRECATION") setAudioStreamType(AudioManager.STREAM_MUSIC)
            }
            else
            {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(
                        AudioAttributes.CONTENT_TYPE_SPEECH).build())
            }

            /** Called when [MediaPlayer] is ready */
            setOnPreparedListener {
                Log.d(TAG, ":initMediaPlayer::mediaPlayer::setOnPreparedListener")
                mediaPlayerState = MediaPlayerState.PREPARED

                startMediaPlayer()
            }

            // Called when the playback reaches the end of stream
            setOnCompletionListener {
                Log.d(TAG, ":initMediaPlayer::mediaPlayer::setOnCompletionListener")
                /** the [MediaPlayer] is now in the [MediaPlayerState.COMPLETED] state */
                stopMediaPlayer()
//                playNext();
            }
        }
    }

    private fun initMediaPlayerListeners()
    {
        // Start the media player
        imageButtonPlay.setOnClickListener {
            Log.d(TAG, ":initMediaPlayer::imageButtonPlay::setOnClickListener")
            startPlaying()
        }

        imageButtonPause.setOnClickListener {
            Log.d(TAG, ":initMediaPlayer::imageButtonPause::setOnClickListener")
            if (mediaPlayer.isPlaying)
            {
                mediaPlayer.pause()
                mediaPlayerState = MediaPlayerState.PAUSED
            }
            togglePlayVisible()
        }

        imageButtonForwards.setOnClickListener {
            Log.d(TAG, ":initMediaPlayer::imageButtonForwards::setOnClickListener")
            val skipToTime = mediaPlayer.currentPosition + SKIP_TIME_DURATION_MS

            if (skipToTime <= mediaPlayer.duration)
            {
                mediaPlayer.seekTo(skipToTime)
/*
                Toast.makeText(applicationContext, "You have Jumped forward ${SKIP_TIME_DURATION_MS / 1000} seconds",
                    Toast.LENGTH_SHORT).show()
*/
            }
            else
            {
                mediaPlayer.seekTo(mediaPlayer.duration)
//                Toast.makeText(applicationContext, "You have Jumped forwards to the end", Toast.LENGTH_SHORT).show()
            }
        }

        imageButtonRewind.setOnClickListener {
            Log.d(TAG, ":initMediaPlayer::imageButtonRewind::setOnClickListener")
            val skipToTime = mediaPlayer.currentPosition - SKIP_TIME_DURATION_MS

            if (skipToTime > 0)
            {
                mediaPlayer.seekTo(skipToTime)
/*
                Toast.makeText(applicationContext, "You have Jumped backward ${SKIP_TIME_DURATION_MS / 1000} seconds",
                    Toast.LENGTH_SHORT).show()
*/
            }
            else
            {
                mediaPlayer.seekTo(0)
//                Toast.makeText(applicationContext, "You have Jumped backwards to the start", Toast.LENGTH_SHORT).show()
            }
        }

        // Seek bar change listener
        mediaPlayerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener
                                                      {
                                                          override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean)
                                                          {
                                                              if (fromUser)
                                                              {
                                                                  mediaPlayer.seekTo(progress * 1000)
                                                              }
                                                          }

                                                          override fun onStartTrackingTouch(seekBar: SeekBar)
                                                          {
                                                          }

                                                          override fun onStopTrackingTouch(seekBar: SeekBar)
                                                          {
                                                          }
                                                      })
    }

    override fun onRestart()
    {
        Log.d(TAG, ":onRestart:")
        super.onRestart()

        // re-init the media player
        initMediaPlayer()
    }

    override fun onPause()
    {
        Log.d(TAG, ":onPause:")
        super.onPause()
        releaseMediaPlayer()
    }

    // When the user switched apps or pressed the home button
    override fun onStop()
    {
        Log.d(TAG, ":onStop:")
        super.onStop()
        releaseMediaPlayer()
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean
    {
        // ... react appropriately ...
        // The MediaPlayer has moved to the Error state, must be reset!
        Log.e(TAG, "MediaPlayer::onError: what = $what, extra = $extra")
        mediaPlayerState = MediaPlayerState.ERROR
        when (what)
        {
            MediaPlayer.MEDIA_ERROR_UNKNOWN     -> Log.e(TAG, "MediaPlayer::onError: unknown media playback error")
            MediaPlayer.MEDIA_ERROR_SERVER_DIED ->
            {
                Log.e(TAG, "MediaPlayer::onError: server connection died")
            }
            else                                -> Log.e(TAG, "MediaPlayer::onError: generic audio playback error")
        }

        when (extra)
        {
            MediaPlayer.MEDIA_ERROR_IO          -> Log.e(TAG, "MediaPlayer::onError: IO media error")
            MediaPlayer.MEDIA_ERROR_MALFORMED   -> Log.e(TAG, "MediaPlayer::onError: media error, malformed")
            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> Log.e(TAG, "MediaPlayer::onError: unsupported media content")
            MediaPlayer.MEDIA_ERROR_TIMED_OUT   -> Log.e(TAG, "MediaPlayer::onError: media timeout error")
            else                                -> Log.e(TAG, "MediaPlayer::onError: unknown playback error")
        }
        Toast.makeText(this, applicationContext.getString(R.string.error_mediaplayer), Toast.LENGTH_SHORT).show()
        return false
    }

    private fun startMediaPlayer()
    {
        Log.d(TAG, ":startMediaPlayer:")
        mediaPlayer.start()
        mediaPlayerState = MediaPlayerState.STARTED

//        textViewDuration.text = "${mediaPlayer.duration/1000/60}:${mediaPlayer.duration/1000 % 60}"
        textViewDuration.text = convertToMinAndSec(mediaPlayer.durationSeconds)
        initializeSeekBar()

        togglePauseVisible()
    }

    // Stop the media player
    private fun stopMediaPlayer()
    {
        Log.d(TAG, ":stopMediaPlayer:")
        togglePlayVisible()

        removeHandlerCallback()

        // transfers a MediaPlayer object to the Idle state
        mediaPlayer.reset()
        mediaPlayerState = MediaPlayerState.IDLE


    }

    private fun releaseMediaPlayer()
    {
        Log.d(TAG, ":releaseMediaPlayer:")
        togglePlayVisible()

        removeHandlerCallback()

        // transfers a MediaPlayer object to the End state
        mediaPlayer.release()
        mediaPlayerState = MediaPlayerState.END


    }

    private fun removeHandlerCallback()
    {
        if (::runnable.isInitialized)
        {
            handler.removeCallbacks(runnable)
        }
    }

    private fun togglePauseVisible()
    {
        imageButtonPlay.visibility = View.GONE
        imageButtonPause.visibility = View.VISIBLE
    }

    private fun togglePlayVisible()
    {
        imageButtonPlay.visibility = View.VISIBLE
        imageButtonPause.visibility = View.GONE
        togglePlayClickable()
    }

    private fun togglePlayUnclickable()
    {
        imageButtonPlay.isClickable = false
        imageButtonPlay.alpha = GRAYED_OUT
    }

    private fun togglePlayClickable()
    {
        imageButtonPlay.isClickable = true
        imageButtonPlay.alpha = UNGRAYED_OUT
    }

    // Method to initialize seek bar and audio stats
    private fun initializeSeekBar()
    {
        Log.d(TAG, ":initializeSeekBar:")
        mediaPlayerSeekBar.max = mediaPlayer.durationSeconds

        runnable = Runnable{
            mediaPlayerSeekBar.progress = mediaPlayer.currentSeconds

            textViewPass.text = convertToMinAndSec(mediaPlayer.currentSeconds)
            val diff = mediaPlayer.durationSeconds - mediaPlayer.currentSeconds
            textViewDue.text = convertToMinAndSec(diff)

            handler.postDelayed(runnable, 1000)
        }
        handler.postDelayed(runnable, 1000)
    }


    private fun startPlaying()
    {
        Log.d(TAG, ":startPlaying: dataSourceUrl = $dataSourceUrl")
        if (dataSourceUrl.isEmpty()) return
        if (mediaPlayerState == MediaPlayerState.PREPARED || mediaPlayerState == MediaPlayerState.PAUSED)
        {
            startMediaPlayer()
        }
        else
        {
            togglePlayUnclickable() // Preventing the user from re-clicking
            mediaPlayer.setDataSource(dataSourceUrl)
            /**
             * transfers a [MediaPlayer] object in the [MediaPlayerState.IDLE] state to the
             * [MediaPlayerState.INITIALIZED] state
             * */
            mediaPlayerState = MediaPlayerState.INITIALIZED
            mediaPlayer.prepareAsync()
            mediaPlayerState = MediaPlayerState.PREPARING
        }
    }

    // Extension property to get media player duration in seconds
    private val MediaPlayer.durationSeconds:Int
        get() {
            return this.duration/1000
        }

    // Extension property to get media player current position in seconds
    private val MediaPlayer.currentSeconds:Int
        get() {
            return this.currentPosition/1000
        }

    // Extension property to convert seconds to compound duration - mm:ss
    private fun convertToMinAndSec(timeSeconds: Int): String
    {
//        Log.d(TAG, ":convertToMinAndSec:")
        return "${timeSeconds/60}:"+"%02d".format(timeSeconds % 60)
    }
}
