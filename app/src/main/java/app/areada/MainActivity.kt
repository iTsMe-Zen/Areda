package app.areada

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import app.areada.ui.AreadaApp
import app.areada.ui.VolumePageTurnHost

class MainActivity : ComponentActivity(), VolumePageTurnHost {
    private val externalOpenUri = mutableStateOf<Uri?>(null)
    private var volumePageTurnHandler: ((volumeUp: Boolean) -> Boolean)? = null

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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handler = volumePageTurnHandler
        if (
            handler != null &&
            event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0
        ) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> if (handler(true)) return true
                KeyEvent.KEYCODE_VOLUME_DOWN -> if (handler(false)) return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun setVolumePageTurnHandler(handler: ((volumeUp: Boolean) -> Boolean)?) {
        volumePageTurnHandler = handler
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
