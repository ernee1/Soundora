package com.soundora.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soundora.app.audio.AudioEngine
import kotlinx.coroutines.delay

private val E = "................"
private val presets: Map<String, Pair<Int, List<String>>> = mapOf(
    // sıra: Kick, Snare, Clap, Hi-Hat, Açık Hat, Tom, Rim
    "Pop" to (110 to listOf("x...x...x...x...", "....x.......x...", E, "x.x.x.x.x.x.x.x.", E, E, E)),
    "Hip-hop" to (90 to listOf("x.....x...x.....", "....x.......x...", E, "x.x.x.x.x.x.x.x.", E, E, E)),
    "Trap" to (140 to listOf("x.......x.x.....", "........x.......", "........x.......", "x.xxx.x.xxx.x.xx", "......x.......x.", E, E)),
    "Lo-fi" to (75 to listOf("x.....x...x..x..", "....x.......x...", E, "x.x.x.x.x.x.x.x.", E, E, ".......x.......x")),
    "House" to (124 to listOf("x...x...x...x...", E, "....x.......x...", "..x...x...x...x.", "..x...x...x...x.", E, E))
)

@Composable
fun BeatScreen() {
    val e = AudioEngine
    var playing by remember { mutableStateOf(e.playing) }
    var bpm by remember { mutableIntStateOf(e.bpm) }
    var step by remember { mutableIntStateOf(-1) }
    val grid = remember {
        mutableStateListOf<Boolean>().apply {
            for (r in 0 until AudioEngine.ROWS) for (s in 0 until AudioEngine.STEPS) add(e.pattern[r][s])
        }
    }
    val vols = remember { mutableStateListOf<Float>().apply { e.rowVol.forEach { add(it) } } }

    LaunchedEffect(Unit) { while (true) { step = e.currentStep; delay(30) } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("Beat Maker", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { playing = !playing; e.playing = playing }) {
                Text(if (playing) "■ Durdur" else "▶ Çal")
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = { if (bpm > 40) { bpm -= 5; e.bpm = bpm } }) { Text("−") }
            Text("  $bpm BPM  ")
            OutlinedButton(onClick = { if (bpm < 220) { bpm += 5; e.bpm = bpm } }) { Text("+") }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { (name, p) ->
                AssistChip(onClick = {
                    bpm = p.first; e.bpm = p.first
                    for (r in 0 until AudioEngine.ROWS) for (s in 0 until AudioEngine.STEPS) {
                        val on = p.second[r][s] == 'x'
                        e.pattern[r][s] = on
                        grid[r * AudioEngine.STEPS + s] = on
                    }
                }, label = { Text(name) })
            }
            AssistChip(onClick = {
                for (r in 0 until AudioEngine.ROWS) for (s in 0 until AudioEngine.STEPS) {
                    e.pattern[r][s] = false; grid[r * AudioEngine.STEPS + s] = false
                }
            }, label = { Text("Temizle") })
        }
        Spacer(Modifier.height(8.dp))

        for (r in 0 until AudioEngine.ROWS) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .background(Card, RoundedCornerShape(12.dp)).padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { e.hitDrum(r) }) { Text(AudioEngine.rowNames[r]) }
                    Spacer(Modifier.weight(1f))
                    Slider(
                        value = vols[r], onValueChange = { vols[r] = it; e.rowVol[r] = it },
                        modifier = Modifier.width(130.dp)
                    )
                }
                Row(Modifier.fillMaxWidth()) {
                    for (s in 0 until AudioEngine.STEPS) {
                        val on = grid[r * AudioEngine.STEPS + s]
                        val base = if (s % 4 == 0) Color(0xFF2A3560) else Color(0xFF1E2748)
                        Box(
                            Modifier.weight(1f).height(30.dp).padding(1.dp)
                                .background(if (on) Purple else base, RoundedCornerShape(4.dp))
                                .then(if (s == step) Modifier.border(2.dp, Blue, RoundedCornerShape(4.dp)) else Modifier)
                                .clickable {
                                    val v = !on
                                    grid[r * AudioEngine.STEPS + s] = v
                                    e.pattern[r][s] = v
                                }
                        )
                    }
                }
            }
        }
    }
}
