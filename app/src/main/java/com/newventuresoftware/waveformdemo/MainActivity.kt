/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.newventuresoftware.waveformdemo

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.view.ViewTreeObserver

import com.newventuresoftware.waveform.WaveformView

import org.apache.commons.io.IOUtils

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

import rm.com.audiowave.AudioWaveView

class MainActivity : AppCompatActivity() {

    private var mRecordingThread: RecordingThread? = null
    private var mPlaybackThread: PlaybackThread? = null

    private val audioSample: ShortArray
        @Throws(IOException::class)
        get() {
            val `is` = resources.openRawResource(R.raw.sample)
            val data: ByteArray
            try {
                data = IOUtils.toByteArray(`is`)
            } finally {
                `is`?.close()
            }

            val sb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val samples = ShortArray(sb.limit())
            sb.get(samples)
            return samples
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val mPlaybackView = findViewById<View>(R.id.playbackWaveformView) as WaveformView


        var samples: ShortArray? = null
        try {
            samples = audioSample
        } catch (e: IOException) {
            e.printStackTrace()
        }



        if (samples != null) {
            val playFab = findViewById<View>(R.id.playFab) as FloatingActionButton

            mPlaybackThread = PlaybackThread(samples, object : PlaybackListener {
                override fun onProgress(progress: Int) {
                }

                override fun onCompletion() {
                    playFab.setImageResource(android.R.drawable.ic_media_play)
                }
            })
            mPlaybackView.channels = 1

            mPlaybackView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener{
                override fun onPreDraw(): Boolean {
                    mPlaybackView.viewTreeObserver.removeOnPreDrawListener(this)
                    mPlaybackView.samples = samples
                    return true
                }

            })


            playFab.setOnClickListener {
                if (!mPlaybackThread!!.playing()) {
                    mPlaybackThread!!.startPlayback()
                    playFab.setImageResource(android.R.drawable.ic_media_pause)
                } else {
                    mPlaybackThread!!.stopPlayback()
                    playFab.setImageResource(android.R.drawable.ic_media_play)
                }
            }
        }

        val wave = findViewById<AudioWaveView>(R.id.wave)

        wave.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener{
            override fun onPreDraw(): Boolean {
                wave.viewTreeObserver.removeOnPreDrawListener(this)
                wave.setRawData(samples!!)
                Log.e("WAVE-NOR", "mSamples.length: " + resources.openRawResource(R.raw.sample).readBytes().size)
                return true
            }

        })

    }

    override fun onStop() {
        super.onStop()

        mRecordingThread!!.stopRecording()
        mPlaybackThread!!.stopPlayback()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)

    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.size > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mRecordingThread!!.stopRecording()
        }
    }

    companion object {
        private val REQUEST_RECORD_AUDIO = 13
    }
}
