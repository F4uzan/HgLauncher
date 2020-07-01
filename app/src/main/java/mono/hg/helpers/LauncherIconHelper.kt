package mono.hg.helpers

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.UserManager
import mono.hg.utils.AppUtils
import mono.hg.utils.Utils
import mono.hg.utils.Utils.LogLevel
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream
import java.util.*

/*
 * A class used to handle icon packs.
 * The implementation is based off of rickeythefox's code @ StackOverflow
 * (https://stackoverflow.com/questions/24937890/using-icon-packs-in-my-app)
 */
object LauncherIconHelper {
    private val mPackagesDrawables = HashMap<String?, String?>()
    private var iconPackageName = PreferenceHelper.preference
            .getString("icon_pack", "default")

    /**
     * Clears cached icon pack.
     */
    fun refreshIcons() {
        iconPackageName = PreferenceHelper.preference.getString("icon_pack", "default")
        mPackagesDrawables.clear()
    }

    /**
     * Retrieve an icon for a component name, applying user preferences
     * such as shaded adaptive icons and icon pack.
     *
     * @param activity      Activity where LauncherApps service can be retrieved.
     * @param componentName Component name of the activity.
     * @param user          The numerical representation of the user.
     * @param shouldHide    Whether we are even needed at all.
     *
     * @return Drawable of the icon.
     */
    fun getIcon(activity: Activity, componentName: String, user: Long, shouldHide: Boolean): Drawable? {
        var icon: Drawable? = null
        if (!shouldHide) {
            icon = getIconDrawable(activity, componentName, user)
            if (PreferenceHelper.appTheme() == "light" && PreferenceHelper.shadeAdaptiveIcon()
                    && (Utils.atLeastOreo()
                            && icon is AdaptiveIconDrawable)) {
                icon = drawAdaptiveShadow(icon)
            }
        }
        return icon
    }

    /**
     * Draws a shadow below a drawable.
     *
     * @param icon Foreground layer to which the shadows will be drawn.
     *
     * @return BitmapDrawable masked with shadow.
     */
    private fun drawAdaptiveShadow(icon: Drawable): BitmapDrawable {
        return BitmapDrawable(
                addShadow(icon, icon.intrinsicHeight, icon.intrinsicWidth,
                        Color.LTGRAY, 4, 1f, 3f))
    }

