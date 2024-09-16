package me.kavyamurthy.hestia

import com.auth0.jwt.algorithms.Algorithm
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import net.jpountz.xxhash.XXHashFactory

private val xxHash32 = XXHashFactory.fastestInstance().hash32()

data class DBConfig(val username: String, val password: String, val url: String)
data class AuthConfig(val salt: String, val seed: Int)
data class ServerConfig(val secret: String, val port: Int)
data class Config(val db: DBConfig, val auth: AuthConfig, val server: ServerConfig)

val config = ConfigLoaderBuilder.default()
//    .addResourceSource("/hestia-dev.yaml", true, true)
    .addResourceSource("/hestia.yaml")
    .build()
    .loadConfigOrThrow<Config>()

val jwtAlgorithm: Algorithm = Algorithm.HMAC256(config.server.secret)

fun hashPassword(password: String): Int {
    val passwordBytes = (password + config.auth.salt).toByteArray()
    return xxHash32.hash(passwordBytes, 0, passwordBytes.size, config.auth.seed)
}