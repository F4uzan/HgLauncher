package mono.hg.tasks;

import android.content.pm.PackageManager;
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
    private WeakReference<PackageManager> packageManager;
    private WeakReference<AppAdapter> adapter;
    private WeakReference<List<App>> appsList;

    public FetchAppsTask(PackageManager manager, AppAdapter adapter, List<App> list) {
        this.packageManager = new WeakReference<>(manager);
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
        PackageManager managerRef = packageManager.get();
        List<App> listRef = appsList.get();

        if (managerRef != null && listRef != null) {
            listRef.addAll(AppUtils.loadApps(managerRef));
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
