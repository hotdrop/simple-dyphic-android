package jp.hotdrop.simpledyphic.domain.usecase

import java.time.LocalDate
import javax.inject.Inject
import jp.hotdrop.simpledyphic.data.ai.LiteRtLmAdviceClient
import jp.hotdrop.simpledyphic.model.AdvicePeriod
import jp.hotdrop.simpledyphic.model.AppResult
import jp.hotdrop.simpledyphic.model.ExerciseAdviceInput
import jp.hotdrop.simpledyphic.model.ExerciseAdviceResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GenerateExerciseAdviceUseCase @Inject constructor(
    private val inputBuilder: ExerciseAdviceInputBuilder,
    private val liteRtLmAdviceClient: LiteRtLmAdviceClient
) {
    suspend fun buildInput(
        period: AdvicePeriod,
        today: LocalDate = LocalDate.now()
    ): AppResult<ExerciseAdviceInput> = inputBuilder.build(period, today)

    fun execute(
        input: ExerciseAdviceInput
    ): Flow<ExerciseAdviceResult> = flow {
        require(input.canGenerate) { "Exercise advice input is incomplete" }
        emit(ExerciseAdviceResult.Initializing(input))
        var latestText = ""
        liteRtLmAdviceClient.streamAdvice(input).collect { text ->
            latestText = text
            emit(ExerciseAdviceResult.Streaming(input, text))
        }
        emit(ExerciseAdviceResult.Success(input, latestText))
    }
}
