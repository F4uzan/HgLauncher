package mono.hg.wrappers;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.lang.ref.WeakReference;

/**
 * TextSpectator is an extension to TextWatcher, used when it is attached to an EditText.
 */
public class TextSpectator implements TextWatcher {
    // The default duration of the Handler loop, in milliseconds.
    private static int DEFAULT_TICK_DURATION = 125;

    private WeakReference<EditText> watchField;
    private int tickDuration = DEFAULT_TICK_DURATION;
    private boolean timerStopped = false;
    private boolean spaceSpam = false;

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override public void run() {
            if (!timerStopped) {
                whenTimerTicked();
                handler.postDelayed(this, tickDuration);
            }
        }
    };

    protected TextSpectator(EditText editText) {
        watchField = new WeakReference<>(editText);
    }

    /**
     * Fetch the input text of the attached EditText;
     *
     * @return The contents of EditText.
     */
    protected String getInputText() {
        if (watchField != null) {
            return watchField.get().getText().toString();
        } else {
            return null;
        }
    }

    /**
     * Fetch the input text of the attached EditText. Trimmed using String.trim().
     *
     * @return The trimmed contents of EditText;
     */
    public String getTrimmedInputText() {
        if (watchField != null) {
            return watchField.get().getText().toString().trim();
        } else {
            return null;
        }
    }

    /**
     * Should the user be allowed to start the query with empty spaces?
     *
     * @param enabled Whether query can start with an empty space.
     */
    public void spaceSpamming(boolean enabled) {
        spaceSpam = enabled;
    }

    /**
     * Stops the Handler loop.
     */
    protected void stopTimer() {
        timerStopped = true;
    }

    /**
     * Starts the Handler loop.
     */
    protected void startTimer() {
        timerStopped = false;
        handler.postDelayed(runnable, tickDuration);
    }

    /**
     * Sets the Handler loop duration.
     *
     * @param duration In milliseconds, how long should the Handler delay before looping?
     */
    public void setTimerDuration(int duration) {
        tickDuration = duration;
    }

    /**
     * Actions done when the Handler completes a single loop.
     */
    public void whenTimerTicked() {
        // Overridden when necessary.
    }

    public void beforeChanged(CharSequence s, int start, int count, int after) {
        // Overridden when necessary.
    }

    public void whenChanged(CharSequence s, int start, int before, int count) {
        // Overridden when necessary.
    }

    public void afterChanged(Editable s) {
        // Overridden when necessary.
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        beforeChanged(s, start, count, after);
    }

    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        whenChanged(s, start, before, count);
    }

    @Override public void afterTextChanged(Editable s) {
        if (!spaceSpam && s.length() > 0 && s.charAt(0) == ' ') {
            s.delete(0, 1);
        }

        afterChanged(s);
    }
}
