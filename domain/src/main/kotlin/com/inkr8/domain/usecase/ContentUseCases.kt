package com.inkr8.domain.usecase

import com.inkr8.domain.repository.HomeRepository
import com.inkr8.domain.repository.SeasonRepository
import com.inkr8.domain.repository.ProgressRepository

class GetHome(private val repository: HomeRepository) { suspend operator fun invoke() = repository.getHome() }
class GetSeasonRanking(private val repository: SeasonRepository) { suspend operator fun invoke() = repository.getRanking() }
class GetWordExample(private val repository: HomeRepository) { suspend operator fun invoke(day: Long) = repository.getExample(day) }
class GetSeasonHistory(private val repository: SeasonRepository) { suspend operator fun invoke() = repository.getHistory() }
class GetProgress(private val repository: ProgressRepository) { suspend operator fun invoke() = repository.getProgress() }
