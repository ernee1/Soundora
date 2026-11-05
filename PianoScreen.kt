package com.soundora.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.soundora.app.audio.AudioEngine

private val instrumentNames = listOf("Piyano", "Synth", "Bas", "Akustik Gitar", "Elektro Gitar")

@Composable
fun PianoScreen() {
    var kind by remember { mutableIntStateOf(0) }
    var octave by remember { mutableIntStateOf(4) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Enstrümanlar", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            instrumentNames.forEachIndexed { i, n ->
                FilterChip(selected = kind == i, onClick = { kind = i }, label = { Text(n) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { if (octave > 1) octave-- }) { Text("Oktav −") }
            Text("   Oktav $octave   ")
            OutlinedButton(onClick = { if (octave < 7) octave++ }) { Text("Oktav +") }
        }
        Spacer(Modifier.weight(1f))
        Keyboard(kind, octave, Modifier.fillMaxWidth().height(260.dp))
        Spacer(Modifier.weight(1f))
    }
}

private fun Modifier.keyTouch(semi: Int, kind: State<Int>, oct: State<Int>) = pointerInput(semi) {
    detectTapGestures(onPress = {
        val v = AudioEngine.noteOn(kind.value, 12 * (oct.value + 1) + semi)
        try { tryAwaitRelease() } finally { v.release() }
    })
}

@Composable
private fun Keyboard(kind: Int, octave: Int, modifier: Modifier) {
    val whiteSemis = listOf(0, 2, 4, 5, 7, 9, 11)
    val blackAfter = mapOf(0 to 1, 1 to 3, 3 to 6, 4 to 8, 5 to 10)
    val kindState = rememberUpdatedState(kind)
    val octState = rememberUpdatedState(octave)

    BoxWithConstraints(modifier) {
        val ww = maxWidth / 14
        val bw = ww * 0.62f
        Row(Modifier.fillMaxSize()) {
            for (i in 0 until 14) {
                val semi = whiteSemis[i % 7] + 12 * (i / 7)
                Box(
                    Modifier.width(ww).fillMaxHeight().padding(1.dp)
                        .background(Color.White, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                        .keyTouch(semi, kindState, octState)
                )
            }
        }
        for (i in 0 until 14) {
            val b = blackAfter[i % 7] ?: continue
            val semi = b + 12 * (i / 7)
            Box(
                Modifier.offset(x = ww * (i + 1) - bw / 2).width(bw).fillMaxHeight(0.6f)
                    .background(Color(0xFF111111), RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                    .keyTouch(semi, kindState, octState)
            )
        }
    }
}
