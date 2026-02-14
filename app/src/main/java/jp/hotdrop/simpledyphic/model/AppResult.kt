package jp.hotdrop.simpledyphic.model

import kotlinx.coroutines.CancellationException

sealed interface AppResult<out T> {
    data class Success<out T>(val value: T) : AppResult<T>
    data class Failure(val error: Throwable) : AppResult<Nothing>
}

sealed interface AppCompletable {
    data object Complete : AppCompletable
    data class Failure(val error: Throwable) : AppCompletable
}

suspend inline fun <T> appResultSuspend(crossinline block: suspend () -> T): AppResult<T> {
    return try {
        AppResult.Success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        AppResult.Failure(error)
    }
}

suspend inline fun appCompletableSuspend(crossinline block: suspend () -> Unit): AppCompletable {
    return try {
        block()
        AppCompletable.Complete
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        AppCompletable.Failure(error)
    }
}
