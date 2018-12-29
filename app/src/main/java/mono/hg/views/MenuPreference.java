package mono.hg.views;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.PopupMenu;

public class MenuPreference extends ListPreference {
    private View anchor;

    public MenuPreference(Context context) {
        super(context);
    }

    public MenuPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override protected View onCreateView(ViewGroup parent) {
        return anchor = super.onCreateView(parent);
    }

    @Override protected void showDialog(Bundle state) {
        final PopupMenu popup = new PopupMenu(getContext(), anchor, Gravity.TOP);
        final Menu menu = popup.getMenu();

        for (int i = 0; i < getEntries().length; i++) {
            MenuItem item = menu.add(1, i, Menu.NONE, getEntries()[i]);
            item.setChecked(item.getTitle().equals(getEntry())); // 1
        }
        menu.setGroupCheckable(1, true, true); // 2

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                popup.dismiss();
                String value = getEntryValues()[item.getItemId()].toString();
                String entry = getEntries()[item.getItemId()].toString();
                if (callChangeListener(value)) {
                    setValue(value);
                    setSummary(entry);
                }
                return true;
            }
        });
        popup.show();
    }

}
