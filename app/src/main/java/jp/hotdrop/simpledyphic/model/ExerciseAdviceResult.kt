package jp.hotdrop.simpledyphic.model

sealed interface ExerciseAdviceResult {
    data class Initializing(
        val input: ExerciseAdviceInput
    ) : ExerciseAdviceResult

    data class Streaming(
        val input: ExerciseAdviceInput,
        val text: String
    ) : ExerciseAdviceResult

    data class Success(
        val input: ExerciseAdviceInput,
        val text: String
    ) : ExerciseAdviceResult
}
