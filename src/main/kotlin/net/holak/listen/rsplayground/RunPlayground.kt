@file:Suppress("unused")

package net.holak.listen.rsplayground

// Idea: maybe have a way to use nested scripts, so that it is easier to just read a specific thread,
// or saved threads etc.
//
// There is not much functionality to reuse anyway, so just nesting SubredditScript, ThreadScript and CommentScript
// could work very nicely.

abstract class StackBasedScript(topEntry: Entry) : Iterator<String> {

    val stack = mutableListOf(topEntry)

    override fun hasNext() = stack.isNotEmpty()

    override fun next(): String {
        val item = stack.last()
        stack.removeAt(stack.lastIndex)

        fillStack(item)

        return "${item.summary()} (${item.depth})"
    }

    abstract fun fillStack(item: Entry)
}

class BasicDepthFirstScript(topEntry: Entry) : StackBasedScript(topEntry) {
    override fun fillStack(item: Entry) {
        stack.addAll(item.children.reversed())
    }
}

class ScriptWithBreadthLimit(topEntry: Entry, val breadthLimit: Int) : StackBasedScript(topEntry) {

    override fun fillStack(item: Entry) {
        val children = item.children
        stack.addAll(
                children
                        .slice(0..Math.min(breadthLimit, children.size) - 1)
                        .reversed()
        )
    }
}

class ScriptWithDepthLimit(topEntry: Entry, val depthLimit: Int) : StackBasedScript(topEntry) {

    override fun fillStack(item: Entry) {
        if (item.depth < depthLimit) {
            stack.addAll(item.children.reversed())
        }
    }
}

class ScriptWithBothLimits(topEntry: Entry, val depthLimit: Int, val breadthLimit: Int) : StackBasedScript(topEntry) {

    override fun fillStack(item: Entry) {
        if (item.depth >= depthLimit) { return }

        val children = item.children
        stack.addAll(
                children
                        .slice(0..Math.min(breadthLimit, children.size) - 1)
                        .reversed()
        )
    }

}

class ScriptWithMapLimits(topEntry: Entry, val breadthLimitForDepth: Map<Int, Int>) : StackBasedScript(topEntry) {

    val depthLimit = breadthLimitForDepth.keys.max() ?: 0

    override fun fillStack(item: Entry) {
        if (item.depth > depthLimit) { return }

        val children = item.children
        stack.addAll(
                children
                        .slice(0..Math.min(breadthLimitForDepth[item.depth] ?: 0, children.size) - 1)
                        .reversed()
        )
    }
}

fun main(args: Array<String>) {
//    ScriptWithBreadthLimit(testData, 3).forEach { println(">" + it) }
//    ScriptWithDepthLimit(testData, 4).forEach { println(">" + it) }
//    ScriptWithBothLimits(testData, 4, 2).forEach { println("> $it") }
    ScriptWithMapLimits(testData, mapOf(
            0 to 2,
            1 to 5,
            2 to 5
    )).forEach { println("> $it") }
}
