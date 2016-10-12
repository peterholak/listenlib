package net.holak.listen.tts

import javafx.application.Platform
import marytts.LocalMaryInterface
import marytts.util.data.audio.AudioPlayer

class MaryTextToSpeech(val callbackRunner: CallbackRunner = JavaFxCallbackRunner()) : TextToSpeech {

    val mary by lazy { LocalMaryInterface() }
    private val DefaultMaryVoice = VoiceInfo("Mary", "Default Mary voice", "en")
    private var playingAudio: AudioPlayer? = null
    private var speaking = false

    override fun getMaxSpeechInputLength(): Int = Int.MAX_VALUE

    override fun enumerateVoices(callback: (List<VoiceInfo>) -> Unit) {
        callback(listOf(DefaultMaryVoice))
    }

    override fun say(thing: String, finishedCallback: ((TextToSpeech) -> Unit)?) {
        Thread {
            if (!thing.isEmpty()) {
                // TODO: concurrent speaking of multiple things
                val audio = mary.generateAudio(thing)
                playingAudio = AudioPlayer(audio)
                playingAudio?.start()
                speaking = true
                playingAudio?.join()
                speaking = false
            }

            callbackRunner.run {
                if (finishedCallback != null) {
                    finishedCallback(this)
                }
            }
        }.start()
    }

    override fun stop() {
        playingAudio?.cancel()
    }

    override fun isSpeaking(): Boolean = speaking

    override fun shutdown() {
        stop()
    }

}