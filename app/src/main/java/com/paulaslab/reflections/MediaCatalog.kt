/*
 * Copyright 2018 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.paulaslab.reflections

import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import java.io.File

/**
 * Manages a set of media metadata that is used to create a playlist for [VideoActivity].
 */
val mediaCatalog: List<MediaDescriptionCompat> = listOf(
        with(MediaDescriptionCompat.Builder()) {
            setDescription("mp")
            setMediaId("1")
            //setMediaUri(Uri.fromFile(File("/storage/emulated/0/Android/media/com.paulaslab.reflections/reflections/film")))
            //setMediaUri(Uri.parse("https://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4"))
            //setMediaUri(Uri.parse ("/storage/emulated/0/Android/media/com.paulaslab.reflections/reflections/film"))
            //setMediaUri(Uri.parse ("file:///storage/emulated/0/Android/media/com.paulaslab.reflections/reflections/film"))
            setMediaUri(Uri.parse("http://techslides.com/demos/sample-videos/small.mp4"))
            setTitle("Short film")
            setSubtitle("Streaming video")
            build()
        }
)
