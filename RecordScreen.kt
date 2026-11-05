package com.soundora.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.soundora.app.audio.Recorder
import java.io.File

@Composable
fun RecordScreen() {
    val ctx = LocalContext.current
    val view = LocalView.current
    var recording by remember { mutableStateOf(false) }
    val amps = remember { mutableStateListOf<Float>() }
    var files by remember { mutableStateOf(Recorder.list(ctx)) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var renaming by remember { mutableStateOf<File?>(null) }
    var newName by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }

    SideEffect { view.keepScreenOn = recording }
    DisposableEffect(Unit) { onDispose { player?.release(); if (recording) Recorder.stop(); view.keepScreenOn = false } }

    fun startRec() {
        amps.clear()
        Recorder.start(ctx) { a -> amps.add(a); if (amps.size > 150) amps.removeAt(0) }
        recording = true
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) startRec() else msg = "Kayıt için mikrofon izni gerekli."
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Ses Kaydı", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.fillMaxWidth().height(100.dp).background(Card, RoundedCornerShape(12.dp))) {
            val w = size.width / 150f
            amps.forEachIndexed { i, a ->
                val h = (a * size.height).coerceAtLeast(2f)
                drawLine(
                    Purple, Offset(i * w, (size.height - h) / 2), Offset(i * w, (size.height + h) / 2),
                    strokeWidth = w * 0.7f
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            msg = ""
            if (recording) {
                Recorder.stop(); recording = false; files = Recorder.list(ctx)
            } else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startRec()
            } else launcher.launch(Manifest.permission.RECORD_AUDIO)
        }) { Text(if (recording) "■ Kaydı Durdur" else "● Kayda Başla") }
        if (msg.isNotEmpty()) Text(msg, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Text("Kayıtlarım", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(files) { f ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .background(Card, RoundedCornerShape(12.dp)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(f.nameWithoutExtension, Modifier.weight(1f), maxLines = 1)
                    TextButton(onClick = {
                        player?.release()
                        player = MediaPlayer.create(ctx, Uri.fromFile(f))?.also { it.start() }
                    }) { Text("▶") }
                    TextButton(onClick = { renaming = f; newName = f.nameWithoutExtension }) { Text("Ad") }
                    TextButton(onClick = { f.delete(); files = Recorder.list(ctx) }) { Text("Sil") }
                }
            }
        }
    }

    renaming?.let { f ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Yeniden adlandır") },
            text = { OutlinedTextField(newName, { newName = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val clean = newName.trim().replace(Regex("[^\\p{L}\\p{N}_ -]"), "")
                    if (clean.isNotEmpty()) f.renameTo(File(f.parentFile, "$clean.wav"))
                    renaming = null; files = Recorder.list(ctx)
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("İptal") } }
        )
    }
}
