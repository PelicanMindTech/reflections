package com.paulaslab.reflections

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer


class AudioFromVideo(val context: Context, private val video: String, private val audio: String) {
    private var amc: MediaCodec? = null
    private val ame: MediaExtractor
    private var amf: MediaFormat? = null
    private var amime: String? = null

    private var mSpeechService: SpeechService? = null
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            mSpeechService = SpeechService.from(binder)
            mSpeechService!!.addListener(mSpeechServiceListener)
        }
        override fun onServiceDisconnected(componentName: ComponentName) {
            mSpeechService = null
        }
    }

    private val mSpeechServiceListener: SpeechService.Listener = object : SpeechService.Listener {
        override fun onSpeechRecognized(text: String?, isFinal: Boolean) {
            if (isFinal) {
                Log.i(TAG, "API finished processing audio")
            }
            if (!TextUtils.isEmpty(text)) {
                Log.i(TAG, "Extracted text: ${text}")
            }
        }
    }

    fun init() {
        try {
            //mSpeechService = SpeechService()
            //mSpeechService!!.fetchAccessToken()
            //mSpeechService!!.addListener(mSpeechServiceListener)

            ame.setDataSource(video)
            amf = ame.getTrackFormat(1)
            ame.selectTrack(1)
            amime = amf!!.getString(MediaFormat.KEY_MIME)
            amc = MediaCodec.createDecoderByType(amime!!)
            amc!!.configure(amf, null, null, 0)
            amc!!.start()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun start() {

        context!!.bindService(
            Intent(context, SpeechService::class.java),
            mServiceConnection, Context.BIND_AUTO_CREATE
        )

        AudioService(amc, ame, audio).start()

    }

    private inner class AudioService internal constructor(
        private val amc: MediaCodec?,
        private val ame: MediaExtractor,
        private val destFile: String
    ) :
        Thread() {
        private val aInputBuffers: Array<ByteBuffer>
        private var aOutputBuffers: Array<ByteBuffer>
        override fun run() {
            try {
                val os: OutputStream = FileOutputStream(File(destFile))
                var count: Long = 0
                while (true) {
                    val inputIndex = amc!!.dequeueInputBuffer(0)
                    if (inputIndex == -1) {
                        continue
                    }
                    val sampleSize = ame.readSampleData(aInputBuffers[inputIndex], 0)
                    if (sampleSize == -1) break
                    val presentationTime = ame.sampleTime
                    val flag = ame.sampleFlags
                    ame.advance()
                    amc.queueInputBuffer(inputIndex, 0, sampleSize, presentationTime, flag)
                    val info = MediaCodec.BufferInfo()
                    val outputIndex = amc.dequeueOutputBuffer(info, 0)
                    if (outputIndex >= 0) {
                        val data = ByteArray(info.size)
                        aOutputBuffers[outputIndex].get(data, 0, data.size)
                        aOutputBuffers[outputIndex].clear()
                        os.write(data)
                        count += data.size.toLong()
                        amc.releaseOutputBuffer(outputIndex, false)
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        aOutputBuffers = amc.outputBuffers
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    }
                }
                os.flush()
                os.close()

                val audioFile = File(destFile)
                Log.i("AUDIOOOOO", "Audio file: ${audioFile.exists().toString()}")

                // send audio to speech to text
                //val audioInput = audioFile.inputStream()
                //mSpeechService = SpeechService()
                //mSpeechService!!.fetchAccessToken()
                //mSpeechService!!.addListener(mSpeechServiceListener)
                //mSpeechService!!.recognizeInputStream(audioInput)


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        init {
            aInputBuffers = amc!!.inputBuffers
            aOutputBuffers = amc.outputBuffers
        }

    }

    init {
        ame = MediaExtractor()
        init()
    }

    companion object {
        val TAG = "AUDIOOOOO"
    }
}