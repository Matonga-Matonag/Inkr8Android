package com.inkr8.domain.model

enum class PlayMode { PRACTICE, RANKED }
enum class WritingMode(val requiredWords: Int, val minWords: Int, val maxWords: Int) {
    STANDARD(4, 50, 150), ON_TOPIC(2, 50, 200)
}
enum class GameStatus { DRAFT, PENDING, EVALUATED, FAILED }
enum class MatchStatus { NONE, PENDING, MATCHED, EXPIRED }
data class User(val id: String, val name: String, val email: String?, val merit: Long, val rating: Long, val league: Int, val username: String? = null)
data class Word(val text: String, val definition: String, val sentence: String, val phonetic: String = "", val partOfSpeech: String = "", val rarity: String = "")
data class Season(val id: String, val start: Long, val end: Long)
data class SeasonMember(val userId: String, val name: String, val rating: Long, val league: Int, val position: Long = 0)
data class SeasonRanking(val season: Season, val members: List<SeasonMember>, val own: SeasonMember? = null)
data class SeasonHistory(val season: Season, val position: Long, val rating: Long, val meritEarned: Long)
data class Statistics(val gamesPlayed: Long, val averageScore: Double, val bestScore: Double, val winRate: Double)
data class Progress(val history: List<Game>, val statistics: Statistics)
data class Home(val word: Word, val season: Season, val rankedCost: Long, val wordDay: Long = 0)
data class Challenge(val words: List<Word>, val topic: String?, val theme: String?, val advanced: List<String> = emptyList())
data class ConstraintResult(val label: String, val fulfilled: Boolean)
data class Evaluation(val score: Double, val feedback: String, val meritEarned: Long,
    val spelling: String = "", val vocabulary: String = "", val coherence: String = "", val creativity: String = "",
    val constraints: List<ConstraintResult> = emptyList(), val feedbackError: String? = null, val constraintsError: String? = null)
data class MatchResult(val opponentName: String, val opponentScore: Double, val outcome: String, val ratingChange: Long?)
data class Game(
    val id: String, val mode: PlayMode, val writingMode: WritingMode,
    val challenge: Challenge, val status: GameStatus, val content: String,
    val evaluation: Evaluation?, val match: MatchResult?, val error: String?, val seasonId: String,
    val matchStatus: MatchStatus = MatchStatus.NONE, val timestamp: Long = 0, val matchError: String? = null
)
