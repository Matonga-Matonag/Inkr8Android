package com.inkr8.data.source

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleSignInDataSource(private val context: Context) {
    private val manager = CredentialManager.create(context)
    @SuppressLint("DiscouragedApi")
    suspend fun token(activityContext: Context): String {
        val resource = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        check(resource != 0) { "Falta configurar el cliente OAuth de Google en Firebase." }
        val option = GetSignInWithGoogleOption.Builder(context.getString(resource)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try { manager.getCredential(activityContext, request).credential }
        catch (error: NoCredentialException) { throw IllegalStateException("Añade una cuenta de Google al dispositivo e inténtalo otra vez.", error) }
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { "Google devolvió una credencial desconocida." }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
    suspend fun signOut() { manager.clearCredentialState(ClearCredentialStateRequest()) }
}
