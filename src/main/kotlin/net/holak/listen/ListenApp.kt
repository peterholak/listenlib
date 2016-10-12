package net.holak.listen

import net.holak.listen.ui.DebugWindow
import tornadofx.App

class ListenApp : App() {
    override val primaryView = DebugWindow::class
}
