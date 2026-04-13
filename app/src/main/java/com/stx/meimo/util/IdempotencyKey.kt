package com.stx.meimo.util

import kotlin.random.Random

fun generateIdempotencyKey(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    val prefix = buildString(16) {
        repeat(16) { append(chars[Random.nextInt(chars.length)]) }
    }
    val suffix = buildString(8) {
        repeat(8) { append(Random.nextInt(10)) }
    }
    return prefix + suffix
}
