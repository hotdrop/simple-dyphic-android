package jp.hotdrop.simpledyphic.core.log

interface AppLogger {
    fun i(message: String)
    fun e(message: String, throwable: Throwable? = null)
}
