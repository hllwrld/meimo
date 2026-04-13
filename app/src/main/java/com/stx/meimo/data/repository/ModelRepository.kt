package com.stx.meimo.data.repository

import com.stx.meimo.data.model.ModelCategoryDto
import com.stx.meimo.data.model.ModelDto
import com.stx.meimo.data.remote.MeimoApi
import com.stx.meimo.util.ApiException

class ModelRepository(private val api: MeimoApi) {

    suspend fun getModels(categoryId: Int? = null): Result<List<ModelDto>> = runCatching {
        val body = if (categoryId != null) mapOf("categoryId" to categoryId) else emptyMap()
        val response = api.getModelList(body)
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to get models")
        }
        response.data
    }

    suspend fun getModelCategories(): Result<List<ModelCategoryDto>> = runCatching {
        val response = api.getModelCategories()
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "Failed to get model categories")
        }
        response.data
    }
}
