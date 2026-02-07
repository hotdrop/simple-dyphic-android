package jp.hotdrop.simpledyphic.core.log

import android.util.Log
import javax.inject.Inject

class LogcatAppLogger @Inject constructor() : AppLogger {
    override fun i(message: String) {
        Log.i(TAG, message)
    }

    override fun e(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
    }

    private companion object {
        private const val TAG = "SimpleDyphic"
    }
}
