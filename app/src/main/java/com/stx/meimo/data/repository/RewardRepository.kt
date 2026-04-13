package com.stx.meimo.data.repository

import com.stx.meimo.data.remote.MeimoApi
import com.stx.meimo.data.remote.TaskDto
import com.stx.meimo.util.ApiException

class RewardRepository(private val api: MeimoApi) {

    suspend fun signIn(): Result<Boolean> = runCatching {
        val response = api.signIn()
        if (response.code != 200) {
            throw ApiException(response.code, response.message ?: "签到失败")
        }
        response.data ?: true
    }

    suspend fun getTaskList(): Result<List<TaskDto>> = runCatching {
        val response = api.getTaskList()
        if (response.code != 200 || response.data == null) {
            throw ApiException(response.code, response.message ?: "获取任务失败")
        }
        response.data
    }
}
