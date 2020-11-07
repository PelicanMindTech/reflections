package com.paulaslab.reflections

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import java.io.File
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A simple [Fragment] subclass.
 * Use the [JournallingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class JournallingFragment(val previousFragment: Fragment, val filmFile: File) : Fragment() {

    private val ttsInitialized : AtomicBoolean = AtomicBoolean(false)
    private var tts: TextToSpeech? = null

    private var videoCapture: VideoCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: Executor? = null
    private var currentQuestion = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    @SuppressLint("RestrictedApi")
    fun endJournalEntry() {
        currentQuestion = -1
        videoCapture?.stopRecording()
        //cameraProvider?.unbindAll()

    }

    fun nextQuestion() {
        if (!ttsInitialized.get()) return
        if (currentQuestion == QUESTIONS.size - 1) return
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

        view.findViewById<Button>(R.id.next_question_button).setOnTouchListener(View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    if (currentQuestion == QUESTIONS.size - 1) {
                        endJournalEntry()
                    } else {
                        nextQuestion()
                        if (currentQuestion == QUESTIONS.size - 1) {
                            val b: Button = v as Button
                            b.setBackgroundColor(Color.RED)
                            b.setText("End entry")
                        }
                    }
                }
            }
            false
        })

        tts = TextToSpeech(context!!, TextToSpeech.OnInitListener {
                status ->
                    when (status) {
                        TextToSpeech.SUCCESS -> {
                            tts!!.setLanguage(Locale.FRENCH)
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


        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()


            videoCapture = VideoCapture.Builder().build()

            val viewFinder = getView()?.findViewById<PreviewView>(R.id.viewFinder)
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder?.createSurfaceProvider())
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()

                cameraProvider?.bindToLifecycle(this, cameraSelector, videoCapture)
                // Bind use cases to camera
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview)
            } catch (exc: Exception) {
                Log.e("JournallingFragment", "Use case binding failed", exc)
            }

            val obj = object : VideoCapture.OnVideoSavedCallback {
                override fun onVideoSaved(file: File) {
                    Log.i("FILMSAVE", "Film saved")

                    val fragmentManager = parentFragmentManager

                    val ft: FragmentTransaction = fragmentManager.beginTransaction()

                    ft.replace(R.id.flContainer, previousFragment)
                    ft.commit()
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    Log.i("FILMSAVE", "File save file: ${message} ${cause}")
                }

            }
            videoCapture?.startRecording(filmFile, cameraExecutor!!, obj)
        }, executor)
    }

    @SuppressLint("RestrictedApi")
    override fun onPause() {

        Log.i("SAVINGFILE", "Destroying")
        //cameraProvider?.unbindAll()
        //videoCapture?.stopRecording()
        super.onPause()
    }

    @SuppressLint("RestrictedApi")
    override fun onDestroy() {
        Log.i("SAVINGFILE", "Destroying")
        //videoCapture?.stopRecording()
        //cameraProvider?.unbindAll()
        super.onDestroy()
    }
    companion object {
        val QUESTIONS = arrayOf(
            "How are you today?",
            "What is your favorite color?",
            "Which is fastest: an african or a european swallow?"
        )
        @JvmStatic
        fun newInstance(previousFragment: Fragment, file: File) = JournallingFragment(previousFragment, file)
    }
}