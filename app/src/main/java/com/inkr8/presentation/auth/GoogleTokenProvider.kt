package com.inkr8.presentation.auth

import android.content.Context

interface GoogleTokenProvider {
    suspend fun token(activityContext: Context): String
}
