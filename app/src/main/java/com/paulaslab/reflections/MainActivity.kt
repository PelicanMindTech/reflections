package com.paulaslab.reflections

import android.Manifest
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.util.Rational
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.core.VideoCapture.OnVideoSavedCallback
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.AnkoLogger
import wseemann.media.FFmpegMediaMetadataRetriever
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity(), AnkoLogger {
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private val mediaSession: MediaSessionCompat by lazy { createMediaSession() }
    private val mediaSessionConnector: MediaSessionConnector by lazy {
        createMediaSessionConnector()
    }
    private val playerState by lazy { PlayerState() }
    private lateinit var playerHolder: PlayerHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // While the user is in the app, the volume controls should adjust the music volume.
        //volumeControlStream = AudioManager.STREAM_MUSIC
        //createMediaSession()
        //createPlayer()

        val mmr = FFmpegMediaMetadataRetriever()
        mmr.setDataSource("http://techslides.com/demos/sample-videos/small.mp4")

        val time: String = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION.toString())
        val videoDuration = time.toInt() // This will give time in millisecond
        // 1 millisecond = 1000 microseconds
        // 1 second = 1000000 microseconds
        val videoDurationUs = videoDuration.toLong()*1000;
        val frame1Time = (videoDurationUs.toFloat() * 10/100).toLong();
        val frame2Time = (videoDurationUs.toFloat() * 50/100).toLong();
        val frame3Time = (videoDurationUs.toFloat() * 95/100).toLong();
        Log.d(MainActivity::class.java.simpleName, "Duration: ${videoDurationUs.toString()}")
        Log.d(MainActivity::class.java.simpleName, "Time1: ${frame1Time.toString()}")
        Log.d(MainActivity::class.java.simpleName, "Time2: ${frame2Time.toString()}")
        Log.d(MainActivity::class.java.simpleName, "Time3: ${frame3Time.toString()}")

        val b1: Bitmap = mmr.getFrameAtTime(frame1Time, FFmpegMediaMetadataRetriever.OPTION_CLOSEST)
        val b2: Bitmap = mmr.getFrameAtTime(frame2Time, FFmpegMediaMetadataRetriever.OPTION_CLOSEST)
        val b3: Bitmap = mmr.getFrameAtTime(frame3Time, FFmpegMediaMetadataRetriever.OPTION_CLOSEST)
        //val artwork: ByteArray = mmr.getEmbeddedPicture()
        mmr.release()

        val frameImage1: ImageView = findViewById(R.id.frame_image_1) as ImageView
        frameImage1.setImageBitmap(b1);
        val frameImage2: ImageView = findViewById(R.id.frame_image_2) as ImageView
        frameImage2.setImageBitmap(b2);
        val frameImage3: ImageView = findViewById(R.id.frame_image_3) as ImageView
        frameImage3.setImageBitmap(b3);

        //val file = File(getOutputDirectory(), "film")
        //Log.d(MainActivity::class.java.simpleName, "File length: ${file.length()}")
        //Log.d(MainActivity::class.java.simpleName, "File can read: ${file.canRead()}")

        // Request camera permissions
        if (allPermissionsGranted()) {
            //TODO: remove comment below
            //startCamera()
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
            startCamera()
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {}


    @SuppressLint("RestrictedApi", "UnsafeExperimentalUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        val executor = ContextCompat.getMainExecutor(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val videoCapture = VideoCapture.Builder().apply {
                setAudioRecordSource(1)
            }.build()

            // Preview
            val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.createSurfaceProvider())
                    }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(this, cameraSelector, videoCapture)
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            val obj = object : OnVideoSavedCallback {
                override fun onVideoSaved(file: File) {
                    //TODO("Not yet implemented")
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    //TODO("Not yet implemented")
                }

            }
            val file = File(getOutputDirectory(), "film")
            Log.d(MainActivity::class.java.simpleName, "File exists: ${file.exists()}")
            //videoCapture.startRecording(file, cameraExecutor, obj)

        }, executor)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults:
            IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                        this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onStart() {
        super.onStart()
        //startPlayer()
        //activateMediaSession()
    }

    override fun onStop() {
        super.onStop()
        //stopPlayer()
        //deactivateMediaSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        //releasePlayer()
        //releaseMediaSession()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    // MediaSession related functions.
    private fun createMediaSession(): MediaSessionCompat = MediaSessionCompat(this, packageName)

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
    /*private fun createPlayer() {
        playerHolder = PlayerHolder(this, playerState, exoplayerview_activity_video)
    }

    private fun startPlayer() {
        playerHolder.start()
    }

    private fun stopPlayer() {
        playerHolder.stop()
    }

    private fun releasePlayer() {
        playerHolder.release()
    }*/

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