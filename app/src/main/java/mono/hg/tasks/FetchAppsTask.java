package mono.hg.tasks;

import android.app.Activity;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.List;

import mono.hg.adapters.AppAdapter;
import mono.hg.models.App;
import mono.hg.utils.AppUtils;

/**
 * AsyncTask used to load/populate the app list.
 */
public class FetchAppsTask extends AsyncTask<Void, Void, Void> {
    private WeakReference<Activity> activity;
    private WeakReference<AppAdapter> adapter;
    private WeakReference<List<App>> appsList;

    public FetchAppsTask(Activity activity, AppAdapter adapter, List<App> list) {
        this.activity = new WeakReference<>(activity);
        this.adapter = new WeakReference<>(adapter);
        this.appsList = new WeakReference<>(list);
    }

    @Override
    protected void onPreExecute() {
        AppAdapter adapterRef = adapter.get();
        List<App> listRef = appsList.get();

        if (adapterRef != null && listRef != null) {
            adapterRef.removeRange(0, listRef.size());
            listRef.clear();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        Activity activityRef = activity.get();
        List<App> listRef = appsList.get();

        if (activityRef != null && listRef != null) {
            listRef.addAll(AppUtils.loadApps(activityRef, true));
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void results) {
        AppAdapter adapterRef = adapter.get();
        List<App> listRef = appsList.get();

        if (adapterRef != null && listRef != null) {
            // Add the fetched apps and update item view cache.
            adapterRef.updateDataSet(listRef);
            adapterRef.getRecyclerView().setItemViewCacheSize(listRef.size() - 1);
            adapterRef.finishedLoading(true);
        }
    }
}
