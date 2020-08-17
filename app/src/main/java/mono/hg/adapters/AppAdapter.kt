package mono.hg.adapters

import android.view.KeyEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.FlexibleAdapter.OnItemClickListener
import mono.hg.models.App
import mono.hg.utils.AppUtils
import mono.hg.utils.Utils
import java.util.*

/**
 * Adapter used to handle generic list of apps.
 * Implements [SectionedAdapter] allowing fast-scroll seeking with sections.
 */
class AppAdapter(apps: List<App?>, listeners: Any?, stableIds: Boolean) :
    FlexibleAdapter<App>(apps, listeners, stableIds), SectionedAdapter {
    private var mSelectedItem = 0
    private var finishedLoading = false

    constructor(apps: List<App?>) : this(apps, null, true)

    /**
     * Resets the current filter, as well as the filtered items.
     */
    fun resetFilter() {
        setFilter("")
        filterItems()
    }

    override fun filterItems() {
        if (hasFinishedLoading()) {
            super.filterItems()
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.hashCode() !!.toLong()
    }

    override fun getSectionName(position: Int): String {
        return getItem(position)?.appName !!.substring(0, 1).toUpperCase(Locale.getDefault())
    }

    /**
     * Keyboard navigation code is taken from the adapter code by zevektor/Vektor (https://github.com/zevektor/KeyboardRecyclerView)
     */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        // Handle key up and key down and attempt to move selection.
        // This is unnecessary for newer API.
        if (Utils.sdkIsBelow(21)) {
            recyclerView.setOnKeyListener(View.OnKeyListener { _, keyCode, event -> // Return false if scrolled to the bounds and allow focus to move off the list.
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (isConfirmButton(event)) {
                        if (event.flags and KeyEvent.FLAG_LONG_PRESS == KeyEvent.FLAG_LONG_PRESS) {
                            recyclerView.findViewHolderForAdapterPosition(mSelectedItem)?.itemView?.performLongClick()
                        } else {
                            event.startTracking()
                        }
                        return@OnKeyListener true
                    } else {
                        if (isDown(keyCode)) {
                            return@OnKeyListener tryMoveSelection(1, recyclerView)
                        } else if (isUp(keyCode)) {
                            return@OnKeyListener tryMoveSelection(- 1, recyclerView)
                        }
                    }
                } else if (event.action == KeyEvent.ACTION_UP && isConfirmButton(event)
                    && event.flags and KeyEvent.FLAG_LONG_PRESS != KeyEvent.FLAG_LONG_PRESS
                    && mSelectedItem != - 1
                ) {
                    recyclerView.findViewHolderForAdapterPosition(mSelectedItem)?.itemView?.performClick()
                    return@OnKeyListener true
                }
                false
            })
        }
    }

    /**
     * Checks if the adapter has finished loading its data.
     * The adapter itself does not set this flag; and the flag
     * does not actually do anything internally.
     *
     * @return true if the adapter has been notified that it has finished loading.
     */
    fun hasFinishedLoading(): Boolean {
        return finishedLoading
    }

    /**
     * Notifies the adapter that its data has been loaded.
     *
     * @param finished the new state of the adapter.
     */
    fun finishedLoading(finished: Boolean) {
        finishedLoading = finished
    }

    private fun tryMoveSelection(direction: Int, recyclerView: RecyclerView): Boolean {
        val nextSelectItem = mSelectedItem + direction

        // If still within valid bounds, move the selection, notify to redraw, and scroll.
        if (nextSelectItem in 0 until itemCount) {
            notifyItemChanged(mSelectedItem)
            mSelectedItem = nextSelectItem
            notifyItemChanged(mSelectedItem)
            recyclerView.smoothScrollToPosition(mSelectedItem)
            return true
        }
        return false
    }

    companion object {
        private fun isConfirmButton(event: KeyEvent): Boolean {
            return event.keyCode == KeyEvent.KEYCODE_ENTER
        }

        private fun isUp(keycode: Int): Boolean {
            return when (keycode) {
                KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP,
                KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_UP -> true
                else -> false
            }
        }

        private fun isDown(keycode: Int): Boolean {
            return when (keycode) {
                KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN,
                KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_DOWN -> true
                else -> false
            }
        }
    }

    init {
        addListener(OnItemClickListener { _, position ->
            getItem(position)?.let { AppUtils.launchApp(recyclerView.context, it) }
            true
        })
    }
}