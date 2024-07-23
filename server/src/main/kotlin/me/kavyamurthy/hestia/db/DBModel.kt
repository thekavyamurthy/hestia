package me.kavyamurthy.hestia.db

data class User(val id: Int, val emailId: String, val firstName: String,
                val lastName: String, val displayName: String?)