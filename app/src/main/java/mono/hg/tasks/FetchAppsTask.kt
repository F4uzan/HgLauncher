package mono.hg.tasks

import android.app.Activity
import android.os.AsyncTask
import mono.hg.adapters.AppAdapter
import mono.hg.helpers.PreferenceHelper
import mono.hg.models.App
import mono.hg.utils.AppUtils
import java.lang.ref.WeakReference

/**
 * AsyncTask used to load/populate the app list.
 */
class FetchAppsTask(activity: Activity, adapter: AppAdapter, list: MutableList<App?>) :
    AsyncTask<Void?, Void?, Void?>() {
    private val activity: WeakReference<Activity> = WeakReference(activity)
    private val adapter: WeakReference<AppAdapter> = WeakReference(adapter)
    private val appsList: WeakReference<MutableList<App?>> = WeakReference(list)
    override fun onPreExecute() {
        val adapterRef = adapter.get()
        val listRef = appsList.get()
        listRef?.size?.let { adapterRef?.removeRange(0, it) }
        listRef?.clear()
    }

    override fun doInBackground(vararg params: Void?): Void? {
        val activityRef = activity.get()
        val listRef = appsList.get()
        activityRef?.let { AppUtils.loadApps(it, true) }?.let { listRef?.addAll(it) }
        return null
    }

    override fun onPostExecute(results: Void?) {
        val activityRef = activity.get()
        val adapterRef = adapter.get()
        val listRef: List<App?>? = appsList.get()

        // Add the fetched apps and update item view cache.
        adapterRef?.updateDataSet(listRef)
        listRef?.size?.minus(1)?.let { adapterRef?.recyclerView?.setItemViewCacheSize(it) }

        // Let the adapter know we're all done.
        adapterRef?.finishedLoading(true)

        // Recount the number of installed package to make sure we're up-to-date
        // when calling AppUtils.hasNewPackage()
        activityRef?.packageManager?.let { AppUtils.countInstalledPackage(it) }?.let {
            PreferenceHelper.update(
                "package_count",
                it
            )
        }
    }

}