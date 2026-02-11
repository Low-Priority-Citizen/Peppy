package com.lowprioritycitizen.peppy.ui.blocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BlockerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Optionally read extras: packageName that triggered
        val triggeredPackage = intent.getStringExtra("trigger_pkg")

        setContent {
            BlockerScreen(
                triggeredPackage = triggeredPackage ?: "Blocked app",
                onGoHome = { goHome() },
                onFinish = { finish() }
            )
        }
    }
}

private fun BlockerActivity.goHome() {
    val home = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(home)
    finish()
}

@Composable
fun BlockerScreen(
    triggeredPackage: String,
    onGoHome: () -> Unit,
    onFinish: () -> Unit
) {
    var waiting by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Hold on — you opened\n$triggeredPackage",
            modifier = Modifier.padding(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (waiting) {
            Text("Wait for $secondsLeft s")
        }

        Row(modifier = Modifier.padding(top = 20.dp)) {

            Button(onClick = onGoHome) {
                Text("Go home")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(onClick = {
                waiting = true
                secondsLeft = 5

                scope.launch {
                    while (secondsLeft > 0) {
                        delay(1000)
                        secondsLeft--
                    }
                    onFinish()   // close blocker after countdown
                }
            }) {
                Text("Wait 5s then continue")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(onClick = onFinish) {
                Text("Continue anyway")
            }
        }
    }
}
