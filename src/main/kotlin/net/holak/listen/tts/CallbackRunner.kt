package net.holak.listen.tts

import javafx.application.Platform
import javax.swing.SwingUtilities

/** Interface with a wrapper function that makes sure that the callback gets called on the UI thread in UI applications. */
interface CallbackRunner {
    fun run(r: () -> Unit)
}

class JavaFxCallbackRunner : CallbackRunner {
    override fun run(r: () -> Unit) = Platform.runLater(r)
}

class SwingCallbackRunner : CallbackRunner {
    override fun run(r: () -> Unit) = SwingUtilities.invokeLater(r)
}

/** For applications that do not have an event loop, just runs the callback on the TTS-spawned worker thread. */
class WorkerThreadCallbackRunner: CallbackRunner {
    override fun run(r: () -> Unit) = r()
}
