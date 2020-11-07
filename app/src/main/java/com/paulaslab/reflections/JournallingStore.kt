package com.paulaslab.reflections

import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Log
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.Vision.Images.Annotate
import com.google.api.services.vision.v1.VisionRequest
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.*
import com.google.common.io.BaseEncoding
import com.google.common.reflect.Reflection.getPackageName
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import wseemann.media.FFmpegMediaMetadataRetriever
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.collections.ArrayList


object PackageManagerUtils {
    fun getSignature(pm: PackageManager, packageName: String): String? {
        return try {
            val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            if (packageInfo == null || packageInfo.signatures == null || packageInfo.signatures.size == 0 || packageInfo.signatures[0] == null
            ) {
                null
            } else signatureDigest(packageInfo.signatures[0])
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun signatureDigest(sig: Signature): String? {
        val signature: ByteArray = sig.toByteArray()
        return try {
            val md: MessageDigest = MessageDigest.getInstance("SHA1")
            val digest: ByteArray = md.digest(signature)
            BaseEncoding.base16().lowerCase().encode(digest)
        } catch (e: NoSuchAlgorithmException) {
            null
        }
    }
}

class JournallingStore(val context: Context) {
    private val basePath = context.filesDir

    fun getEntryFile(id: Int): File =
        File("${basePath}${File.pathSeparator}${id}${File.pathSeparator}entry.mpg")

    fun getHumorFile(id: Int): File =
        File("${basePath}${File.pathSeparator}${id}${File.pathSeparator}humor.json")

    private fun mapLikelihood(likelihood: String): Int? {
        when (likelihood) {
            "VERY_UNLIKELY" -> return 0
            "UNLIKELY" -> return 1
            "POSSIBLE" -> return 2
            "LIKELY" -> return 3
            "VERY_LIKELY" -> return 4
        }
        return null
    }

    data class JudgementClass(
        val anger: Float?,
        val joy: Float?,
        val surprise: Float?,
        val sorrow: Float?
    ) {
        fun getScore(): Float = (-1.0f) * (anger ?: 3.0f) + (joy ?: 3.0f) + (surprise ?: 3.0f) + (-1.0f) * (sorrow ?: 3.0f)
    }

    fun extractHumor(id: Int): JudgementClass? {
        val humorFile = getHumorFile(id)
        if (humorFile.exists()) {
            val text = humorFile.readText(Charset.forName("UTF-8"))
            val itemType = object : TypeToken<List<JudgementClass>>() {}.type

            val judgement = Gson().fromJson<List<JudgementClass>>(text, itemType)

            var anger: Float? = null
            var angerCount = 0
            var joy: Float? = null
            var joyCount = 0
            var surprise: Float? = null
            var surpriseCount = 0
            var sorrow: Float? = null
            var sorrowCount = 0

            for (j in judgement) {
                if (j.anger != null) {
                    if (anger == null)
                        anger = j.anger
                    else {
                        angerCount++
                        anger += j.anger
                    }
                }

                if (j.joy != null) {
                    if (joy == null)
                        joy = j.joy
                    else {
                        joyCount++
                        joy += j.joy
                    }
                }
                if (j.sorrow != null) {
                    if (sorrow == null)
                        sorrow = j.sorrow
                    else {
                        sorrowCount++
                        sorrow += j.sorrow
                    }
                }
                if (j.surprise != null) {
                    if (surprise == null)
                        surprise = j.surprise
                    else {
                        surpriseCount++
                        surprise += j.surprise
                    }
                }
            }
            return JudgementClass(
                if (angerCount == 0) null else anger!!.toFloat() / angerCount,
                if (joyCount == 0) null else joy!!.toFloat() / joyCount,
                if (sorrowCount == 0) null else sorrow!!.toFloat() / sorrowCount,
                if (surpriseCount == 0) null else surprise!!.toFloat() / surpriseCount
            )
        }
        return null
    }

    fun extractFaces(id: Int) {
        val file = getEntryFile(id)
        if (!file.exists()) return

        val mmr = FFmpegMediaMetadataRetriever()
        mmr.setDataSource(file.absolutePath)

        val time: String =
            mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION.toString())
        val videoDuration = time.toInt() // This will give time in millisecond
        val videoDurationUs = videoDuration.toLong() * 1000;
        val frame1Time = (videoDurationUs.toFloat() * 10 / 100).toLong();
        val frame2Time = (videoDurationUs.toFloat() * 50 / 100).toLong();
        val frame3Time = (videoDurationUs.toFloat() * 95 / 100).toLong();
        Log.d(MainActivity::class.java.simpleName, "Duration: ${videoDurationUs.toString()}")
        Log.d(MainActivity::class.java.simpleName, "Time1: ${frame1Time.toString()}")
        Log.d(MainActivity::class.java.simpleName, "Time2: ${frame2Time.toString()}")
        Log.d(MainActivity::class.java.simpleName, "Time3: ${frame3Time.toString()}")
        val b1: Bitmap = mmr.getFrameAtTime(frame1Time, FFmpegMediaMetadataRetriever.OPTION_CLOSEST)
        val b2: Bitmap = mmr.getFrameAtTime(frame2Time, FFmpegMediaMetadataRetriever.OPTION_CLOSEST)
        val b3: Bitmap = mmr.getFrameAtTime(frame3Time, FFmpegMediaMetadataRetriever.OPTION_CLOSEST)
        mmr.release()

        val req = prepareFaceRequest(b1, b2, b3)

        val response = req!!.execute()

        val judgments = ArrayList<Map<String, Int?>>()
        for (resp in response.responses) {
            val annotations = resp.faceAnnotations
            if (annotations != null) {
                for (annotation in annotations) {
                    judgments.add(
                        mapOf(
                            "anger" to mapLikelihood(annotation.angerLikelihood),
                            "joy" to mapLikelihood(annotation.joyLikelihood),
                            "surprise" to mapLikelihood(annotation.surpriseLikelihood),
                            "sorrow" to mapLikelihood(annotation.sorrowLikelihood)
                        )
                    )
                }
            }
        }

        getHumorFile(id).writeText(Gson().toJson(judgments), Charset.forName("UTF-8"))
        Log.i("FUNNY", "HUMOR: ${extractHumor(id)}")
    }

    private fun prepareFaceRequest(vararg bitmaps: Bitmap): Annotate? {
        val httpTransport: HttpTransport = AndroidHttp.newCompatibleTransport()
        val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
        val requestInitializer: VisionRequestInitializer =
            object : VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                override fun initializeVisionRequest(visionRequest: VisionRequest<*>) {
                    super.initializeVisionRequest(visionRequest)
                    val packageName: String = getPackageName(this.javaClass)
                    visionRequest.requestHeaders[ANDROID_PACKAGE_HEADER] = packageName
                    val sig: String? = PackageManagerUtils.getSignature(
                        context.packageManager,
                        packageName
                    )
                    visionRequest.requestHeaders[ANDROID_CERT_HEADER] = sig
                }
            }
        val builder = Vision.Builder(httpTransport, jsonFactory, null)
        builder.setVisionRequestInitializer(requestInitializer)
        val vision = builder.build()
        val batchAnnotateImagesRequest = BatchAnnotateImagesRequest()
        val requests = ArrayList<AnnotateImageRequest>()

        for (bm in bitmaps) {
            val annotateImageRequest = AnnotateImageRequest()

            // Add the image
            val base64EncodedImage = Image()
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            val byteArrayOutputStream = ByteArrayOutputStream()
            bm.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
            val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes)
            annotateImageRequest.image = base64EncodedImage

            // add the features we want
            annotateImageRequest.features = object : ArrayList<Feature?>() {
                init {
                    val faceDetection = Feature()
                    faceDetection.setType("FACE_DETECTION")
                    add(faceDetection)
                }
            } as List<Feature>?
            requests.add(annotateImageRequest)
        }

