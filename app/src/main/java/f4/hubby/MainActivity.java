package f4.hubby;

import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import f4.hubby.helpers.RecyclerClick;

public class MainActivity extends AppCompatActivity {

    boolean anim, icon_hide, list_order, wallpaper_hide, shade_view, fab_view;
    String launch_anim;
    String title_style;
    private List<AppDetail> appList = new ArrayList<>();
    private PackageManager manager;
    private AppAdapter apps = new AppAdapter(appList);
    private RecyclerView list;
    private AppBarLayout appBarLayout;
    private CollapsingToolbarLayout toolbarLayout;
    private SharedPreferences prefs;

    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        appBarLayout = findViewById(R.id.app_bar);
        toolbarLayout = findViewById(R.id.toolbar_layout);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);

        list = findViewById(R.id.apps_list);
        list.setAdapter(apps);
        list.setLayoutManager(mLayoutManager);
        list.setItemAnimator(new DefaultItemAnimator());

        refreshWallpaper();
        loadPref();

        fab = findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (anim) {
                        appBarLayout.setExpanded(false, true);
                    } else {
                        appBarLayout.setExpanded(false, false);
                    }
                }
            });
        }

        loadApps();
        addClickListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.update_wallpaper) {
            Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
            startActivity(Intent.createChooser(intent, getString(R.string.action_wallpaper)));
            refreshWallpaper();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadApps() {
        manager = getPackageManager();

        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        loadPref();

        List<ResolveInfo> availableActivities = manager.queryIntentActivities(i, 0);
        if (list_order) {
            Collections.sort(availableActivities, Collections
                    .reverseOrder(new ResolveInfo.DisplayNameComparator(manager)));
        } else {
            Collections.sort(availableActivities, new ResolveInfo.DisplayNameComparator(manager));
        }
        for (ResolveInfo ri : availableActivities) {
            String label = ri.loadLabel(manager).toString();
            String name = ri.activityInfo.packageName;
            Drawable icon = ri.activityInfo.loadIcon(manager);
            AppDetail app = new AppDetail(icon, label, name);
            appList.add(app);
            apps.notifyItemInserted(appList.size() - 1);
        }
    }

    private void addClickListener() {
        RecyclerClick.addTo(list).setOnItemClickListener(new RecyclerClick.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                Intent i = manager.getLaunchIntentForPackage(appList.get(position).getName().toString());
                switch (launch_anim) {
                    case "default":
                        MainActivity.this.startActivity(i);
                        break;
                    case "pull_up":
                        MainActivity.this.startActivity(i);
                        overridePendingTransition(R.anim.pull_up, 0);
                        break;
                    case "slide_in":
                        MainActivity.this.startActivity(i);
                        overridePendingTransition(R.anim.slide_in, 0);
                        break;
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPref();
        refreshWallpaper();
        appBarLayout.setExpanded(true, false);
    }

    private void refreshWallpaper() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        Drawable wallpaperDrawable = wallpaperManager.getDrawable();

        AppCompatImageView homePaper = findViewById(R.id.homePaper);
        RelativeLayout shade = findViewById(R.id.shade);
        if (homePaper != null) {
            if (!wallpaper_hide) {
                homePaper.setImageDrawable(wallpaperDrawable);
            } else {
                homePaper.setImageDrawable(null);
            }
        }

        if (shade_view) {
            shade.setVisibility(View.VISIBLE);
        } else {
            shade.setVisibility(View.GONE);
        }

        if (fab != null) {
            if (fab_view) {
                fab.setVisibility(View.VISIBLE);
            } else {
                fab.setVisibility(View.GONE);
            }
        }
    }

    private void loadPref() {
        String title = prefs.getString("title_text", getString(R.string.pref_title_default));
        launch_anim = prefs.getString("launch_anim", "default");
        title_style = prefs.getString("title_style", "default");
        anim = prefs.getBoolean("anim_switch", true);
        icon_hide = prefs.getBoolean("icon_hide_switch", false);
        list_order = prefs.getString("list_order", "alphabetical").equals("invertedAlphabetical");
        wallpaper_hide = prefs.getBoolean("wall_hide_switch", false);
        shade_view = prefs.getBoolean("shade_view_switch", false);
        fab_view = prefs.getBoolean("fab_view", true);
        toolbarLayout.setTitle(title);
        switch (title_style) {
            case "bold":
                toolbarLayout.setExpandedTitleTextAppearance(R.style.MaterialTextAppearance_Title);
                break;
            case "headline":
                toolbarLayout.setExpandedTitleTextAppearance(R.style.MaterialTextAppearance_Headline);
                break;
            case "default":
                toolbarLayout.setExpandedTitleTextAppearance(R.style.MaterialTextAppearance_Display1);
                break;
            case "expanded":
                toolbarLayout.setExpandedTitleTextAppearance(R.style.MaterialTextAppearance_Display2);
                break;
            case "huge":
                toolbarLayout.setExpandedTitleTextAppearance(R.style.MaterialTextAppearance_Display3);
                break;
        }
    }
}
