package net.holak.listen.preprocessing

/**
 * Puts the words "quote unquote" in front of short bits of text in quotes -
 * text that is not an entire paragraph, but appears in the middle of a sentence.
 */
class QuoteUnquote : CommentTransformer {
    override fun transform(comment: String): String {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}