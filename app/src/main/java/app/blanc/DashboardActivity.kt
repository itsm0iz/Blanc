package app.blanc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import app.blanc.ui.dashboard.DashboardApp

/** The launchable "Blanc" app: a normal, opaque app hosting the dashboard. */
class DashboardActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.factory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DashboardApp(viewModel, onClose = { finish() })
        }
    }
}
