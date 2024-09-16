package me.kavyamurthy.hestia.db

import Conversation
import Message
import kotlinx.datetime.Clock
import me.kavyamurthy.hestia.config
import me.kavyamurthy.hestia.hashPassword
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction

object Users : Table("hestia.users") {
    val id = integer("id").autoIncrement()
    val userName = text("user_name").uniqueIndex()
    val displayName = text("display_name")

    override val primaryKey = PrimaryKey(id)

    fun createUser(userName: String, displayName: String, email: String, password: String) = transaction {
        val userId = Users.insert {
            it[Users.userName] = userName
            it[Users.displayName] = displayName
        } get Users.id
        Login.insert {
            it[Login.email] = email
            it[Login.userId] = userId
            it[Login.passwordHash] = hashPassword(password)
        }
    }

    fun userExists(userName: String) = transaction {
        Users.select(Users.userName)
            .where { Users.userName eq userName }
            .empty()
            .not()
    }
}

object Login : Table("hestia.login") {
    val email = text("email")
    val userId = integer("user_id") references Users.id
    val passwordHash = integer("password_hash")

    override val primaryKey = PrimaryKey(email)

    fun login(email: String, passwordHash: Int) = try {
        transaction {
            Login.innerJoin(Users)
                .select(Users.id, Users.userName, Users.displayName)
                .where { (Login.email eq email) and (Login.passwordHash eq passwordHash) }
                .map { User(it[Users.id], it[Users.userName], it[Users.displayName]) }
                .singleOrNull()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    fun loginExists(email: String) = transaction {
        Login.select(Login.email)
            .where { Login.email eq email }
            .empty()
            .not()
    }
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

        cm1.join(cm2, JoinType.INNER, cm1[cm.convId], cm2[cm.convId])
            .join(Users, JoinType.INNER, cm2[cm.userId], Users.id)
            .select(cm1[cm.convId], cm2[cm.userId], Users.displayName)
            .where { (cm1[cm.userId] eq userId) and (cm2[cm.userId] neq userId) }
            .toList()
            .map { Conversation(it[Users.displayName], it[cm1[cm.convId]]) }
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
        DirectMessages.innerJoin(Users)
            .select(fromUser, Users.displayName, DirectMessages.convId, message, timestamp)
            .where { DirectMessages.convId eq convId }
            .orderBy(timestamp)
            .toList()
            .map { Message(it[fromUser], it[Users.displayName], it[DirectMessages.convId], it[message], it[timestamp].epochSeconds) }
    }

    fun add(userId: Int, msg: Message) = transaction {
        addMessage(userId, msg.convId, msg.body)
    }
}

fun initializeDB() = transaction {
    SchemaUtils.createSchema(Schema("hestia"))
    SchemaUtils.create(Users, Login, DirectMessages, Conversations, ConversationMembers)
}

//fun addUser(email: String, password: String, firstName: String, lastName: String): Int = transaction {
//    Users.insert {
//        it[Users.email] = email
//        it[passwordHash] = hashPassword(password)
//        it[Users.firstName] = firstName
//        it[Users.lastName] = lastName
//    } get Users.id
//}

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
    initializeDB()
}