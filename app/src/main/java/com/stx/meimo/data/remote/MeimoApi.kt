package com.stx.meimo.data.remote

import com.stx.meimo.data.model.*
import retrofit2.http.Body
import retrofit2.http.POST

interface MeimoApi {

    // User
    @POST("user/info")
    suspend fun getUserInfo(@Body body: @JvmSuppressWildcards Map<String, Any> = emptyMap()): ApiResponse<UserDto>

    @POST("user/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<UserDto>

    @POST("user/guest")
    suspend fun guestLogin(@Body body: @JvmSuppressWildcards Map<String, Any> = emptyMap()): ApiResponse<UserDto>

    // Role / Character
    @POST("role/category/list")
    suspend fun getCategoryList(@Body body: @JvmSuppressWildcards Map<String, Any> = emptyMap()): ApiResponse<List<CategoryDto>>

    @POST("role/list")
    suspend fun getRoleList(@Body request: RoleListRequest): ApiResponse<PagedData<RoleCardDto>>

    @POST("role/list")
    suspend fun searchRoles(@Body request: RoleSearchRequest): ApiResponse<PagedData<RoleCardDto>>

    @POST("role/list/hot")
    suspend fun getHotRoles(@Body request: RoleHotListRequest): ApiResponse<PagedData<RoleCardDto>>

    @POST("role/list/hot")
    suspend fun getHotRolesRaw(@Body request: RoleHotListRequest): ApiResponse<com.google.gson.JsonElement>

    @POST("role/list/recommend")
    suspend fun getRecommendedRoles(@Body request: RoleRecommendRequest): ApiResponse<PagedData<RoleCardDto>>

    @POST("role/list/recommend")
    suspend fun getRecommendedRolesRaw(@Body request: RoleRecommendRequest): ApiResponse<com.google.gson.JsonElement>

    @POST("role/top")
    suspend fun getTopRoles(@Body body: @JvmSuppressWildcards Map<String, Any> = emptyMap()): ApiResponse<List<RoleCardDto>>

    @POST("role/query")
    suspend fun getRoleDetail(@Body request: RoleQueryRequest): ApiResponse<RoleDetailDto>

    @POST("role/list/fav")
    suspend fun getFavorites(@Body request: FavListRequest): ApiResponse<PagedData<RoleCardDto>>

    @POST("role/customize/list")
    suspend fun getMyRoles(@Body request: CustomizeListRequest): ApiResponse<PagedData<RoleCardDto>>

    @POST("role/favourite")
    suspend fun toggleFavourite(@Body body: @JvmSuppressWildcards Map<String, Any>): ApiResponse<Boolean>

    // Chat
    @POST("conversation/list")
    suspend fun getAllConversations(@Body body: @JvmSuppressWildcards Map<String, Any> = mapOf("page" to 1, "size" to 50)): ApiResponse<List<ConversationDto>>

    @POST("conversation2/list")
    suspend fun getConversations(@Body request: ConversationListRequest): ApiResponse<PagedData<ConversationDto>>

    @POST("conversation2/create")
    suspend fun createConversation(@Body body: @JvmSuppressWildcards Map<String, Any>): ApiResponse<ConversationDto>

    @POST("conversation2/delete")
    suspend fun deleteConversation(@Body body: @JvmSuppressWildcards Map<String, Any>): ApiResponse<Any>

    @POST("message/history/page")
    suspend fun getMessageHistory(@Body request: MessageHistoryRequest): ApiResponse<PagedData<MessageDto>>

    // Model
    @POST("model/list")
    suspend fun getModelList(@Body body: @JvmSuppressWildcards Map<String, Any> = emptyMap()): ApiResponse<List<ModelDto>>

    @POST("model/category/list")
    suspend fun getModelCategories(@Body body: @JvmSuppressWildcards Map<String, Any> = emptyMap()): ApiResponse<List<ModelCategoryDto>>

    // Chat send (synchronous — returns AI reply as MessageDto)
    @POST("chat/completions")
    suspend fun chatCompletions(@Body request: ChatCompletionRequest): ApiResponse<MessageDto>

    // User actions
    @POST("user/sign-in")
    suspend fun signIn(@Body body: @JvmSuppressWildcards Map<String, Any> = emptyMap()): ApiResponse<Boolean>

    @POST("user/task/list")
    suspend fun getTaskList(@Body body: @JvmSuppressWildcards Map<String, Any> = emptyMap()): ApiResponse<List<TaskDto>>

    // Instruction
    @POST("instruction/list")
    suspend fun getInstructions(@Body body: @JvmSuppressWildcards Map<String, Any> = emptyMap()): ApiResponse<List<InstructionDto>>

    // Notice
    @POST("notification/unreadCount")
    suspend fun getUnreadCount(@Body body: Map<String, Any> = mapOf("types" to listOf(1, 2, 3, 4, 5, 6, 7))): ApiResponse<List<UnreadCountDto>>
}

data class InstructionDto(
    val id: Long,
    val name: String,
    val content: String? = null,
    val sort: Int = 0
)

data class UnreadCountDto(
    val type: Int,
    val num: Int
)

data class TaskDto(
    val id: Long = 0,
    val name: String? = null,
    val description: String? = null,
    val reward: Int = 0,
    val progress: Int = 0,
    val target: Int = 0,
    val status: Int = 0,
    val type: Int = 0
)
