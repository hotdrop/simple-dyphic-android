package jp.hotdrop.simpledyphic.data.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import jp.hotdrop.simpledyphic.model.ExerciseAdviceInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

@Singleton
class LiteRtLmAdviceClient @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val engineMutex = Mutex()
    private val generationMutex = Mutex()
    private var currentModelPath: String? = null
    private var engine: Engine? = null

    suspend fun streamAdvice(input: ExerciseAdviceInput): Flow<String> = flow {
        generationMutex.withLock {
            val modelPath = requireNotNull(input.settings.modelFilePath) { "Model file is not configured" }
            val activeEngine = ensureEngine(modelPath)
            val conversationConfig = ConversationConfig(
                systemInstruction = Contents.of(BASE_SYSTEM_PROMPT),
                samplerConfig = SamplerConfig(
                    topK = 32,
                    topP = 0.9,
                    temperature = 0.8
                )
            )
            activeEngine.createConversation(conversationConfig).use { conversation ->
                var rendered = ""
                conversation.sendMessageAsync(buildPrompt(input)).collect { message ->
                    val candidate = extractText(message)
                    if (candidate.isBlank()) {
                        return@collect
                    }
                    rendered = mergeRenderedText(rendered, candidate)
                    emit(rendered)
                }
            }
        }
    }

    private suspend fun ensureEngine(modelPath: String): Engine {
        return engineMutex.withLock {
            if (currentModelPath == modelPath && engine != null) {
                return@withLock requireNotNull(engine)
            }

            engine?.close()
            val initialized = runCatching {
                createEngine(modelPath, Backend.GPU())
            }.getOrElse { error ->
                Timber.w(error, "Failed to initialize LiteRT-LM with GPU backend. Falling back to CPU.")
                createEngine(modelPath, Backend.CPU())
            }

            currentModelPath = modelPath
            engine = initialized
            initialized
        }
    }

    private fun createEngine(modelPath: String, backend: Backend): Engine {
        val cacheDir = File(context.cacheDir, "litertlm-cache").apply { mkdirs() }
        return Engine(
            EngineConfig(
                modelPath = modelPath,
                backend = backend,
                cacheDir = cacheDir.absolutePath
            )
        ).also { it.initialize() }
    }

    private fun buildPrompt(input: ExerciseAdviceInput): String {
        return buildString {
            appendLine(input.settings.advisorPrompt)
            appendLine()
            appendLine(input.promptDataBlock)
            appendLine()
            append("この内容を踏まえて、前向きな運動アドバイスを日本語で 3〜5 文程度で返してください。")
        }
    }

    private fun extractText(message: Message): String {
        return message.contents.contents
            .mapNotNull { content ->
                (content as? com.google.ai.edge.litertlm.Content.Text)?.text
            }
            .joinToString(separator = "")
    }

    private fun mergeRenderedText(previous: String, candidate: String): String {
        return when {
            candidate.startsWith(previous) -> candidate
            previous.endsWith(candidate) -> previous
            else -> previous + candidate
        }
    }

    companion object {
        private const val BASE_SYSTEM_PROMPT: String =
            "あなたは医療診断を行わない、明るく親しみやすい運動アドバイザーです。ユーザーが継続したくなる、" +
                "具体的で前向きなコメントを返してください。"
    }
}
