package com.expensetracker.app.domain.usecase

import java.security.MessageDigest

object DedupUseCase {
    fun computeHash(source: String, merchant: String, amount: Double, date: Long): String {
        val raw = "${source}|${merchant}|${amount}|${date}"
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
