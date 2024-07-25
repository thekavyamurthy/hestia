package me.kavyamurthy.hestia.db

import Conversation
import Message
import kotlinx.datetime.Clock
import me.kavyamurthy.hestia.config
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

object Conversations : Table("hestia.conversations") {
    val id = long("id").autoIncrement()
    val name = text("name").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val createdBy = integer("created_by") references Users.id
// TODO: conversation type

    override val primaryKey = PrimaryKey(id)
    fun getConvList(userId: Int) = transaction {
        val cm = ConversationMembers
        val cm1 = cm.alias("cm1")
        val cm2 = cm.alias("cm2")
        val convName = coalesce(Users.displayName, Users.firstName)

        cm1.join(cm2, JoinType.INNER, cm1[cm.convId], cm2[cm.convId])
            .join(Users, JoinType.INNER, cm2[cm.userId], Users.id)
            .select(cm1[cm.convId], cm2[cm.userId], convName)
            .where { (cm1[cm.userId] eq userId) and (cm2[cm.userId] neq userId) }
            .toList()
            .map { Conversation(it[convName], it[cm1[cm.convId]]) }
    }
}

object ConversationMembers : Table("hestia.conversation_members") {
    val convId = long("conv_id") references Conversations.id
    val userId = integer("user_id") references Users.id

    fun getMembers(convId: Long) = transaction {
        ConversationMembers.select(userId).where {
            ConversationMembers.convId eq convId
        }.toList().map { it[userId].toInt() }
    }
}

object DirectMessages : Table("hestia.direct_messages") {
    val id = long("id").autoIncrement()
    val convId = long("conv_id") references Conversations.id
    val fromUser = integer("from_user") references Users.id
    val message = text("message")
    val timestamp = timestamp("ts").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)

    fun getMessages(convId: Long) = transaction {
        val fromName = coalesce(Users.displayName, Users.firstName)
        DirectMessages.innerJoin(Users)
            .select(fromUser, fromName, DirectMessages.convId, message, timestamp)
            .where { DirectMessages.convId eq convId }
            .toList()
            .map { Message(it[fromUser], it[fromName], it[DirectMessages.convId], it[message], it[timestamp].epochSeconds) }
    }

    fun add(userId: Int, msg: Message) = transaction {
        addMessage(userId, msg.convId, msg.body)
    }
}

fun initializeDB() = transaction {
    SchemaUtils.createSchema(Schema("hestia"))
    SchemaUtils.create(Users, DirectMessages, Conversations, ConversationMembers)
    val kavya = addUser("thekavyamurthy@gmail.com", "kavya", "Kavya", "Murthy")
    val vikas = addUser("vikasmurthy@hotmail.com", "vikas", "Vikas", "Murthy")
    val shilpa = addUser("shilpaprasad@gmail.com", "shilpa", "Shilpa", "Prasad")

    val kv = addConversation(listOf(vikas, kavya))
    val sk = addConversation(listOf(kavya, shilpa))

    addMessage(kavya, kv, "Hi Appa")
    addMessage(vikas, kv, "Hi Kavyu")
    addMessage(shilpa, sk, "Hi Laddoo")
}

fun addUser(email: String, password: String, firstName: String, lastName: String): Int {
    return Users.insert {
        it[Users.email] = email
        it[passwordHash] = hashPassword(password)
        it[Users.firstName] = firstName
        it[Users.lastName] = lastName
    } get Users.id
}

fun addConversation(users: List<Int>, name: String? = null): Long {
    val convId = Conversations.insert {
        it[Conversations.name] = name
        it[createdAt] = Clock.System.now()
        it[createdBy] = 1
    } get Conversations.id

    users.forEach {userId ->
        ConversationMembers.insert {
            it[ConversationMembers.convId] = convId
            it[ConversationMembers.userId] = userId
        }
    }

    return convId
}

fun addMessage(from: Int, conv: Long, msg: String) {
    DirectMessages.insert {
        it[convId] = conv
        it[fromUser] = from
        it[message] = msg
        it[timestamp] = Clock.System.now()
    }
}

fun main() {
    Database.connect(config.db.url, user = config.db.username, password = config.db.password)

    println(Conversations.getConvList(1))

//    initializeDB()
}