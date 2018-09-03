package mono.hg;

import android.graphics.drawable.Drawable;

public class AppDetail {
    private String appName, packageName;
    private Boolean isHidden;
    Drawable icon;

    public AppDetail(Drawable icon, String appName, String packageName, Boolean isHidden) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.isHidden = isHidden;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public Boolean isHidden() {
        return isHidden;
    }

    public void setHidden(Boolean hidden) {
        isHidden = hidden;
    }

    public boolean equals(Object object) {
        AppDetail alt = (AppDetail) object;
        return this == object || getPackageName().equals(alt.getPackageName());
    }
}
