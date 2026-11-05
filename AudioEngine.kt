package com.soundora.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

object AudioEngine {
    const val SR = 44100
    const val ROWS = 7
    const val STEPS = 16
    val rowNames = listOf("Kick", "Snare", "Clap", "Hi-Hat", "Açık Hat", "Tom", "Rim")

    val pattern = Array(ROWS) { BooleanArray(STEPS) }
    val rowVol = FloatArray(ROWS) { 0.8f }
    @Volatile var bpm = 100
    @Volatile var playing = false
    @Volatile var currentStep = -1
    @Volatile var masterVol = 0.8f

    private val voices = CopyOnWriteArrayList<Voice>()
    private var track: AudioTrack? = null
    private var thread: Thread? = null
    @Volatile private var running = false

    fun start() {
        if (running) return
        val min = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SR)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setBufferSizeInBytes(max(min, 2048))
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
        track = t
        running = true
        thread = Thread({ loop(t) }, "soundora-audio").also {
            it.priority = Thread.MAX_PRIORITY
            it.start()
        }
    }

    fun stop() {
        running = false
        thread?.join()
        track?.let { it.stop(); it.release() }
        track = null
        voices.clear()
        playing = false
        currentStep = -1
    }

    private fun loop(t: AudioTrack) {
        val chunk = ShortArray(256)
        t.play()
        var acc = 0.0
        var step = 0
        var wasPlaying = false
        while (running) {
            val p = playing
            if (p && !wasPlaying) { step = 0; acc = 0.0 }
            if (!p && wasPlaying) currentStep = -1
            wasPlaying = p
            val sps = SR * 60.0 / bpm / 4.0
            val snap = voices.toTypedArray()
            for (i in chunk.indices) {
                if (p) {
                    if (acc <= 0.0) {
                        triggerStep(step)
                        currentStep = step
                        step = (step + 1) % STEPS
                        acc += sps
                    }
                    acc -= 1.0
                }
                var mix = 0f
                for (v in snap) if (!v.finished) mix += v.render()
                mix *= masterVol
                chunk[i] = (mix.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            }
            t.write(chunk, 0, chunk.size)
            voices.removeIf { it.finished }
        }
    }

    private fun triggerStep(step: Int) {
        for (r in 0 until ROWS) if (pattern[r][step]) addVoice(DrumVoice(r, rowVol[r]))
    }

    private fun addVoice(v: Voice) {
        if (voices.size > 40) voices.firstOrNull()?.finished = true
        voices.add(v)
    }

    fun hitDrum(row: Int) = addVoice(DrumVoice(row, rowVol[row]))

    fun noteOn(kind: Int, midi: Int): Voice {
        val v = NoteVoice(kind, midi)
        addVoice(v)
        return v
    }
}
