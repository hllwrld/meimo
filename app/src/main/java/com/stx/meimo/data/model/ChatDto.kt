package com.stx.meimo.data.model

data class MessageDto(
    val id: Long,
    val roleId: Long = 0,
    val userId: Long = 0,
    val direction: Int = 0,
    val content: String? = null,
    val contentEn: String? = null,
    val model: String? = null,
    val consumption: Int = 0,
    val createTime: String? = null,
    val deleted: Int = 0
)

data class ConversationDto(
    val id: Long,
    val roleId: Long = 0,
    val userId: Long = 0,
    val avatar: String? = null,
    val name: String? = null,
    val nameEn: String? = null,
    val content: String? = null,
    val updateTime: String? = null
)

data class MessageHistoryRequest(
    val roleId: String,
    val page: Int = 1,
    val size: Int = 50
)

data class ConversationListRequest(
    val roleId: String
)

data class SendMessageRequest(
    val roleId: String,
    val content: String,
    val modelId: Int = 0,
    val maxToken: Int = 4096
)

data class ChatCompletionRequest(
    val roleId: String,
    val content: String,
    val modelId: Int,
    val maxToken: Int = 4096
)
