package me.kavyamurthy.hestia

import net.jpountz.xxhash.XXHashFactory

private val xxHash32 = XXHashFactory.fastestInstance().hash32()
private const val SALT = "gf7ik7m\$&aP#Mm"
private const val SEED = 0xBE548D
const val SECRET = "e42f5b73-f53d-428b-8fc0-98ac06e555db"
private const val DB_HOST = "db.czeu8uqce7rf.us-east-2.rds.amazonaws.com" //localhost
const val DB_URL = "jdbc:postgresql://$DB_HOST:5432/postgres"
const val DB_USERNAME = "postgres" //kavyamurthy
const val DB_PASSWORD = "UVWdwh24j8r9k6" // [blank]

fun hashPassword(password: String): Int {
    val passwordBytes = (password + SALT).toByteArray()
    return xxHash32.hash(passwordBytes, 0, passwordBytes.size, SEED)
}