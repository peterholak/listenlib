package net.holak.listen.rsplayground

val testData = subreddit {

    title = "Subreddit 1"

    post {
        title = "Post A"

        comment {
            text = "A1"
            comment { text = "A1.1" }
        }

        comment {
            text = "A2"
        }

        comment {
            text = "A3"
            comment {
                text = "A3.1"
                comment {
                    text = "A3.1.1"
                    comment { text = "A3.1.1.1" }
                }
                comment { text = "A3.1.2" }
            }
        }
    }

    post {
        title = "Post B"

        val sequence = listOf(3, 3, 2, 1, 2, 1)

        fun addTo(item: Entry, parentText: String, sequenceIndex: Int = 0) {
            if (sequenceIndex > sequence.lastIndex) { return }

            for(i in 1..sequence[sequenceIndex]) {
                val newComment = Comment()
                newComment.text = parentText + i
                item.children.add(newComment)
                addTo(item = newComment, parentText = newComment.text + ".", sequenceIndex = sequenceIndex + 1)
            }
        }

        addTo(item = this, parentText = "B")

    }

    post {
        title = "Post C"
    }

    post {
        title = "Post D"
        var item: Entry = this
        for (i in 1..10) {
            val newComment = Comment()
            newComment.text = "D" + "1.".repeat(i-1) + "1"
            item.children.add(newComment)
            item = newComment
        }
    }

    post {
        title = "Post E"
    }

}
