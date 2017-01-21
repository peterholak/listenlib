package net.holak.listen.rsplayground

abstract class Entry {
    val children = mutableListOf<Entry>()
    abstract fun summary(): String

    fun dump(indent: Int = 0) {
        print(" ".repeat(indent * 4) + summary())
        println()

        children.forEach { it.dump(indent + 1) }
    }
}

class Subreddit : Entry() {

    var title = "<empty>"
    override fun summary() = "Subreddit " + title
    fun post(init: Post.() -> Unit): Post {
        val p = Post()
        p.init()
        children.add(p)
        return p
    }

}

class Post : Entry() {

    var title = "<empty>"
    var selfPostText = ""
    override fun summary() = title
    fun isLink() = selfPostText.isEmpty()

    fun comment(init: Comment.() -> Unit): Comment {
        val c = Comment()
        c.init()
        children.add(c)
        return c
    }

}

class Comment : Entry() {

    var text = "<empty>"
    override fun summary() = text

    fun comment(init: Comment.() -> Unit): Comment {
        val c = Comment()
        c.init()
        children.add(c)
        return c
    }

}

fun subreddit(init: Subreddit.() -> Unit): Subreddit {
    return Subreddit().apply { init() }
}
