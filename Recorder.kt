package com.soundora.app.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

object Recorder {
    private const val SR = 44100
    private var rec: AudioRecord? = null
    @Volatile private var running = false
    private var thread: Thread? = null
    private val main = Handler(Looper.getMainLooper())

    fun dir(ctx: Context): File = File(ctx.filesDir, "recordings").apply { mkdirs() }

    fun list(ctx: Context): List<File> =
        dir(ctx).listFiles { f -> f.extension == "wav" }?.sortedByDescending { it.lastModified() } ?: emptyList()

    @SuppressLint("MissingPermission")
    fun start(ctx: Context, onAmp: (Float) -> Unit) {
        if (running) return
        val min = AudioRecord.getMinBufferSize(SR, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val r = AudioRecord(
            MediaRecorder.AudioSource.MIC, SR,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(min, 8192)
        )
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir(ctx), "Kayit_$stamp.wav")
        rec = r
        running = true
        r.startRecording()
        thread = Thread {
            val raf = RandomAccessFile(file, "rw")
            raf.write(ByteArray(44))
            val buf = ShortArray(1024)
            val bytes = ByteArray(2048)
            var total = 0L
            while (running) {
                val n = r.read(buf, 0, buf.size)
                if (n > 0) {
                    var peak = 0
                    for (i in 0 until n) {
                        val s = buf[i].toInt()
                        bytes[2 * i] = (s and 0xff).toByte()
                        bytes[2 * i + 1] = ((s shr 8) and 0xff).toByte()
                        peak = max(peak, abs(s))
                    }
                    raf.write(bytes, 0, n * 2)
                    total += n * 2
                    val a = peak / 32768f
                    main.post { onAmp(a) }
                }
            }
            r.stop(); r.release()
            raf.seek(0); raf.write(header(total)); raf.close()
        }.also { it.start() }
    }

    fun stop() {
        running = false
        thread?.join()
        thread = null
        rec = null
    }

    private fun header(dataLen: Long): ByteArray {
        val b = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()); b.putInt((36 + dataLen).toInt())
        b.put("WAVE".toByteArray()); b.put("fmt ".toByteArray())
        b.putInt(16); b.putShort(1); b.putShort(1)
        b.putInt(SR); b.putInt(SR * 2); b.putShort(2); b.putShort(16)
        b.put("data".toByteArray()); b.putInt(dataLen.toInt())
        return b.array()
    }
}
