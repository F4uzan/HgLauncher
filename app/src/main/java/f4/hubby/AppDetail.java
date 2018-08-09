package f4.hubby;

import android.graphics.drawable.Drawable;

public class AppDetail {
    String label, name;
    Drawable icon;

    AppDetail(Drawable icon, String label, String name) {
        this.name = name;
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public CharSequence getName() {
        return name;
    }

    public Drawable getIcon() {
        return icon;
    }
}
