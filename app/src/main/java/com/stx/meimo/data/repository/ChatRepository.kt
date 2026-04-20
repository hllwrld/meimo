package com.stx.meimo.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stx.meimo.data.model.ChatCompletionRequest
import com.stx.meimo.data.model.ConversationDto
import com.stx.meimo.data.model.ConversationListRequest
import com.stx.meimo.data.model.MessageDto
import com.stx.meimo.data.model.MessageHistoryRequest
import com.stx.meimo.data.model.PagedData
import com.stx.meimo.data.remote.InstructionDto
import com.stx.meimo.data.remote.MeimoApi
import com.stx.meimo.util.ApiException

class ChatRepository(
    private val api: MeimoApi,
    private val gson: Gson = Gson()
) {

    suspend fun getAllConversations(page: Int = 1, size: Int = 50): Result<List<ConversationDto>> = runCatching {
        val response = api.getAllConversationsRaw(mapOf("page" to page, "size" to size))
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to get conversations")
        }
        val data = response.data
        Log.d("ChatRepo", "getAllConversations raw: isArray=${data.isJsonArray}, isObject=${data.isJsonObject}")
        val list: List<ConversationDto> = if (data.isJsonArray) {
            val type = object : TypeToken<List<ConversationDto>>() {}.type
            gson.fromJson(data, type)
        } else {
            val obj = data.asJsonObject
            val content = obj.getAsJsonArray("content")
                ?: obj.getAsJsonArray("records")
                ?: obj.getAsJsonArray("list")
            if (content != null) {
                val type = object : TypeToken<List<ConversationDto>>() {}.type
                gson.fromJson(content, type)
            } else {
                Log.e("ChatRepo", "Unknown data format: $data")
                emptyList()
            }
        }
        Log.d("ChatRepo", "Parsed ${list.size} conversations")
        list
    }

    suspend fun getConversations(roleId: Long): Result<PagedData<ConversationDto>> = runCatching {
        val response = api.getConversations(ConversationListRequest(roleId.toString()))
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to get conversations")
        }
        response.data
    }

    suspend fun getMessageHistory(
        roleId: Long,
        page: Int = 1,
        size: Int = 50
    ): Result<PagedData<MessageDto>> = runCatching {
        val response = api.getMessageHistory(
            MessageHistoryRequest(roleId = roleId.toString(), page = page, size = size)
        )
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to get messages")
        }
        response.data
    }

    suspend fun createConversation(roleId: Long): Result<ConversationDto> = runCatching {
        val response = api.createConversation(mapOf("roleId" to roleId.toString()))
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to create conversation")
        }
        response.data
    }

    suspend fun getInstructions(): Result<List<InstructionDto>> = runCatching {
        val response = api.getInstructions()
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to get instructions")
        }
        response.data.sortedBy { it.sort }
    }

    suspend fun sendMessage(
        roleId: Long,
        content: String,
        modelId: Int,
        maxToken: Int = 4096
    ): Result<MessageDto> = runCatching {
        val response = api.chatCompletions(
            ChatCompletionRequest(
                roleId = roleId.toString(),
                content = content,
                modelId = modelId,
                maxToken = maxToken
            )
        )
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "发送失败")
        }
        response.data
    }
}
