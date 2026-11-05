package com.inkr8.data.source

import com.google.firebase.functions.FirebaseFunctions
import com.inkr8.data.dto.WireMap
import com.inkr8.data.dto.wire
import kotlinx.coroutines.tasks.await

internal class FunctionsDataSource(private val functions: FirebaseFunctions) {
    suspend fun call(name: String, data: Map<String, Any> = emptyMap()): WireMap =
        functions.getHttpsCallable(name).call(data).await().data.wire()
}
