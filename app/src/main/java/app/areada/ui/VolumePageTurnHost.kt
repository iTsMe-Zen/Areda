package app.areada.ui

import android.os.Handler

interface VolumePageTurnHost {
    fun setVolumePageTurnHandler(handler: ((volumeUp: Boolean) -> Boolean)?)
}

