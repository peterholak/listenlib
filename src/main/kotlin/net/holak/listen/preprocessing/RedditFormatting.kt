package net.holak.listen.preprocessing

class RedditFormatting : CommentTransformer {
    override fun transform(comment: String): String {
        return comment.replace("&gt;", "Quote: ") // TODO: only on the beginning of a line
    }
}