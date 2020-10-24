package mono.hg.helpers

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.UserManager
import androidx.core.content.res.ResourcesCompat
import mono.hg.utils.AppUtils
import mono.hg.utils.Utils
import mono.hg.utils.Utils.LogLevel
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.util.*

/**
 * A class used to handle icon packs.
 * The implementation is based off of rickeythefox's code @ StackOverflow
 * (https://stackoverflow.com/questions/24937890/using-icon-packs-in-my-app)
 */
object LauncherIconHelper {
    private var mPackagesDrawables = HashMap<String?, String?>()

    private var mBackImages = ArrayList<Bitmap>()
    private var mMaskImage: Bitmap? = null
    private var mFrontImage: Bitmap? = null
    private var mFactor = 1.0f

    /**
     * Clears cached icon pack.
     */
    fun refreshIcons() {
        mFactor = 1.0f
        mBackImages.clear()
        mMaskImage = null
        mFrontImage = null
        mPackagesDrawables = HashMap()
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
    fun getIcon(
        activity: Activity,
        componentName: String,
        user: Long,
        shouldHide: Boolean
    ): Drawable? {
        return if (! shouldHide) {
            var icon = getIconDrawable(activity, componentName, user)
            if (PreferenceHelper.shadeAdaptiveIcon() &&
                (Utils.atLeastOreo() && icon is AdaptiveIconDrawable)
            ) {
                icon = drawAdaptiveShadow(activity.resources, icon)
            }
            icon
        } else {
            null
        }
    }

    private fun drawAdaptiveShadow(resources: Resources, icon: Drawable): BitmapDrawable {
        return BitmapDrawable(resources, addShadow(icon, icon.intrinsicHeight, icon.intrinsicWidth))
    }

    /**
     * Adds a shadow to a Bitmap.
     *
     * TODO: Make this return Drawable for our use case.
     *
     * @param drawable  Drawable that should be used as the foreground layer
     *                  of the shadow
     * @param dstHeight Height of the returned bitmap.
     * @param dstWidth  Width of the returned bitmap.
     *
     * @return Resulting bitmap with a shadow drawn.
     *
     * @author schwiz (https://stackoverflow.com/a/24579764)
     */
    private fun addShadow(drawable: Drawable, dstHeight: Int, dstWidth: Int): Bitmap {
        val bm = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        with(Canvas(bm)) {
            drawable.setBounds(0, 0, width, height)
            drawable.draw(this)
        }
        val mask = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ALPHA_8)
        val src = RectF(0F, 0F, bm.width.toFloat(), bm.height.toFloat())
        val dst = RectF(0F, 0F, dstWidth - 1F, dstHeight - 3F)
        val scaleToFit = Matrix().apply {
            setRectToRect(src, dst, Matrix.ScaleToFit.FILL)
        }
        val maskCanvas = Canvas(mask)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val filter = BlurMaskFilter(4.toFloat(), BlurMaskFilter.Blur.SOLID)
        val dropShadow = Matrix(scaleToFit).apply {
            postTranslate(1F, 3F)
        }

        with(paint) {
            maskCanvas.drawBitmap(bm, scaleToFit, this)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
            maskCanvas.drawBitmap(bm, dropShadow, this)
            reset()
            isAntiAlias = true
            color = Color.LTGRAY
            maskFilter = filter
            isFilterBitmap = true
        }

        return Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888).apply {
            with(Canvas(this)) {
                drawBitmap(mask, 0F, 0F, paint)
                drawBitmap(bm, scaleToFit, null)
            }
            mask.recycle()
        }
    }

    /**
     * Fetches icon from an icon pack by reading through its appfilter.xml content.
     *
     * @param packageManager PackageManager object used to fetch resources from the
     * icon pack.
     *
     * @return  1 if there is no exception thrown (a success), or 0 if otherwise. In the event of
     *          an exception, the default icon pack will be automatically loaded.
     */
    fun loadIconPack(packageManager: PackageManager): Int {
        var iconFilterXml: XmlPullParser? = null
        val iconPackageName =
            PreferenceHelper.preference.getString("icon_pack", "default") ?: "default"
        val iconRes: Resources = try {
            if ("default" != iconPackageName) {
                packageManager.getResourcesForApplication(iconPackageName)
            } else {
                // Return with a success because there's nothing to fetch.
                return 1
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Utils.sendLog(
                LogLevel.VERBOSE,
                "Cannot find icon resources for $iconPackageName!"
            )
            Utils.sendLog(LogLevel.VERBOSE, "Loading default icon.")
            return 0
        }

        // Get appfilter from the icon pack.
        try {
            val appFilterXml: Int = iconRes.getIdentifier("appfilter", "xml", iconPackageName)
            if (appFilterXml > 0) {
                iconFilterXml = iconRes.getXml(appFilterXml)
            } else {
                iconFilterXml = XmlPullParserFactory.newInstance()
                    .apply { isNamespaceAware = true }.newPullParser().also {
                        it.setInput(iconRes.assets.open("appfilter.xml"), Charsets.UTF_8.name())
                    }
            }
        } catch (e: IOException) {
            Utils.sendLog(LogLevel.ERROR, e.toString())
        } catch (e: XmlPullParserException) {
            Utils.sendLog(LogLevel.ERROR, e.toString())
        }

        // Begin parsing the received appfilter.
        iconFilterXml?.apply {
            try {
                var eventType = this.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (name) {
                            "iconback" -> {
                                for (i in 0 until attributeCount) {
                                    if (getAttributeName(i).startsWith("img")) {
                                        loadBitmap(
                                            iconRes,
                                            getAttributeValue(i),
                                            iconPackageName
                                        )?.apply {
                                            mBackImages.add(this)
                                        }
                                    }
                                }
                            }
                            "iconmask" -> {
                                if (attributeCount > 0 && getAttributeName(0) == "img1") {
                                    mMaskImage =
                                        loadBitmap(iconRes, getAttributeValue(0), iconPackageName)
                                }
                            }
                            "iconupon" -> {
                                if (attributeCount > 0 && getAttributeName(0) == "img1") {
                                    mFrontImage =
                                        loadBitmap(iconRes, getAttributeValue(0), iconPackageName)
                                }
                            }
                            "scale" -> {
                                mFactor =
                                    if (this.attributeCount > 0 && this.getAttributeName(0) == "factor")
                                        this.getAttributeValue(0).toFloat() else 1.0f
                            }
                            "item" -> {
                                var componentName: String? = null
                                var drawableName: String? = null
                                for (i in 0 until attributeCount) {
                                    if (getAttributeName(i) == "component") {
                                        componentName = getAttributeValue(i)
                                    } else if (getAttributeName(i) == "drawable") {
                                        drawableName = getAttributeValue(i)
                                    }
                                }
                                if (! mPackagesDrawables.containsKey(componentName)) {
                                    mPackagesDrawables[componentName] = drawableName
                                }
                            }
                        }
                    }
                    eventType = next()
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
     * Loads a Bitmap from icon pack.
     *
     * @param resources       Resources object to use with getIdentifier() and getDrawable().
     * @param drawableName    Name of drawable (usually package name) to load.
     * @param iconPackageName Package name of the icon pack.
     *
     * @return null if there is no such identifier associated with the name of the requested drawable.
     */
    private fun loadBitmap(
        resources: Resources,
        drawableName: String,
        iconPackageName: String
    ): Bitmap? {
        val id: Int = resources.getIdentifier(drawableName, "drawable", iconPackageName)
        if (id > 0) {
            ResourcesCompat.getDrawable(resources, id, null)
                ?.apply { if (this is BitmapDrawable) return bitmap }
        }
        return null
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
    private fun loadDrawable(
        resources: Resources?,
        drawableName: String,
        iconPackageName: String
    ): Drawable? {
        val icon: Int = resources?.getIdentifier(drawableName, "drawable", iconPackageName) ?: 0
        return if (icon > 0) {
            resources?.let { ResourcesCompat.getDrawable(it, icon, null) }
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
        val iconPackageName =
            PreferenceHelper.preference.getString("icon_pack", "default") ?: "default"
        val defaultIcon: Drawable? = if (Utils.atLeastLollipop()) {
            val launcher =
                activity.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val userManager = activity.getSystemService(Context.USER_SERVICE) as UserManager
            launcher.getActivityList(
                AppUtils.getPackageName(appPackageName),
                userManager.getUserForSerialNumber(user)
            )[0].getBadgedIcon(0)
        } else {
            ComponentName.unflattenFromString(appPackageName)?.let {
                packageManager.getActivityIcon(it)
            }
        }

        try {
            val iconRes = if ("default" != iconPackageName) {
                packageManager.getResourcesForApplication(iconPackageName)
            } else {
                return defaultIcon
            }

            val drawable = mPackagesDrawables[componentName]
            return drawable?.let { loadDrawable(iconRes, it, iconPackageName) }
                ?: defaultIcon?.let { generateBitmap(iconRes, it) }
        } catch (e: PackageManager.NameNotFoundException) {
            // The icon pack might have been removed
            // and we haven't yet set iconPackageName to 'default',
            // so as a fallback, return the default icon and re-set everything here.
            refreshIcons()
            if (iconPackageName != "default") PreferenceHelper.editor?.putString(
                "icon_pack",
                "default"
            )?.apply()
            return defaultIcon
        }
    }

    /**
     * Generates a masked [BitmapDrawable], used to facilitate
     * icon packs with a specific drawable mask (i.e, to make icons uniform).
     *
     * The codes written below are based off KISS Launcher IconsHandler:
     * (https://github.com/Neamar/KISS/blob/master/app/src/main/java/fr/neamar/kiss/IconsHandler.java)
     *
     * @param resources The resources object used to generate the new [BitmapDrawable]
     * @param icon      The base icon that will be transformed with the icon pack's mask.
     *
     * @return Drawable A [BitmapDrawable] if the icon pack has the necessary icon
     *                  background. The original Drawable otherwise.
     */
    private fun generateBitmap(resources: Resources?, icon: Drawable): Drawable {
        // Return the original icon when the icon pack doesn't provide any
        // background image/mask.
        val iconPackageName =
            PreferenceHelper.preference.getString("icon_pack", "default") ?: "default"

        if (mBackImages.size == 0 || iconPackageName == "default") return icon

        val defaultBitmap = Bitmap.createBitmap(
            icon.intrinsicWidth,
            icon.intrinsicHeight, Bitmap.Config.ARGB_8888
        )

        with(Canvas(defaultBitmap)) {
            icon.setBounds(0, 0, width, height)
            icon.draw(this)
        }

        val backImage = mBackImages[Random().nextInt(mBackImages.size)]
        val width = backImage.width
        val height = backImage.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val mCanvas = Canvas(result).apply { density = Bitmap.DENSITY_NONE }

        // Draw the background first
        mCanvas.drawBitmap(backImage, 0f, 0f, null)

        // Create a mutable mask bitmap with the same mask
        val scaledBitmap = Bitmap.createScaledBitmap(
            defaultBitmap,
            (width * mFactor).toInt(),
            (height * mFactor).toInt(),
            false
        ).apply { density = Bitmap.DENSITY_NONE }

        mMaskImage?.apply {
            // Draw the scaled bitmap with mask
            val matScale = Matrix()
            matScale.setScale(width / this.width.toFloat(), height / this.height.toFloat())

            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            }

            mCanvas.drawBitmap(
                scaledBitmap,
                (width - scaledBitmap.width) / 2f,
                (height - scaledBitmap.height) / 2f,
                null
            )

            mCanvas.drawBitmap(this, matScale, paint)
            paint.xfermode = null
        } ?: run {
            // Draw the back image as a mask to the scaled bitmap.
            mCanvas.drawBitmap(
                scaledBitmap,
                (width - scaledBitmap.width) / 2f,
                (height - scaledBitmap.height) / 2f,
                null
            )
        }

        // Apply the front image bitmap.
        mFrontImage?.apply { mCanvas.drawBitmap(this, 0f, 0f, null) }

        return BitmapDrawable(resources, result)
    }
}