package app.areada

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import app.areada.ui.AreadaApp

class MainActivity : ComponentActivity() {
    private val externalOpenUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalOpenUri.value = viewUriFrom(intent)
        enableEdgeToEdge()
        setContent {
            AreadaApp(
                externalOpenUri = externalOpenUri.value,
                onExternalOpenHandled = { externalOpenUri.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalOpenUri.value = viewUriFrom(intent)
    }

    private fun viewUriFrom(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_VIEW) {
            return null
        }
        val uri = intent.data ?: return null
        return when (uri.scheme?.lowercase()) {
            "content", "file" -> uri
            else -> null
        }
    }
}
