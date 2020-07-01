package mono.hg.utils

import android.content.Context
import android.os.Process
import android.os.UserHandle
import android.os.UserManager

class UserUtils(context: Context) {
    private var userManager: UserManager? = null
    fun getSerial(user: UserHandle?): Long {
        return if (Utils.sdkIsAround(17)) {
            userManager!!.getSerialNumberForUser(user)
        } else {
            0
        }
    }

    fun getUser(serial: Long): UserHandle? {
        return if (Utils.sdkIsAround(17)) {
            userManager!!.getUserForSerialNumber(serial)
        } else {
            null
        }
    }

    val currentSerial: Long
        get() = if (Utils.sdkIsAround(17)) {
            getSerial(currentUser)
        } else {
            0
        }

    val currentUser: UserHandle?
        get() = if (Utils.sdkIsAround(17)) {
            Process.myUserHandle()
        } else {
            null
        }

    init {
        if (Utils.sdkIsAround(17)) {
            userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        }
    }
}