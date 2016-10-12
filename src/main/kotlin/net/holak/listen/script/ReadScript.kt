package net.holak.listen.script

import net.dean.jraw.models.*
import net.dean.jraw.models.meta.Model
import net.dean.jraw.paginators.SubredditPaginator
import net.holak.listen.config.Preset
import net.holak.listen.history.History
import net.holak.listen.reddit.Reddit
import kotlin.concurrent.thread

interface ReadScript : Iterator<String> {
    fun stepInto(): String
    fun stepOut(): String
}

class DefaultScript(
        val preset: Preset,
        val reddit: Reddit,
        val history: History? = null,
        val maxLength: Int = Int.MAX_VALUE)
: ReadScript {

    var atEnd = false
    val subredditIterator = preset.subreddits.iterator()
    lateinit var currentSubreddit: String

    lateinit var currentThreadLister: SubredditPaginator
    lateinit var currentThreadListing: Iterator<Submission>
    lateinit var currentThread: Submission
    lateinit var currentComments: Iterator<CommentNode>
    var currentComment: CommentNode? = null
    fun currentDepth() = currentComment?.depth ?: 0

    var failsafeCounter = 0

    var currentType: Model.Kind = Model.Kind.NONE
        private set

    val threadsPerSub = preset.threadsPerSubreddit
    val commentsPerThread = preset.commentsPerThread
    var threadCursor = 0
    var commentCursor = 0

    override fun hasNext(): Boolean = !atEnd
    override fun next(): String {

        failsafeCounter++
        if (failsafeCounter >= 500) {
            System.err.println("failsafe triggered")
            atEnd = true
        }

        when (currentType) {
            Model.Kind.NONE -> return topLevelNext()
            Model.Kind.SUBREDDIT -> return subredditNext()
            Model.Kind.LINK -> return threadNext()
            Model.Kind.COMMENT -> return commentNext()
            else -> throw Exception("Unsupported reddit thing type: " + currentType.name)
        }

    }

    override fun stepInto(): String {
        when (currentType) {
            Model.Kind.COMMENT -> return commentStepInto()
            else -> return next()
        }
    }

    override fun stepOut(): String {
        when (currentType) {
            else -> throw Exception("Unsupported reddit thing type: " + currentType.name)
        }
    }

    private fun topLevelNext(): String {
        currentType = Model.Kind.SUBREDDIT
        return "Starting script."
    }

    private fun subredditNext(): String {
        if (!subredditIterator.hasNext()) {
            atEnd = true
            return "Script finished."
        }

        currentSubreddit = subredditIterator.next()
        currentType = Model.Kind.LINK
        threadCursor = 0

        currentThreadLister = SubredditPaginator(reddit.reddit, currentSubreddit)
        if (!currentThreadLister.hasNext()) {
            return "Empty subreddit: " + currentSubreddit
        }

        currentThreadListing = currentThreadLister.next().iterator()

        return "Subreddit: " + currentSubreddit
    }

    private fun threadNext(): String {
        if (!currentThreadListing.hasNext() || threadCursor >= threadsPerSub) {
            currentType = Model.Kind.SUBREDDIT
            return "Subreddit finished: " + currentSubreddit
        }

        commentCursor = 0

        var nextId = currentThreadListing.next().id
        while (history?.wasListened(nextId) ?: false) {
            nextId = currentThreadListing.next().id
        }

        currentThread = reddit.reddit.getSubmission(nextId)
        threadCursor++
        currentComments = currentThread.comments.iterator()
        currentType = Model.Kind.COMMENT

        history?.setAsListened(nextId)

        return readPost(currentThread)
    }

    private fun readPost(thread: Submission): String {
        if (thread.isSelfPost && thread.selftext.isNullOrEmpty()) {
            return thread.title
        }

        if (thread.isSelfPost) {
            val fullText = thread.title + "\n\n" + thread.selftext
            if (fullText.length > maxLength) {
                return thread.title + "\n\n" + "Post text too long." // TODO: split into multiple utterances
            }
            return fullText
        }

        return thread.title // TODO: domain link? but not on pic subreddits? todo etc.
    }

    private fun commentNext(): String {
        if (!currentComments.hasNext() || commentCursor >= commentsPerThread) {
            return commentsUp()
        }

        currentComment = currentComments.next();
        commentCursor++

        return readComment(currentComment!!)
    }

    private fun commentStepInto(): String {
        if (currentComment?.children?.isEmpty() ?: true) {
            return commentNext()
        }

        currentComments = currentComment!!.children.iterator()
        return commentNext()
    }

    private fun commentsUp(): String {
        currentType = Model.Kind.LINK
        return "Thread finished.";
    }

    private fun readComment(comment: CommentNode): String {
        if (comment.comment.body.length > maxLength) {
            return "Comment text too long." // TODO
        }
        return comment.comment.body
    }
}

class DebuggingInspiredReadScript(val preset: Preset, val reddit: Reddit) : ReadScript {

    class Cursor<T>(val items: Iterable<T>, val parent: Cursor<*>? = null): Iterator<T> {

        val iterator = items.iterator()
        override fun hasNext() = iterator.hasNext()
        override fun next() = iterator.next()

        fun <U> spawnChildCursor(data: Iterable<U>): Cursor<U> {
            return Cursor<U>(data, this)
        }
    }

    val subredditCursor = Cursor<String>(preset.subreddits)
    var threadCursor: Cursor<Submission>? = null
    val commentCursorStack = mutableListOf<Cursor<CommentNode>>()

    private fun currentCursor(): Cursor<*> =
        when {
            threadCursor == null -> subredditCursor
            commentCursorStack.isEmpty() -> threadCursor!!
            else -> commentCursorStack.last()
        }

    val currentType: Model.Kind
        get() = when {
            threadCursor == null -> Model.Kind.SUBREDDIT
            commentCursorStack.isEmpty() -> Model.Kind.LINK
            else -> Model.Kind.COMMENT
        }

    fun currentDepth() = commentCursorStack.size

    override fun stepInto(): String {
        val cursor = currentCursor()

        if (cursor === subredditCursor) {
            if (!cursor.hasNext()) return "At end."

            val subreddit = cursor.next()
            // TODO: this will only work for one page
            threadCursor = cursor.spawnChildCursor(SubredditPaginator(reddit.reddit, subreddit).next())
            return "Subreddit: ${subreddit}"
        }

        if (cursor === threadCursor) {
            if (!cursor.hasNext()) {
                commentCursorStack.clear()
                threadCursor = null
                return "Subreddit finished"
            }

            val thread = cursor.next()
            if (thread.commentCount > 0) {
                val threadWithComments = reddit.reddit.getSubmission(thread.id)
                commentCursorStack.add(cursor.spawnChildCursor(threadWithComments.comments))
            }
            return thread.title
        }

        val commentCursor = commentCursorStack.last()
        if (!commentCursor.hasNext()) {
            commentCursorStack.removeAt(commentCursorStack.lastIndex)
            if (commentCursorStack.isEmpty()) {
                return "Thread finished"
            }
            return stepInto()
        }

        val comment = commentCursor.next()
        if (comment.children.isNotEmpty()) {
            commentCursorStack.add(cursor.spawnChildCursor(comment.children))
        }
        return comment.comment.body
    }

    override fun stepOut(): String {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasNext(): Boolean = true

    override fun next(): String = stepInto()

}
