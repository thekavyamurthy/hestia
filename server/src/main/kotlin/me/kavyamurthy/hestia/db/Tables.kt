package me.kavyamurthy.hestia.db

import Conversation
import Message
import kotlinx.datetime.Clock
import me.kavyamurthy.hestia.DB_PASSWORD
import me.kavyamurthy.hestia.DB_URL
import me.kavyamurthy.hestia.DB_USERNAME
import me.kavyamurthy.hestia.hashPassword
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.coalesce
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.union

object Users : Table("hestia.users") {
    val id = integer("id").autoIncrement()
    val email = text("email_id")
    val passwordHash = integer("password_hash")
    val firstName = text("first_name")
    val lastName = text("last_name")
    val displayName = text("display_name").nullable()

    override val primaryKey = PrimaryKey(id)

    fun login(email: String, passwordHash: Int) = try {
        transaction {
            Users.selectAll().where {
                (Users.email eq email) and (Users.passwordHash eq passwordHash)
            }.singleOrNull()?.toUser()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private fun ResultRow.toUser() = User(
        this[id],
        this[email],
        this[firstName],
        this[lastName],
        this[displayName]
    )
}

object DirectMessages : Table("hestia.direct_messages") {
    val id = long("id").autoIncrement()
    val fromUser = integer("from_user") references Users.id
    val toUser = integer("to_user") references Users.id
    val message = text("message")
    val timestamp = timestamp("ts").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(DirectMessages.id)

    fun getConvList(userId: Int) = transaction {
        val userName = coalesce(Users.displayName, Users.firstName)
        val conv = DirectMessages.select(toUser)
            .where { fromUser eq userId }
            .union(
                DirectMessages.select(fromUser)
                    .where { toUser eq userId }
            )
            .alias("conv")
        conv.join(Users, JoinType.INNER, conv.columns[0], Users.id)
            .select(userName, Users.id)
            .toList()
            .map { Conversation(it[userName], it[Users.id]) }
    }

    fun getConversation(userId: Int, otherUserId: Int) = transaction {
        val msgQuery = DirectMessages.selectAll().where {
                ((fromUser eq userId) and (toUser eq otherUserId))
            }.union(
                DirectMessages.selectAll().where {
                    ((toUser eq userId) and (fromUser eq otherUserId))
                }
            ).alias("messages")

        val from = Users.alias("from")
        val to = Users.alias("to")
        val fromName = coalesce(from[Users.displayName], from[Users.firstName])
        val toName = coalesce(to[Users.displayName], to[Users.firstName])
        msgQuery.join(from, JoinType.INNER, msgQuery[fromUser], from[Users.id])
            .join(to, JoinType.INNER, msgQuery[toUser], to[Users.id])
            .select(from[Users.id], fromName, to[Users.id], toName, msgQuery[message], msgQuery[timestamp])
            .orderBy(msgQuery[timestamp])
            .toList()
            .map { Message(it[from[Users.id]], it[fromName], it[to[Users.id]], it[toName], it[msgQuery[message]], it[msgQuery[timestamp]].epochSeconds) }
    }

    fun add(userId: Int, msg: Message) = transaction {
        addMessage(userId, msg.toId, msg.body)
    }
}

fun initializeDB() = transaction {
    SchemaUtils.createSchema(Schema("hestia"))
    SchemaUtils.create(Users, DirectMessages)
    val kavya = addUser("thekavyamurthy@gmail.com", "kavya", "Kavya", "Murthy")
    val vikas = addUser("vikasmurthy@hotmail.com", "vikas", "Vikas", "Murthy")
    val shilpa = addUser("shilpaprasad@gmail.com", "shilpa", "Shilpa", "Prasad")

    addMessage(kavya, vikas, "Hi Appa")
    addMessage(vikas, kavya, "Hi Kavyu")
    addMessage(shilpa, kavya, "Hi Laddoo")
}

fun addUser(email: String, password: String, firstName: String, lastName: String): Int {
    return Users.insert {
        it[Users.email] = email
        it[passwordHash] = hashPassword(password)
        it[Users.firstName] = firstName
        it[Users.lastName] = lastName
    } get Users.id
}

fun addMessage(from: Int, to: Int, msg: String) {
    DirectMessages.insert {
        it[fromUser] = from
        it[toUser] = to
        it[message] = msg
        it[timestamp] = Clock.System.now()
    }
}

fun main() {
    Database.connect(DB_URL, user = DB_USERNAME, password = DB_PASSWORD)

    initializeDB()
}