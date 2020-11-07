package com.paulaslab.reflections

import android.content.Context
import android.util.Log
import java.io.File

class JournallingStore(val context: Context) {
    private val basePath = context.filesDir

    public fun getEntryFile(id: Int): File =
        File("${basePath}${File.pathSeparator}${id}${File.pathSeparator}entry.mpg")

    public fun extractAudio(id: Int) {
        val file = getEntryFile(id)
        if (!file.exists()) return
    }

    public fun listFiles() {
        var i = 0
        var file: File = getEntryFile(i)
        while (file.exists()) {
            Log.i("FILMSAVE", "Got ${i}")
            i++
            file = getEntryFile(i)
        }
    }

    public fun newEntryFile(): File {
        var i = 0
        var file: File = getEntryFile(i)
        while (file.exists()) {
            ++i
            file = getEntryFile(i)
        }
        file.parentFile.mkdirs()
        return file
    }
}