        batchAnnotateImagesRequest.requests = requests
        val annotateRequest = vision.images().annotate(batchAnnotateImagesRequest)
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.disableGZipContent = true
        return annotateRequest
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap? {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = maxDimension
        var resizedHeight = maxDimension
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension
            resizedWidth =
                (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight =
                (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension
            resizedWidth = maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }

    fun listFiles() {
        var i = 0
        var file: File = getEntryFile(i)
        while (file.exists()) {
            Log.i("FILMSAVE", "Got ${i}")
            i++
            file = getEntryFile(i)
        }
    }


    fun newEntryId(): Int {
        var i = 0
        var file: File = getEntryFile(i)
        while (file.exists()) {
            ++i
            file = getEntryFile(i)
        }
        file.parentFile.mkdirs()
        return i
    }

    fun newEntryFile(): File {
        var i = 0
        var file: File = getEntryFile(i)
        while (file.exists()) {
            ++i
            file = getEntryFile(i)
        }
        file.parentFile.mkdirs()
        return file
    }

    fun listGoodEntries(): List<Int> {
        var i = 0
        val res = ArrayList<Int>()
        while (true) {
            Log.i("HELLO", "HELLO")
            val file = getEntryFile(i)
            val humorFile = getHumorFile(i)
            if (file.exists() && humorFile.exists())
                res.add(i)
            if (!file.parentFile.exists() || i >= 1000) {
                break
            }
            i++
        }
        return res
    }

    companion object {
        val CLOUD_VISION_API_KEY = "<insert cloud vision key here>"
        val ANDROID_CERT_HEADER = "X-Android-Cert";
        val ANDROID_PACKAGE_HEADER = "X-Android-Package";
    }
}

