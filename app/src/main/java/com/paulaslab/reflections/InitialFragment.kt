package com.paulaslab.reflections

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import androidx.core.net.toUri
import androidx.fragment.app.FragmentTransaction
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.android.synthetic.main.fragment_initial.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.view

/**
 * A simple [Fragment] subclass.
 * Use the [InitialFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class InitialFragment : Fragment(), AnkoLogger {
    var recyclerView: RecyclerView? = null

    private val mediaSession: MediaSessionCompat by lazy { createMediaSession() }
    private val mediaSessionConnector: MediaSessionConnector by lazy {
        createMediaSessionConnector()
    }
    private val playerState by lazy { PlayerState() }
    private lateinit var playerHolder: PlayerHolder

    var mediaCatalog: List<MediaDescriptionCompat> = listOf()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentManager = this.parentFragmentManager
        val app = activity?.application as ReflectionsApp
        val id = app.journalStore!!.newEntryId()
        val file = app.journalStore!!.getEntryFile(id)

        app.journalStore!!.getEntryFile(0)
        val handler = Handler(context!!.mainLooper)
        val journallingFragment = JournallingFragment.newInstance(
            this,
            file,
            id,
            {
                val pos = app.journalStore!!.listGoodEntries()


                Log.i("Recycler", "Add item: ${pos[pos.size - 1]}")
                handler.post(Runnable {
                    val adapter = getView()?.findViewById<RecyclerView>(R.id.diary_entry_container)?.adapter
                    Log.i("HELLO", "Here and dataset changed: ${adapter}")
                    if (adapter != null) {
                        (adapter as DiaryEntryRow).updateIt()
                    }
                })
            }
        )

        //app.journalStore?.listFiles()
        playVideoEntry(app, 1)

        val journallingFragment = JournallingFragment.newInstance(this, file, id)

        val view = inflater.inflate(R.layout.fragment_initial, container, false)

        view.findViewById<Button>(R.id.start_journalling_button)?.setOnTouchListener(
            View.OnTouchListener { view, motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_UP -> {
                        val ft: FragmentTransaction = fragmentManager.beginTransaction()
                        ft.replace(R.id.flContainer, journallingFragment)
                        ft.commit()
                    }
                }
                false
            })

        // set up the RecyclerView
        recyclerView = view.findViewById<RecyclerView>(R.id.diary_entry_container)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        val adapter = DiaryEntryRow(context!!, app.journalStore!!)
        recyclerView?.adapter = adapter
        
        createMediaSession()
        createPlayer(view)

        return view
    }


    private fun playVideoEntry(app: ReflectionsApp, id: Int) {
        // Assuming here that we actually have some files
        val videofile = app.journalStore?.getEntryFile(id)
        mediaCatalog = listOf(
                with(MediaDescriptionCompat.Builder()) {
                    setDescription("journal entry")
                    setMediaId("1")
                    setMediaUri(videofile!!.toUri())
                    setTitle("Short film")
                    setSubtitle("Local video")
                    build()
                }
        )
    }

    override fun onStart() {
        super.onStart()
        startPlayer()
        activateMediaSession()
    }

    override fun onStop() {
        super.onStop()
        stopPlayer()
        deactivateMediaSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        releaseMediaSession()
    }

    companion object {
        @JvmStatic
        fun newInstance() = InitialFragment()
    }


    // MediaSession related functions.
    private fun createMediaSession(): MediaSessionCompat = MediaSessionCompat(this.context, "InitialFragment")

    private fun createMediaSessionConnector(): MediaSessionConnector =
            MediaSessionConnector(mediaSession).apply {
                // If QueueNavigator isn't set, then mediaSessionConnector will not handle following
                // MediaSession actions (and they won't show up in the minimized PIP activity):
                // [ACTION_SKIP_PREVIOUS], [ACTION_SKIP_NEXT], [ACTION_SKIP_TO_QUEUE_ITEM]
                setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
                    override fun getMediaDescription(windowIndex: Int): MediaDescriptionCompat {
                        return mediaCatalog[windowIndex]
                    }
                })
            }


    // MediaSession related functions.
    private fun activateMediaSession() {
        // Note: do not pass a null to the 3rd param below, it will cause a NullPointerException.
        // To pass Kotlin arguments to Java varargs, use the Kotlin spread operator `*`.
        mediaSessionConnector.setPlayer(playerHolder.audioFocusPlayer, null)
        mediaSession.isActive = true
    }

    private fun deactivateMediaSession() {
        mediaSessionConnector.setPlayer(null, null)
        mediaSession.isActive = false
    }

    private fun releaseMediaSession() {
        mediaSession.release()
    }

    // ExoPlayer related functions.
    private fun createPlayer(view: View) {
        val pv = view.findViewById<PlayerView>(R.id.exoplayerview_activity_video)

        playerHolder = PlayerHolder(this.context, playerState, pv!!)
    }

    private fun startPlayer() {
        playerHolder.start(mediaCatalog)
    }

    private fun stopPlayer() {
        playerHolder.stop()
    }

    private fun releasePlayer() {
        playerHolder.release()
    }

    // Picture in Picture related functions.
    /*override fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(
                    with(PictureInPictureParams.Builder()) {
                        val width = 16
                        val height = 9
                        setAspectRatio(Rational(width, height))
                        build()
                    })
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean,
                                               newConfig: Configuration?) {
        exoplayerview_activity_video.useController = !isInPictureInPictureMode
    }*/
}

/*val mediaCatalog: List<MediaDescriptionCompat> = listOf(
        with(MediaDescriptionCompat.Builder()) {
            setDescription("journal entry")
            setMediaId("1")
            setMediaUri(Uri.parse("http://techslides.com/demos/sample-videos/small.mp4"))
            setTitle("Short film")
            setSubtitle("Local video")
            build()
        }
)*/
