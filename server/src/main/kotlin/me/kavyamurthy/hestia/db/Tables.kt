package me.kavyamurthy.hestia.db

import Conversation
import ConversationType
import Group
import Message
import User
import kotlinx.datetime.Clock
import me.kavyamurthy.hestia.config
import me.kavyamurthy.hestia.hashPassword
import org.jetbrains.exposed.sql.CustomStringFunction
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.EqOp
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("HestiaTables")

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

    fun lookupUser(userName: String): User? = transaction {
        Users.select(Users.id, Users.userName, Users.displayName)
            .where { Users.userName eq userName }
            .map { User(it[Users.id], it[Users.userName], it[Users.displayName]) }
            .singleOrNull()
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

object Groups : Table("hestia.groups") {
    val id = long("id").autoIncrement()
    val name = text("name")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val createdBy = integer("created_by") references Users.id
// TODO: conversation type

    override val primaryKey = PrimaryKey(id)

    fun getGroupList(userId: Int) = transaction {
        Groups.innerJoin(GroupMembers).select(Groups.id, name).where {
            GroupMembers.userId eq userId
        }.toList().map { Group(it[Groups.id], it[name]) }
    }

    fun getMembers(groupId: Long) = transaction {
        GroupMembers.innerJoin(Users).select(Users.id, Users.userName, Users.displayName).where {
            GroupMembers.groupId eq groupId
        }.toList().map { User(it[Users.id], it[Users.userName], it[Users.displayName]) }
    }

    fun addMember(groupId: Long, userId: Int) = transaction {
        GroupMembers.insert {
            it[GroupMembers.groupId] = groupId
            it[GroupMembers.userId] = userId
        }
    }

    //this is all wrong basically idk do something about it
    fun newConv(createdBy: Int, otherUser: String, name: String): Conversation = transaction {
        if (Users.userExists(otherUser)) {
            val otherUserId = Users.select(Users.id).where { Users.userName eq otherUser }
            val convId = Groups.insert {
                it[Groups.name] = name
                it[Groups.createdAt] = Clock.System.now()
                it[Groups.createdBy] = createdBy
            } get (Groups.id)

            GroupMembers.insert {
                it[GroupMembers.groupId] = convId
                it[GroupMembers.userId] = otherUserId
            }
            GroupMembers.insert {
                it[GroupMembers.groupId] = convId
                it[GroupMembers.userId] = createdBy
            }

            val otherDisplayName = Users.select(Users.displayName).where { Users.userName eq otherUser }.single().toString()
            Conversation(otherDisplayName, convId, ConversationType.SPACE)
        } else {
            Conversation("", 0L, ConversationType.SPACE)
        }
    }
}

object GroupMembers : Table("hestia.group_members") {
    val groupId = long("group_id") references Groups.id
    val userId = integer("user_id") references Users.id
}

object Messages : Table("hestia.messages") {
    val id = long("id").autoIncrement()
    val convId = long("conv_id") references Spaces.id
    val fromUser = integer("from_user") references Users.id
    val message = text("message")
    val timestamp = timestamp("ts").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)

    fun getMessages(spaceId: Long) = transaction {
        Messages.innerJoin(Users)
            .select(fromUser, Users.displayName, Messages.convId, message, timestamp)
            .where { Messages.convId eq spaceId }
            .orderBy(timestamp)
            .toList()
            .map { Message(ConversationType.SPACE, it[Messages.convId], it[timestamp].epochSeconds, it[fromUser], it[Users.displayName], it[message]) }
    }

    fun add(fromId: Int, msg: Message) = transaction {
        Messages.insert {
            it[convId] = msg.convId
            it[fromUser] = fromId
            it[message] = msg.body
            it[timestamp] = Clock.System.now()
        }
    }
}

object Spaces : Table("hestia.spaces") {
    val id = long("id").autoIncrement()
    val name = text("name")
    val groupId = long("groupId").references(Groups.id)

    override val primaryKey = PrimaryKey(id)

    fun getSpaces(userId: Int) = transaction {
        Spaces.innerJoin(Groups).innerJoin(GroupMembers).select(Spaces.id, name, Groups.name).where {
            GroupMembers.userId eq userId
        }.toList().map { Conversation(it[name] + "(" + it[Groups.name] + ")", it[Spaces.id], ConversationType.SPACE) }
    }

