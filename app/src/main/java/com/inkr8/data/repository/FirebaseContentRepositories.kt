package com.inkr8.data.repository

import com.inkr8.data.dto.*
import com.inkr8.data.mapper.*
import com.inkr8.data.source.FunctionsDataSource
import com.inkr8.domain.repository.*

internal class FirebaseHomeRepository(private val source: FunctionsDataSource) : HomeRepository {
    override suspend fun getHome() = source.call("getHome").toHome()
    override suspend fun getExample(day: Long) = source.call("getWordExample", mapOf("day" to day)).string("sentence")
}
internal class FirebaseSeasonRepository(private val source: FunctionsDataSource) : SeasonRepository {
    override suspend fun getRanking() = source.call("getSeasonRanking").toRanking()
    override suspend fun getHistory() = source.call("getSeasonHistory").maps("seasons").map { it.toSeasonHistory() }
}
internal class FirebaseProgressRepository(private val source: FunctionsDataSource) : ProgressRepository {
    override suspend fun getProgress() = source.call("getProfileProgress").toProgress()
}
