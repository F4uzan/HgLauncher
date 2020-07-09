package mono.hg.utils

import android.content.Context
import android.os.Process
import android.os.UserHandle
import android.os.UserManager

/**
 * Utils class handling retrieval of UserHandle and its serial number.
 */
class UserUtils(context: Context) {
    private var userManager: UserManager? = null

    /**
     * Retrieves the serial for a UserHandle.
     *
     * @param user The UserHandle itself.
     */
    fun getSerial(user: UserHandle?): Long {
        return if (Utils.sdkIsAround(17)) {
            userManager !!.getSerialNumberForUser(user)
        } else {
            0
        }
    }

    /**
     * Retrieves the current user from the provided serial number.
     *
     * @param serial The serial number for the user.
     */
    fun getUser(serial: Long): UserHandle? {
        return if (Utils.sdkIsAround(17)) {
            userManager !!.getUserForSerialNumber(serial)
        } else {
            null
        }
    }

    /**
     * Retrieves the serial for the currently logged-in user.
     *
     * @return Long The serial number itself, 0 on API level older than 17.
     */
    val currentSerial: Long
        get() = if (Utils.sdkIsAround(17)) {
            getSerial(currentUser)
        } else {
            0
        }

    /**
     * Retrieves the UserHandle for the currently logged-in user.
     *
     * @return UserHandle The UserHandle itself, null on API level older than 17.
     */
    val currentUser: UserHandle?
        get() = if (Utils.sdkIsAround(17)) {
            Process.myUserHandle()
        } else {
            null
        }

    init {
        // There is no point to running this class before the advent of multi-user.
        if (Utils.sdkIsAround(17)) {
            userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        }
    }
}