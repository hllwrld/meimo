package com.stx.meimo.data.remote

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

sealed interface SseEvent {
    data class Message(val data: String) : SseEvent
    data class Error(val throwable: Throwable) : SseEvent
    data object Complete : SseEvent
}

class SseClient(private val client: OkHttpClient) {

    fun stream(url: String, jsonBody: String, headers: Map<String, String> = emptyMap()): Flow<SseEvent> =
        callbackFlow {
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)

            headers.forEach { (k, v) -> requestBuilder.header(k, v) }

            val factory = EventSources.createFactory(client)
            val listener = object : EventSourceListener() {
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    if (data == "[DONE]") {
                        trySend(SseEvent.Complete)
                        close()
                        return
                    }
                    trySend(SseEvent.Message(data))
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    if (t != null) {
                        trySend(SseEvent.Error(t))
                    }
                    close()
                }

                override fun onClosed(eventSource: EventSource) {
                    trySend(SseEvent.Complete)
                    close()
                }
            }

            val eventSource = factory.newEventSource(requestBuilder.build(), listener)

            awaitClose {
                eventSource.cancel()
            }
        }
}
