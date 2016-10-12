package net.holak.listen.script

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.io.IOContext
import com.fasterxml.jackson.core.json.UTF8StreamJsonParser
import com.fasterxml.jackson.core.sym.BytesToNameCanonicalizer
import com.fasterxml.jackson.core.util.BufferRecycler
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import net.dean.jraw.JrawUtils
import net.dean.jraw.models.Comment
import net.dean.jraw.models.CommentNode
import net.dean.jraw.models.CommentSort
import net.holak.listen.config.Preset
import net.holak.listen.reddit.Reddit
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class ThreadBuilder {

    fun createComment(): CommentNode {
        val jsonText = "{}"
        val comment = Comment(JrawUtils.fromString(jsonText))
        return CommentNode("", mutableListOf(comment), null, CommentSort.TOP)
    }
}

class TreeTraversalTests {

    @Test fun hello() {
        val comment = ThreadBuilder().createComment()
        val script = DebuggingInspiredReadScript(Preset("Hello", subreddits = listOf("worldnews")), Reddit())
    }

}