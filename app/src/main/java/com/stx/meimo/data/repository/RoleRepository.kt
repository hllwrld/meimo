package com.stx.meimo.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.stx.meimo.data.model.*
import com.stx.meimo.data.remote.MeimoApi
import com.stx.meimo.util.ApiException
import android.util.Log

class RoleRepository(private val api: MeimoApi, private val gson: Gson = Gson()) {

    /** Parse JsonElement that may be PagedData{total,content} or plain List */
    private fun parseRolesData(data: JsonElement): PagedData<RoleCardDto> {
        Log.d("RoleRepo", "parseRolesData: isArray=${data.isJsonArray}")
        // Log first item's fields to check score
        if (data.isJsonArray && data.asJsonArray.size() > 0) {
            val first = data.asJsonArray[0].asJsonObject
            Log.d("RoleRepo", "First item keys: ${first.keySet()}")
            Log.d("RoleRepo", "First item score=${first.get("score")}, scoreNum=${first.get("scoreNum")}, usageNum=${first.get("usageNum")}, playerNum=${first.get("playerNum")}")
        } else if (data.isJsonObject) {
            val content = data.asJsonObject.getAsJsonArray("content")
            if (content != null && content.size() > 0) {
                val first = content[0].asJsonObject
                Log.d("RoleRepo", "First item keys: ${first.keySet()}")
                Log.d("RoleRepo", "First item score=${first.get("score")}, scoreNum=${first.get("scoreNum")}, usageNum=${first.get("usageNum")}, playerNum=${first.get("playerNum")}")
            }
        }
        return if (data.isJsonArray) {
            val listType = object : TypeToken<List<RoleCardDto>>() {}.type
            val list: List<RoleCardDto> = gson.fromJson(data, listType)
            Log.d("RoleRepo", "Parsed array: ${list.size} items")
            PagedData(total = list.size, content = list)
        } else {
            val pagedType = object : TypeToken<PagedData<RoleCardDto>>() {}.type
            val result: PagedData<RoleCardDto> = gson.fromJson(data, pagedType)
            Log.d("RoleRepo", "Parsed PagedData: total=${result.total}, content=${result.content.size}")
            result
        }
    }

    suspend fun getCategories(): Result<List<CategoryDto>> = runCatching {
        val response = api.getCategoryList()
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to get categories")
        }
        response.data
    }

    suspend fun getRoleList(
        page: Int = 1,
        size: Int = 20,
        categoryIds: List<Int> = emptyList(),
        sort: Int = 1
    ): Result<PagedData<RoleCardDto>> = runCatching {
        val response = api.getRoleList(
            RoleListRequest(page = page, size = size, sort = sort, categoryIdArr = categoryIds)
        )
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to get roles")
        }
        response.data
    }

    suspend fun getHotRoles(
        page: Int = 1,
        size: Int = 20,
        categoryIds: List<Int> = emptyList(),
        dayNum: Int = 1
    ): Result<PagedData<RoleCardDto>> = runCatching {
        val request = RoleHotListRequest(page = page, size = size, categoryIdArr = categoryIds, dayNum = dayNum)
        val response = api.getHotRolesRaw(request)
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to get hot roles")
        }
        parseRolesData(response.data)
    }

    suspend fun getRoleDetail(roleId: Long): Result<RoleDetailDto> = runCatching {
        val response = api.getRoleDetail(RoleQueryRequest(roleId.toString()))
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to get role detail")
        }
        response.data
    }

    suspend fun getFavorites(page: Int = 1, size: Int = 10): Result<PagedData<RoleCardDto>> = runCatching {
        val response = api.getFavorites(FavListRequest(page = page, size = size))
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to get favorites")
        }
        response.data
    }

    suspend fun getRecommendedRoles(
        page: Int = 1,
        size: Int = 20,
        categoryIds: List<Int> = emptyList()
    ): Result<PagedData<RoleCardDto>> = runCatching {
        val request = RoleRecommendRequest(page = page, size = size, categoryIds = categoryIds.ifEmpty { null })
        val response = api.getRecommendedRolesRaw(request)
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to get recommendations")
        }
        parseRolesData(response.data)
    }

    suspend fun searchRoles(keyword: String, page: Int = 1, size: Int = 20): Result<PagedData<RoleCardDto>> = runCatching {
        val response = api.searchRoles(RoleSearchRequest(keyword = keyword, page = page, size = size))
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Search failed")
        }
        response.data
    }

    suspend fun toggleFavourite(roleId: Long): Result<Boolean> = runCatching {
        val response = api.toggleFavourite(mapOf("id" to roleId.toString()))
        if (response.code != 200) {
            throw ApiException(response.code, response.message ?: "Failed to toggle favourite")
        }
        response.data ?: false
    }

    suspend fun getMyRoles(page: Int = 1, size: Int = 10): Result<PagedData<RoleCardDto>> = runCatching {
        val response = api.getMyRoles(CustomizeListRequest(page = page, size = size))
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to get my roles")
        }
        response.data
    }
}
