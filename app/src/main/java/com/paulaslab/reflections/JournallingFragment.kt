package com.paulaslab.reflections

import android.annotation.SuppressLint
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_journalling.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A simple [Fragment] subclass.
 * Use the [JournallingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class JournallingFragment : Fragment() {

    private var ttsInitialized : AtomicBoolean = AtomicBoolean(false)
    private var tts: TextToSpeech? = null

    private var currentQuestion = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun nextQuestion() {
        if (!ttsInitialized.get()) return

        currentQuestion++
        val question = QUESTIONS[currentQuestion]
        tts!!.speak(question, TextToSpeech.QUEUE_ADD, null, "question-${currentQuestion}")
        getView()?.findViewById<TextView>(R.id.question_text)?.text = question
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_journalling, container, false)
        startCamera()
        val t = this
        tts = TextToSpeech(context!!, TextToSpeech.OnInitListener {
                status ->
                    when (status) {
                        TextToSpeech.SUCCESS -> {
                            tts!!.setLanguage(Locale.CANADA)
                            ttsInitialized.set(true)
                            this@JournallingFragment.nextQuestion()
                        }
                    }
        })

        return view
    }

    @SuppressLint("RestrictedApi", "UnsafeExperimentalUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context!!)

        val executor = ContextCompat.getMainExecutor(context!!)

        Log.e("PreviewView", executor.toString())
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            //val videoCapture = VideoCapture.Builder().apply {
            //    setAudioRecordSource(1)
            //}.build()

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

                //cameraProvider.bindToLifecycle(this, cameraSelector, videoCapture)
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (exc: Exception) {
                Log.e("JournallingFragment", "Use case binding failed", exc)
            }

//            val obj = object : OnVideoSavedCallback {
//                override fun onVideoSaved(file: File) {
//                    //TODO("Not yet implemented")
//                }
//
//                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
//                    //TODO("Not yet implemented")
//                }
//
//            }
//            val file = File(getOutputDirectory(), "film")
//            videoCapture.startRecording(file, cameraExecutor, obj)
        }, executor)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    companion object {
        val QUESTIONS = arrayOf(
            "How are you today?",
            "What is your favorite color?",
            "Which is fastest: an american or an european swallow?"
        )
        @JvmStatic
        fun newInstance() = JournallingFragment()
    }
}