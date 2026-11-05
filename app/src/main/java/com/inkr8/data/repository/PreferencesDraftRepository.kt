package com.inkr8.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.inkr8.domain.model.PlayMode
import com.inkr8.domain.model.WritingMode
import com.inkr8.domain.repository.Draft
import com.inkr8.domain.repository.DraftRepository
import org.json.JSONObject

internal class PreferencesDraftRepository(private val preferences: SharedPreferences) : DraftRepository {
    private fun key(userId: String, mode: PlayMode) = "$userId-${mode.name}"
    override fun load(userId: String, mode: PlayMode): Draft? {
        val saved = preferences.getString(key(userId, mode), null) ?: return null
        return decode(saved)
    }
    override fun find(userId: String, gameId: String): Draft? = preferences.getString("$userId-game-$gameId", null)?.let(::decode)
    private fun decode(saved: String): Draft? {
        return try {
            val json = JSONObject(saved)
            Draft(json.getString("id"), WritingMode.valueOf(json.getString("writingMode")), json.getString("text"))
        } catch (_: org.json.JSONException) { null }
        catch (_: IllegalArgumentException) { null }
    }
    override fun save(userId: String, mode: PlayMode, draft: Draft) {
        val value = JSONObject().put("id", draft.id).put("writingMode", draft.writingMode.name).put("text", draft.text).toString()
        preferences.edit { putString(key(userId, mode), value); putString("$userId-game-${draft.id}", value) }
    }
    override fun clear(userId: String, mode: PlayMode) { preferences.edit { remove(key(userId, mode)) } }
}
