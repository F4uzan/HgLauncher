package mono.hg.wrappers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.preference.DropDownPreference;
import androidx.preference.PreferenceViewHolder;

import mono.hg.R;

/**
 * {@link DropDownPreference} with spinner as custom widget.
 * Based off of work from SpinnerPreference by <a href="https://github.com/hidroh">hidroh@github</a>.
 */
public class SpinnerPreference extends DropDownPreference {
    private final ArrayAdapter mAdapter;
    private int mSelection;

    private Spinner spinner;

    public SpinnerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpinnerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.layout_preference_spinner);
        mAdapter = createAdapter();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        spinner = view.itemView.findViewById(R.id.spinner);
        spinner.setAdapter(mAdapter);
        spinner.setSelection(mSelection);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String value = getEntryValues()[position].toString();
                    if (!value.equals(getValue()) && callChangeListener(value)) {
                        mSelection = position;
                        setValue(value);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No-op.
            }
        });
        super.onBindViewHolder(view);
    }

    @Override
    protected void onClick() {
        spinner.performClick();
    }
}