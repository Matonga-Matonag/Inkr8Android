package com.inkr8.data.source

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.inkr8.data.dto.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

internal class FirestoreDataSource(private val firestore: FirebaseFirestore) {
    fun observeDocument(path: String): Flow<WireMap> = callbackFlow {
        val registration = firestore.document(path).addSnapshotListener { snapshot, error ->
            if (error != null) close(error)
            else if (snapshot?.exists() == true) trySend(snapshot.data!!.wire())
            else close(IllegalStateException("No se encontró el documento solicitado."))
        }
        awaitClose { registration.remove() }
    }
    suspend fun recentGames(userId: String): List<GameDto> = firestore.collection("submissions")
        .whereEqualTo("authorId", userId).whereEqualTo("schemaVersion", 2)
        .orderBy("timestamp", Query.Direction.DESCENDING).limit(20).get(Source.SERVER).await()
        .documents.map { GameDto(it.id, it.data!!.wire()) }
}
