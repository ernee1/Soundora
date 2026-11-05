package com.soundora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.soundora.app.audio.AudioEngine
import com.soundora.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AudioEngine.start()
        setContent { SoundoraTheme { SoundoraApp() } }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) AudioEngine.stop()
    }
}

@Composable
fun SoundoraApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val items = listOf("🥁" to "Beat", "🎹" to "Enstrüman", "🎤" to "Kayıt")
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = Card) {
                items.forEachIndexed { i, (icon, label) ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Text(icon) }, label = { Text(label) }
                    )
                }
            }
        }
    ) { pad ->
        androidx.compose.foundation.layout.Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> BeatScreen()
                1 -> PianoScreen()
                else -> RecordScreen()
            }
        }
    }
}