    /**
     * Adds a shadow to a Bitmap.
     *
     *
     * TODO: Make this return Drawable for our use case.
     *
     * @param drawable  Drawable that should be used as the foreground layer
     * of the shadow.
     * @param dstHeight Height of the returned bitmap.
     * @param dstWidth  Width of the returned bitmap.
     * @param color     Colour of the drawn shadow.
     * @param size      Size of the drawn shadow.
     * @param dx        Shadow x direction.
     * @param dy        Shadow y direction.
     *
     * @return Bitmap with resulting shadow.
     *
     * @author schwiz (https://stackoverflow.com/a/24579764)
     */
    private fun addShadow(drawable: Drawable, dstHeight: Int, dstWidth: Int, color: Int, size: Int, dx: Float, dy: Float): Bitmap {
        val bm = Bitmap.createBitmap(drawable.intrinsicWidth,
                drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        val mask = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ALPHA_8)
        val scaleToFit = Matrix()
        val src = RectF(0F, 0F, bm.width.toFloat(), bm.height.toFloat())
        val dst = RectF(0F, 0F, dstWidth - dx, dstHeight - dy)
        scaleToFit.setRectToRect(src, dst, Matrix.ScaleToFit.FILL)
        val dropShadow = Matrix(scaleToFit)
        dropShadow.postTranslate(dx, dy)
        val maskCanvas = Canvas(mask)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        maskCanvas.drawBitmap(bm, scaleToFit, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
        maskCanvas.drawBitmap(bm, dropShadow, paint)
        val filter = BlurMaskFilter(size.toFloat(), BlurMaskFilter.Blur.SOLID)
        paint.reset()
        paint.isAntiAlias = true
        paint.color = color
        paint.maskFilter = filter
        paint.isFilterBitmap = true
        val ret = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
        val retCanvas = Canvas(ret)
        retCanvas.drawBitmap(mask, 0f, 0f, paint)
        retCanvas.drawBitmap(bm, scaleToFit, null)
        mask.recycle()
        return ret
    }

    /**
     * Fetches icon from an icon pack by reading through its appfilter.xml content.
     *
     * @param packageManager PackageManager object used to fetch resources from the
     * icon pack.
     */
    fun loadIconPack(packageManager: PackageManager): Int {
        var iconFilterXml: XmlPullParser? = null
        lateinit var iconRes: Resources
        iconRes = try {
            if ("default" != iconPackageName) {
                packageManager.getResourcesForApplication(iconPackageName)
            } else {
                // Return with a success because there's nothing to fetch.
                return 1
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Utils.sendLog(LogLevel.VERBOSE,
                    "Cannot find icon resources for $iconPackageName!")
            Utils.sendLog(LogLevel.VERBOSE, "Loading default icon.")
            return 0
        }

        // Get appfilter from the icon pack.
        try {
            val iconAsset: InputStream
            var appFilterXml = 0
            appFilterXml = iconRes.getIdentifier("appfilter", "xml", iconPackageName)
            if (appFilterXml > 0) {
                iconFilterXml = iconRes.getXml(appFilterXml)
            } else {
                iconAsset = iconRes.assets.open("appfilter.xml")
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                iconFilterXml = factory.newPullParser()
                iconFilterXml.setInput(iconAsset, "utf-8")
            }
        } catch (e: IOException) {
            Utils.sendLog(LogLevel.ERROR, e.toString())
        } catch (e: XmlPullParserException) {
            Utils.sendLog(LogLevel.ERROR, e.toString())
        }

        // Begin parsing the received appfilter.
        if (iconFilterXml != null) {
            try {
                var eventType = iconFilterXml.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (iconFilterXml.name == "item") {
                            var componentName: String? = null
                            var drawableName: String? = null
                            for (i in 0 until iconFilterXml.attributeCount) {
                                if (iconFilterXml.getAttributeName(i) == "component") {
                                    componentName = iconFilterXml.getAttributeValue(i)
                                } else if (iconFilterXml.getAttributeName(i) == "drawable") {
                                    drawableName = iconFilterXml.getAttributeValue(i)
                                }
                            }
                            if (!mPackagesDrawables.containsKey(componentName)) {
                                mPackagesDrawables[componentName] = drawableName
                            }
                        }
                    }
                    eventType = iconFilterXml.next()
                }
            } catch (e: IOException) {
                Utils.sendLog(LogLevel.ERROR, e.toString())
            } catch (e: XmlPullParserException) {
                Utils.sendLog(LogLevel.ERROR, e.toString())
            }
        }
        return 1
    }

    /**
     * Loads drawable from icon pack.
     *
     * @param resources       Resources object to use with getIdentifier() and getDrawable().
     * @param drawableName    Name of drawable (usually package name) to load.
     * @param iconPackageName Package name of the icon pack.
     *
     * @return null if there is no such icon associated with the name of the requested drawable.
     */
    private fun loadDrawable(resources: Resources, drawableName: String, iconPackageName: String?): Drawable? {
        val icon = resources.getIdentifier(drawableName, "drawable", iconPackageName)
        return if (icon > 0) {
            resources.getDrawable(icon)
        } else null
    }

    /**
     * Loads an icon from the icon pack based on the received package name.
     *
     * @param activity       where LauncherApps service can be retrieved.
     * @param appPackageName Package name of the app whose icon is to be loaded.
     *
     * @return Drawable Will return null if there is no icon associated with the package name,
     * otherwise an associated icon from the icon pack will be returned.
     */
    private fun getIconDrawable(activity: Activity, appPackageName: String, user: Long): Drawable? {
        val packageManager = activity.packageManager
        val componentName = "ComponentInfo{$appPackageName}"
        var iconRes: Resources? = null
        var defaultIcon: Drawable? = null
        try {
            if (Utils.atLeastLollipop()) {
                val launcher = activity.getSystemService(
                        Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val userManager = activity.getSystemService(
                        Context.USER_SERVICE) as UserManager
                if (userManager != null && launcher != null) {
                    defaultIcon = launcher.getActivityList(AppUtils.getPackageName(appPackageName),
                            userManager.getUserForSerialNumber(user))[0].getBadgedIcon(0)
                }
            } else {
                defaultIcon = packageManager.getActivityIcon(
                        ComponentName.unflattenFromString(appPackageName))
            }
            iconRes = if ("default" != iconPackageName) {
                packageManager.getResourcesForApplication(iconPackageName)
            } else {
                return defaultIcon
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Utils.sendLog(LogLevel.Companion.ERROR, e.toString())
        }
        val drawable = mPackagesDrawables[componentName]
        return if (drawable != null && iconRes != null) {
            loadDrawable(iconRes, drawable, iconPackageName)
        } else {
            defaultIcon
        }
    }
}