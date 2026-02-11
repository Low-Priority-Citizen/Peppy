package com.lowprioritycitizen.peppy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.app.usage.UsageStatsManager
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.lowprioritycitizen.peppy.monitoring.ForegroundMonitoringService
import com.lowprioritycitizen.peppy.ui.theme.PeppyTheme
import kotlinx.coroutines.delay
import kotlin.collections.arrayListOf


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            var hasAccess by remember {
                mutableStateOf(hasUsageAccess(this))
            }

            // Re-check permission whenever the app resumes
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(lifecycleOwner.lifecycle) {
                lifecycleOwner.lifecycle.repeatOnLifecycle(
                    androidx.lifecycle.Lifecycle.State.RESUMED
                ) {
                    hasAccess = hasUsageAccess(this@MainActivity)
                }
            }

            if (hasAccess) {
                Column {
                    Text("Usage Access is granted ✅\n")
                    Blacklist(arrayOf("Peppy","Instagram"))
                    // when permission granted:
                    Button(onClick = {
                        val svc = Intent(this@MainActivity, ForegroundMonitoringService::class.java)
                        startService(svc) // starts foreground service; user will see notification
                    }) {
                        Text("Start monitoring")
                    }

                    Button(onClick = {
                        val svc = Intent(this@MainActivity, ForegroundMonitoringService::class.java)
                        stopService(svc)
                    }) {
                        Text("Stop monitoring")
                    }
                }
            } else {
                Column {
                    Text(
                        "To help you avoid doomscrolling, Peppy needs Usage Access.\n" +
                                "Please enable it in , and while you're there, have a look at apps" +
                                " that didn't ask for permission👀."
                    )
                    Button(onClick = {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }) {
                        Text("Open Settings")
                    }
                }
            }

            LaunchedEffect(Unit) {
                while (true) {
                    val pkg = getForegroundApp(this@MainActivity)
                    Log.d("Peppy", "Foreground app: $pkg")
                    delay(1000)
                }
            }
        }
    }

    private fun hasUsageAccess(context: Context): Boolean {
        val usageStatsManager =
            context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager

        val end = System.currentTimeMillis()
        val start = end - 1000 * 60   // last minute

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end
        )
        return !stats.isNullOrEmpty()
    }

    private fun getForegroundApp(context: Context): String? {
        val usageStatsManager =
            context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager

        val end = System.currentTimeMillis()
        val start = end - 1000 * 60  // last minute

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end
        ) ?: return null

        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }
}

@Composable
fun Blacklist(blacklist: Array<String>, modifier: Modifier = Modifier) {
    Text(
        text = "Blacklisted Apps:" +
                arrayListOf(blacklist),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun BlacklistPreview() {
    PeppyTheme {
        Blacklist(arrayOf("Peppy","Instagram"))
    }
}