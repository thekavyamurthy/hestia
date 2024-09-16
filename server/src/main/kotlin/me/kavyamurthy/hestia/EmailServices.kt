package me.kavyamurthy.hestia

import aws.sdk.kotlin.services.ses.SesClient
import aws.sdk.kotlin.services.ses.model.Body
import aws.sdk.kotlin.services.ses.model.Content
import aws.sdk.kotlin.services.ses.model.Destination
import aws.sdk.kotlin.services.ses.model.SendEmailRequest
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.minutes

private val client = SesClient { region = "us-east-2" }
private val codeCache = Cache.Builder<String, Int>()
    .expireAfterWrite(15.minutes)
    .build()
private val logger = LoggerFactory.getLogger("EmailServices")

suspend fun sendCode(email: String) {
    val code = Random.nextInt(100_000 .. 999_999)
    codeCache.put(email, code)

    val bodyHTML = (
        "<html><head></head><body><h1>Verification Code: $code</h1></body></html>"
    )

    val msgOb = aws.sdk.kotlin.services.ses.model.Message {
        subject = Content { data = "Verification Code" }
        body = Body { html = Content { data = bodyHTML } }
    }

    val emailRequest = SendEmailRequest {
        destination = Destination { toAddresses = listOf(email) }
        message = msgOb
        source = "no-reply@hestia.fun"
    }


    println("Attempting to send an email through Amazon SES using the AWS SDK for Kotlin...")
    client.sendEmail(emailRequest)
}

fun verifyCode(email: String, enteredCode: Int): Boolean {
    val code = codeCache.get(email)
    if(code == null) {
        logger.info("No code in cache for $email")
        return false
    }
    logger.info("code = $code, enteredCode = $enteredCode")
    return code == enteredCode
}

fun main() = runBlocking {

}