    fun getMembers(spaceId: Long) = transaction {
        Spaces.innerJoin(Groups).innerJoin(GroupMembers).select(GroupMembers.userId).where {
            Spaces.id eq spaceId
        }.toList().map { it[GroupMembers.userId].toInt() }
    }
}

object DirectConversations : Table("hestia.direct_conversations") {
    val id = long("id").autoIncrement()
    val userIds = array<Int>("user_ids").uniqueIndex()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val createdBy = integer("created_by") references Users.id

    override val primaryKey = PrimaryKey(DirectConversations.id)

    fun getConvList(userId: Int): List<Conversation> = transaction {
        logger.info("getting conversation list")
        DirectConversations.join(Users, JoinType.INNER) { Users.id eqAny userIds }
            .select(DirectConversations.id, Users.displayName)
            .where { (userId equalsAny userIds) and (Users.id neq userId) }
            .toList()
            .map { Conversation(it[Users.displayName], it[DirectConversations.id], ConversationType.DIRECT) }
    }

    fun getOrAddConv(from: Int, to: Int) = transaction {
        logger.info("getting or adding conversation")
        select(DirectConversations.id).where {
            userIds eq listOf(from, to).sorted()
        }.singleOrNull()
            ?.get(DirectConversations.id)
            ?.toLong() ?: (DirectConversations.insert {
                it[userIds] = listOf(from, to).sorted()
                it[createdAt] = Clock.System.now()
                it[createdBy] = from
        } get DirectConversations.id)
    }

    fun getMembers(convId: Long) = transaction {
        select(userIds).where {
            DirectConversations.id eq convId
        }.flatMap { it[userIds] }
    }
}

object DirectMessages : Table("hestia.direct_messages") {
    val id = long("id").autoIncrement()
    val convId = long("conv_id") references DirectConversations.id
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
            .map { Message(ConversationType.DIRECT, it[DirectMessages.convId], it[timestamp].epochSeconds, it[fromUser], it[Users.displayName], it[message]) }
    }

    fun add(fromId: Int, msg: Message) = transaction {
        addMessage(fromId, msg.convId, msg.body)
    }
}



fun initializeDB() = transaction {
    SchemaUtils.createSchema(Schema("hestia"))
    SchemaUtils.create(Users, Login, DirectMessages, Groups, GroupMembers, Messages, DirectConversations, Spaces)
}

//fun addUser(email: String, password: String, firstName: String, lastName: String): Int = transaction {
//    Users.insert {
//        it[Users.email] = email
//        it[passwordHash] = hashPassword(password)
//        it[Users.firstName] = firstName
//        it[Users.lastName] = lastName
//    } get Users.id
//}

//fun addConversation(users: List<Int>, name: String? = null): Long {
//    val convId = Conversations.insert {
//        it[Conversations.name] = name
//        it[createdAt] = Clock.System.now()
//        it[createdBy] = 1
//    } get Conversations.id
//
//    users.forEach {userId ->
//        ConversationMembers.insert {
//            it[ConversationMembers.convId] = convId
//            it[ConversationMembers.userId] = userId
//        }
//    }
//
//    return convId
//}

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

//    initializeDB()

    transaction {
        SchemaUtils.create(Messages)
//        Users.createUser("kavyamurthy", "Kavya", "thekavyamurthy@gmail.com", "kavya")
//        Users.createUser("vikasmurthy", "Vikas", "vikasmurthy@gmail.com", "vikas")
//        Users.createUser("koalaperson", "Koala", "koalaperson25@gmail.com", "koala")

//        Groups.insert {
//            it[name] = "Huzzah"
//            it[createdBy] = 10
//        }

//        GroupMembers.insert {
//            it[groupId] = 10
//            it[userId] = 10
//        }
//
//        GroupMembers.insert {
//            it[groupId] = 10
//            it[userId] = 11
//        }
//
//        GroupMembers.insert {
//            it[groupId] = 10
//            it[userId] = 12
//        }

//        Spaces.insert {
//            it[name] = "Animals"
//            it[groupId] = 10
//        }
//
//        Spaces.insert {
//            it[name] = "Shapes"
//            it[groupId] = 10
//        }
    }
}

private infix fun Int.equalsAny(other: Expression<List<Int>>): EqOp =
    intLiteral(this) eqAny other

private infix fun <T> Expression<T>.eqAny(other: Expression<List<T>>): EqOp = EqOp(this, any(other))

private fun <T> any(
    expression: Expression<List<T>>,
): ExpressionWithColumnType<String?> = CustomStringFunction("ANY", expression)