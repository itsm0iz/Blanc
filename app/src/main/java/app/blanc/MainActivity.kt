package app.blanc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import app.blanc.ui.BlancApp
import app.blanc.ui.CrashScreen
import app.blanc.usage.UsageRecorderWorker
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.factory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the previous run crashed, show the captured report instead of
        // re-running the (crashing) UI. This breaks the crash loop and makes
        // the error visible.
        val crashFile = File(filesDir, BlancApplication.CRASH_FILE)
        if (crashFile.exists()) {
            val report = runCatching { crashFile.readText() }.getOrDefault("(crash report unreadable)")
            runCatching { crashFile.delete() }
            setContent { CrashScreen(report) }
            return
        }

        enableEdgeToEdge()
        runCatching { UsageRecorderWorker.schedule(applicationContext) }
        setContent {
            BlancApp(viewModel)
        }
    }

    /** Pressing the home button while Blanc is already foreground returns to Home. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.goHome()
    }
}
