package mono.hg.wrappers

import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.lang.ref.WeakReference

/**
 * TextSpectator is an extension to TextWatcher, used when it is attached to an EditText.
 */
open class TextSpectator protected constructor(editText: EditText) : TextWatcher {
    private val watchField: WeakReference<EditText>?
    private var tickDuration = DEFAULT_TICK_DURATION
    private var timerStopped = false
    private val handler = Handler()
    private val runnable: Runnable = object : Runnable {
        override fun run() {
            if (! timerStopped) {
                whenTimerTicked()
                handler.postDelayed(this, tickDuration.toLong())
            }
        }
    }

    /**
     * Fetch the input text of the attached EditText;
     *
     * @return The contents of EditText.
     */
    protected val inputText: String?
        get() = if (watchField != null) {
            watchField.get() !!.text.toString()
        } else {
            null
        }

    /**
     * Fetch the input text of the attached EditText. Trimmed using String.trim().
     *
     * @return The trimmed contents of EditText;
     */
    val trimmedInputText: String
        get() = if (watchField != null) {
            watchField.get() !!.text.toString().trim { it <= ' ' }
        } else {
            ""
        }

    /**
     * Stops the Handler loop.
     */
    protected fun stopTimer() {
        timerStopped = true
    }

    /**
     * Starts the Handler loop.
     */
    protected fun startTimer() {
        timerStopped = false
        handler.postDelayed(runnable, tickDuration.toLong())
    }

    /**
     * Sets the Handler loop duration.
     *
     * @param duration In milliseconds, how long should the Handler delay before looping?
     */
    fun setTimerDuration(duration: Int) {
        tickDuration = duration
    }

    /**
     * Actions done when the Handler completes a single loop.
     */
    open fun whenTimerTicked() {
        // Overridden when necessary.
    }

    /**
     * A syntactic sugar for beforeTextChanged().
     */
    fun beforeChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // Overridden when necessary.
    }

    /**
     * A syntactic sugar for onTextChanged().
     */
    open fun whenChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // Overridden when necessary.
    }

    /**
     * A syntactic sugar for afterTextChanged().
     */
    open fun afterChanged(s: Editable?) {
        // Overridden when necessary.
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        beforeChanged(s, start, count, after)
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        whenChanged(s, start, before, count)
    }

    override fun afterTextChanged(s: Editable) {
        if (s.isNotEmpty() && s[0] == ' ') {
            s.delete(0, 1)
        }
        afterChanged(s)
    }

    companion object {
        // The default duration of the Handler loop, in milliseconds.
        private const val DEFAULT_TICK_DURATION = 125
    }

    init {
        watchField = WeakReference(editText)
    }
}