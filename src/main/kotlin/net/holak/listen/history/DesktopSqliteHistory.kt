package net.holak.listen.history

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.sql.DriverManager

object HistoryTable : Table() {
    val redditId = varchar("id", 10).primaryKey()
}

class DesktopSqliteHistory : History {

    init {
        Database.registerDialect(SqliteDialect())
        Database.connect("jdbc:sqlite:./var/history.db", driver = "org.sqlite.JDBC")
        db { exec(HistoryTable.createStatement()) }
    }
    private fun<T> db(statement: Transaction.() -> T) = transaction(Connection.TRANSACTION_SERIALIZABLE, 3, statement)

    override fun wasListened(id: String): Boolean {
        return db { !HistoryTable.select { HistoryTable.redditId.eq(id) }.empty() }
    }

    override fun setAsListened(id: String) {
        db { HistoryTable.insertIgnore { it[redditId] = id } }
    }

    override fun resetAll() {
        db { HistoryTable.deleteAll() }
    }

    override fun listListened(): List<String> {
        return db { HistoryTable.selectAll().map { it[HistoryTable.redditId] } }
    }
}