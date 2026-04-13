package com.stx.meimo.data.model

data class UserDto(
    val id: Long,
    val email: String? = null,
    val nickname: String? = null,
    val avatar: String? = null,
    val gender: Int = 0,
    val balance: Double = 0.0,
    val level: Int = 0,
    val fansNum: Int = 0,
    val followNum: Int = 0,
    val inviteCode: String? = null,
    val todayIsSign: Boolean = false,
    val authorPoints: Double = 0.0,
    val equippedBadges: List<BadgeDto>? = null,
    val totalRechargeAmount: Double = 0.0,
    val completedTutorial: Int = 0,
    val token: String? = null
)
