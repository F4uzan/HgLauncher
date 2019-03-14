package mono.hg.tasks;

import android.os.AsyncTask;
import android.view.View;

import java.lang.ref.WeakReference;

import mono.hg.MainActivity;
import mono.hg.utils.AppUtils;

/**
 * AsyncTask used to load/populate the app list.
 */
public class FetchAppsTask extends AsyncTask<Void, Void, Void> {
    private WeakReference<MainActivity> activityRef;

    public FetchAppsTask(MainActivity context) {
        activityRef = new WeakReference<>(context);
    }

    @Override
    protected void onPreExecute() {
        MainActivity activity = activityRef.get();

        if (activity != null) {
            // Clear the apps list first so we wouldn't add over an existing list.
            activity.appsList.clear();
            activity.appsAdapter.removeRange(0, activity.appsList.size());

            // Show the progress bar so the list wouldn't look empty.
            activity.loadProgress.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        MainActivity activity = activityRef.get();
        if (activity != null) {
            activity.appsList.addAll(AppUtils.loadApps(activity));
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void results) {
        MainActivity activity = activityRef.get();
        if (activity != null) {
            // Remove the progress bar.
            activity.loadProgress.setVisibility(View.GONE);
            activity.loadProgress.invalidate();

            // Add the fetched apps and update item view cache.
            activity.appsAdapter.updateDataSet(activity.appsList);
            activity.appsRecyclerView.setItemViewCacheSize(activity
                    .appsAdapter.getItemCount() - 1);

            activity.appsAdapter.finishedLoading(true);
        }
    }
}
