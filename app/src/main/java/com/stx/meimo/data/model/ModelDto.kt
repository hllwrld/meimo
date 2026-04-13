package com.stx.meimo.data.model

data class ModelDto(
    val id: Int,
    val name: String,
    val deductNum: Int = 0,
    val identifier: String? = null,
    val introduce: String? = null,
    val description: String? = null,
    val enableStream: Int = 0,
    val isPaidModel: Int = 0,
    val maxTokenList: List<MaxTokenDto>? = null,
    val successRate: Double = 0.0,
    val newFlag: Int = 0
)

data class MaxTokenDto(
    val maxToken: Int,
    val deductNum: Int
)

data class ModelCategoryDto(
    val id: Int,
    val name: String
)
