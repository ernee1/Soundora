package com.soundora.app.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

abstract class Voice {
    @Volatile var finished = false
    abstract fun render(): Float
    open fun release() {}
}

/** 0 Kick, 1 Snare, 2 Clap, 3 Hi-Hat, 4 Açık Hat, 5 Tom, 6 Rim */
class DrumVoice(private val type: Int, private val gain: Float) : Voice() {
    private var n = 0
    private var phase = 0.0
    private var prev = 0.0
    private val sr = AudioEngine.SR.toDouble()

    override fun render(): Float {
        val t = n / sr
        n++
        val nz = Random.nextDouble() * 2.0 - 1.0
        val s: Double = when (type) {
            0 -> {
                val f = 45.0 + 120.0 * exp(-t * 28.0)
                phase += 2 * PI * f / sr
                if (t > 0.45) finished = true
                sin(phase) * exp(-t * 8.0) * 1.1
            }
            1 -> {
                phase += 2 * PI * 185.0 / sr
                if (t > 0.3) finished = true
                sin(phase) * exp(-t * 20.0) * 0.5 + nz * exp(-t * 18.0) * 0.6
            }
            2 -> {
                val env = if (t < 0.036) exp(-(t % 0.012) * 300.0) else exp(-(t - 0.036) * 25.0)
                if (t > 0.3) finished = true
                nz * env * 0.7
            }
            3 -> {
                val hp = nz - prev; prev = nz
                if (t > 0.07) finished = true
                hp * exp(-t * 70.0) * 0.5
            }
            4 -> {
                val hp = nz - prev; prev = nz
                if (t > 0.35) finished = true
                hp * exp(-t * 10.0) * 0.45
            }
            5 -> {
                val f = 100.0 + 80.0 * exp(-t * 15.0)
                phase += 2 * PI * f / sr
                if (t > 0.5) finished = true
                sin(phase) * exp(-t * 7.0) * 0.9
            }
            else -> {
                phase += 2 * PI * 800.0 / sr
                if (t > 0.08) finished = true
                sin(phase) * exp(-t * 90.0) * 0.6 + nz * exp(-t * 120.0) * 0.3
            }
        }
        return (s * gain).toFloat()
    }
}

/** kind: 0 Piyano, 1 Synth, 2 Bas, 3 Akustik gitar, 4 Elektro gitar */
class NoteVoice(private val kind: Int, midi: Int) : Voice() {
    private val sr = AudioEngine.SR.toDouble()
    private val freq = 440.0 * 2.0.pow((midi - 69) / 12.0)
    private var n = 0
    private var phase = 0.0
    private var lp = 0.0
    private var released = false
    private var rel = 1.0
    private val ks: FloatArray?
    private var ksI = 0

    init {
        ks = if (kind == 3 || kind == 4) {
            val len = max(2, (sr / freq).toInt())
            FloatArray(len) { Random.nextFloat() * 2f - 1f }
        } else null
    }

    override fun release() { released = true }

    override fun render(): Float {
        val t = n / sr
        n++
        if (released) {
            rel -= 1.0 / (0.18 * sr)
            if (rel <= 0.0) { finished = true; return 0f }
        }
        val att = min(1.0, t / 0.005)
        val s: Double = when (kind) {
            0 -> {
                phase += 2 * PI * freq / sr
                val x = sin(phase) + 0.5 * sin(2 * phase) + 0.25 * sin(3 * phase) + 0.12 * sin(4 * phase)
                if (t > 6.0) finished = true
                x * exp(-t * 2.2) * 0.35
            }
            1 -> {
                phase += freq / sr
                if (phase >= 1.0) phase -= 1.0
                val saw = 2.0 * phase - 1.0
                lp += 0.15 * (saw - lp)
                lp * 0.5
            }
            2 -> {
                phase += 2 * PI * freq / sr
                if (t > 4.0) finished = true
                (sin(phase) + 0.4 * sin(2 * phase)) * exp(-t * 1.5) * 0.6
            }
            else -> {
                val buf = ks!!
                val x = buf[ksI]
                val nx = buf[(ksI + 1) % buf.size]
                buf[ksI] = 0.996f * 0.5f * (x + nx)
                ksI = (ksI + 1) % buf.size
                if (t > 4.0) finished = true
                if (kind == 4) tanh(x * 3.0) * 0.4 else x * 0.6
            }
        }
        return (s * att * rel).toFloat()
    }
}
