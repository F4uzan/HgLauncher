package mono.hg.utils;

import android.content.Context;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;

public class UserUtils {
    private UserManager userManager;

    public UserUtils(Context context) {
        if (Utils.sdkIsAround(17)) {
            this.userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        }
    }

    public long getSerial(UserHandle user) {
        if (Utils.sdkIsAround(17)) {
            return userManager.getSerialNumberForUser(user);
        } else {
            return 0;
        }
    }

    public UserHandle getUser(long serial) {
        if (Utils.sdkIsAround(17)) {
            return userManager.getUserForSerialNumber(serial);
        } else {
            return null;
        }
    }

    public long getCurrentSerial() {
        if (Utils.sdkIsAround(17)) {
            return getSerial(getCurrentUser());
        } else {
            return 0;
        }
    }

    public UserHandle getCurrentUser() {
        if (Utils.sdkIsAround(17)) {
            return Process.myUserHandle();
        } else {
            return null;
        }
    }
}
