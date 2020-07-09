package mono.hg.wrappers

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceViewHolder
import mono.hg.R

/**
 * [DropDownPreference] with spinner as custom widget.
 * Based off of work from SpinnerPreference by [hidroh@github](https://github.com/hidroh).
 */
class SpinnerPreference @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0
) : DropDownPreference(context, attrs, defStyleAttr) {
    private val mAdapter: ArrayAdapter<*>
    private var mSelection = 0
    private lateinit var spinner: Spinner
    override fun onBindViewHolder(view: PreferenceViewHolder) {
        spinner = view.itemView.findViewById(R.id.spinner)
        spinner.adapter = mAdapter
        spinner.setSelection(mSelection)
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                if (position > 0) {
                    val value = entryValues[position].toString()
                    if (value != getValue() && callChangeListener(value)) {
                        mSelection = position
                        setValue(value)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No-op.
            }
        }
        super.onBindViewHolder(view)
    }

    override fun onClick() {
        spinner.performClick()
    }

    init {
        layoutResource = R.layout.layout_preference_spinner
        mAdapter = createAdapter()
    }
}