package net.holak.listen.ui

import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import net.dean.jraw.models.meta.Model
import net.holak.listen.config.Preset
import net.holak.listen.history.DesktopSqliteHistory
import net.holak.listen.preprocessing.*
import net.holak.listen.reddit.Reddit
import net.holak.listen.script.DebuggingInspiredReadScript
import net.holak.listen.script.DefaultScript
import net.holak.listen.tts.DefaultVoice
import net.holak.listen.tts.MaryTextToSpeech
import tornadofx.View
import tornadofx.add
import tornadofx.useMaxWidth
import tornadofx.vboxConstraints

class DebugWindow : View() {
    override val root = VBox()
    val textOutput = TextArea()
    val nextButton = Button("Next")
    val stepIntoButton = Button("Step into")
    val stepOutButton = Button("Step out")
    val speakCurrentButton = Button("Speak current")

    data class IndentedText(val text: String, val kind: Model.Kind, val depth: Int = 0)

    val script by lazy {
        DebuggingInspiredReadScript(
                Preset("Hello", DefaultVoice, listOf("kotlin")),
                Reddit()
        )
    }
    val tts by lazy{ MaryTextToSpeech() }
    var current = ""

    val transformers = listOf(
            RedditFormatting(),
            GenderAgeBrackets(),
            RemoveExtraSlashForSubreddits(),
            LinksSummary(),
            CommonAbbreviations()
    )

    init {
        primaryStage.setOnCloseRequest { onClose() }
        with(root) {
            title = "Listen debug UI"
            padding = Insets(10.0)
            spacing = 5.0

            add(nextButton)
            with(nextButton) {
                useMaxWidth = true
                setOnAction { nextClicked() }
            }

            add(stepIntoButton)
            with(stepIntoButton) {
                useMaxWidth = true
                setOnAction { stepIntoClicked() }
            }

            add(stepOutButton)
            with(stepOutButton) {
                useMaxWidth = true
                setOnAction { stepOutClicked() }
            }

            add(speakCurrentButton)
            with(speakCurrentButton) {
                useMaxWidth = true
                setOnAction { speakCurrentClicked() }
            }

            add(textOutput)
            with(textOutput) {
                useMaxWidth = true
                minWidth = 500.0
                minHeight = 200.0
                vboxConstraints {
                    vGrow = Priority.ALWAYS
                }
            }
        }
    }

    fun onClose() {
        tts.shutdown()
    }

    fun nextClicked() {
        nextButton.disableProperty().set(true)

        runAsync {
            if (!script.hasNext()) {
                return@runAsync IndentedText("At end.", Model.Kind.NONE)
            }
            val type = script.currentType
            val text = transformers.fold(script.next(), { str, transformer -> transformer.transform(str) })
            val depth = script.currentDepth()
            IndentedText(text, type, depth)
        } ui {
            current = it.text
            printText(it.text, it.kind, it.depth)
            nextButton.disableProperty().set(false)
        }
    }

    fun stepIntoClicked() {
        runAsync {
            script.stepInto()
            val type = script.currentType
            val depth = script.currentDepth()
            IndentedText(script.next(), type, depth)
        } ui {
            printText(it.text, it.kind, it.depth)
        }
    }

    fun stepOutClicked() {
        runAsync {
            script.stepOut()
            val type = script.currentType
            Pair(script.next(), type)
        } ui {
            printText(it.first, it.second)
        }
    }

    private fun printText(text: String, type: Model.Kind, commentDepth: Int = 0) {
        val indentSize = when (type) {
            Model.Kind.SUBREDDIT -> 4
            Model.Kind.LINK -> 8
            Model.Kind.COMMENT -> 12 + 4*commentDepth
            else -> 0
        }
        val indent = " ".repeat(indentSize)
        textOutput.appendText(text.replaceIndent(indent))
        textOutput.appendText("\n")
        textOutput.appendText(indent + "---\n")
    }

    fun speakCurrentClicked() {
        speakCurrentButton.disableProperty().set(true)
        tts.say(
                current,
                { speakCurrentButton.disableProperty().set(false) }
        )
    }
}
