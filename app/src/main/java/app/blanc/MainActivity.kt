package app.blanc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import app.blanc.ui.BlancApp
import app.blanc.usage.UsageRecorderWorker

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.factory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        UsageRecorderWorker.schedule(applicationContext)
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
