package com.stx.meimo.data.model

data class CategoryDto(
    val id: Int?,
    val name: String
)

data class RoleCardDto(
    val id: Long,
    val userId: Long? = null,
    val name: String,
    val roleDesc: String? = null,
    val avatar: String? = null,
    val imageUrl: String? = null,
    val usageNum: Long = 0,
    val score: Double? = null,
    val scoreNum: Int = 0,
    val playerNum: Int = 0,
    val commentNum: Int = 0,
    val authorName: String? = null,
    val categoryIds: List<Int>? = null,
    val equippedBadges: List<BadgeDto>? = null,
    val likeCount: Int = 0,
    val disLikeCount: Int = 0,
    val isFavourite: Int = 0
)

data class RoleDetailDto(
    val id: Long,
    val userId: Long? = null,
    val name: String,
    val roleDesc: String? = null,
    val avatar: String? = null,
    val imageUrl: String? = null,
    val backgroundUrl: String? = null,
    val beginning: String? = null,
    val beginningZh: String? = null,
    val personality: String? = null,
    val personalityZh: String? = null,
    val example: String? = null,
    val exampleZh: String? = null,
    val prologue: List<String>? = null,
    val usageNum: Long = 0,
    val score: Double? = null,
    val scoreNum: Int = 0,
    val playerNum: Int = 0,
    val commentNum: Int = 0,
    val authorName: String? = null,
    val categoryIds: List<Int>? = null,
    val equippedBadges: List<BadgeDto>? = null,
    val likeCount: Int = 0,
    val disLikeCount: Int = 0,
    val isFavourite: Int = 0,
    val pointsConsumed: Long? = null,
    val personalityWordCount: Int = 0
)

data class BadgeDto(
    val badgeName: String? = null,
    val badgeIcon: String? = null,
    val nameColorStart: String? = null,
    val nameColorEnd: String? = null,
    val isNameColored: Int = -1,
    val animate: Int = -1
)

data class RoleListRequest(
    val scope: Int = 1,
    val page: Int = 1,
    val size: Int = 20,
    val sort: Int = 1,
    val categoryIdArr: List<Int> = emptyList()
)

data class RoleHotListRequest(
    val page: Int = 1,
    val size: Int = 20,
    val dayNum: Int = 1,
    val categoryIdArr: List<Int> = emptyList(),
    val sort: Int = 2
)

data class RoleRecommendRequest(
    val scene: String = "MAIN_PAGE_RECOMMEND",
    val page: Int = 1,
    val size: Int = 20,
    val categoryIds: List<Int>? = null
)

data class RoleQueryRequest(
    val id: String
)

data class FavListRequest(
    val page: Int = 1,
    val size: Int = 10,
    val sort: Int = 2
)

data class RoleSearchRequest(
    val keyword: String,
    val page: Int = 1,
    val size: Int = 20,
    val scope: Int = 1,
    val sort: Int = 1
)

data class CustomizeListRequest(
    val page: Int = 1,
    val size: Int = 10,
    val viewStatus: List<Int> = listOf(1),
    val showTop: Boolean = true,
    val sort: Int = 1
)
