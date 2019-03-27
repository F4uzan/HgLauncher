package mono.hg.helpers;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import mono.hg.utils.Utils;

/*
 * A class used to handle icon packs.
 * The implementation is based off of rickeythefox's code @ StackOverflow
 * (https://stackoverflow.com/questions/24937890/using-icon-packs-in-my-app)
 */

public class LauncherIconHelper {
    private static HashMap<String, String> mPackagesDrawables = new HashMap<>();
    private static String iconPackageName = PreferenceHelper.getIconPackName();

    /**
     * Clears cached icon pack.
     */
    public static void clearDrawableCache() {
        mPackagesDrawables.clear();
    }

    /**
     * Draws a shadow below a drawable.
     *
     * @param icon Foreground layer to which the shadows will be drawn.
     *
     * @return BitmapDrawable masked with shadow.
     */
    public static BitmapDrawable drawAdaptiveShadow(final Drawable icon) {
        return new BitmapDrawable(
                addShadow(icon, icon.getIntrinsicHeight(), icon.getIntrinsicWidth(),
                        Color.LTGRAY, 4, 1, 3));
    }

    /**
     * Adds a shadow to a Bitmap.
     * <p>
     * TODO: Make this return Drawable for our use case.
     *
     * @param drawable  Drawable that should be used as the foreground layer
     *                  of the shadow.
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
    private static Bitmap addShadow(final Drawable drawable, final int dstHeight, final int dstWidth, int color, int size, float dx, float dy) {
        final Bitmap bm = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bm);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        final Bitmap mask = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ALPHA_8);

        final Matrix scaleToFit = new Matrix();
        final RectF src = new RectF(0, 0, bm.getWidth(), bm.getHeight());
        final RectF dst = new RectF(0, 0, dstWidth - dx, dstHeight - dy);
        scaleToFit.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);

        final Matrix dropShadow = new Matrix(scaleToFit);
        dropShadow.postTranslate(dx, dy);

        final Canvas maskCanvas = new Canvas(mask);
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskCanvas.drawBitmap(bm, scaleToFit, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        maskCanvas.drawBitmap(bm, dropShadow, paint);

        final BlurMaskFilter filter = new BlurMaskFilter(size, BlurMaskFilter.Blur.SOLID);
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setMaskFilter(filter);
        paint.setFilterBitmap(true);

        final Bitmap ret = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888);
        final Canvas retCanvas = new Canvas(ret);
        retCanvas.drawBitmap(mask, 0, 0, paint);
        retCanvas.drawBitmap(bm, scaleToFit, null);
        mask.recycle();
        return ret;
    }

    /**
     * Fetches icon from an icon pack by reading through its appfilter.xml content.
     *
     * @param packageManager PackageManager object used to fetch resources from the
     *                       icon pack.
     */
    public static int loadIconPack(PackageManager packageManager) {
        XmlPullParser iconFilterXml = null;
        Resources iconRes;

        try {
            iconRes = packageManager.getResourcesForApplication(iconPackageName);
        } catch (PackageManager.NameNotFoundException e) {
            Utils.sendLog(1, "Cannot find icon resources for " + iconPackageName + "!");
            Utils.sendLog(1, "Loading default icon.");
            return 0;
        }

        // Get appfilter from the icon pack.
        try {
            InputStream iconAsset;
            int appFilterXml = 0;

            if (iconRes != null) {
                appFilterXml = iconRes.getIdentifier("appfilter", "xml", iconPackageName);
            }

            if (appFilterXml > 0) {
                iconFilterXml = iconRes.getXml(appFilterXml);
            } else {
                iconAsset = Utils.requireNonNull(iconRes).getAssets().open("appfilter.xml");
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                iconFilterXml = factory.newPullParser();
                iconFilterXml.setInput(iconAsset, "utf-8");
            }
        } catch (IOException | XmlPullParserException e) {
            Utils.sendLog(3, e.toString());
        }

        // Begin parsing the received appfilter.
        if (iconFilterXml != null) {
            try {
                int eventType = iconFilterXml.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (iconFilterXml.getName().equals("item")) {
                            String componentName = null;
                            String drawableName = null;

                            for (int i = 0; i < iconFilterXml.getAttributeCount(); i++) {
                                if (iconFilterXml.getAttributeName(i).equals("component")) {
                                    componentName = iconFilterXml.getAttributeValue(i);
                                } else if (iconFilterXml.getAttributeName(i).equals("drawable")) {
                                    drawableName = iconFilterXml.getAttributeValue(i);
                                }
                            }

                            if (!mPackagesDrawables.containsKey(componentName)) {
                                mPackagesDrawables.put(componentName, drawableName);
                            }
                        }
                    }
                    eventType = iconFilterXml.next();
                }
            } catch (IOException | XmlPullParserException e) {
                Utils.sendLog(3, e.toString());
            }
        }
        return 1;
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
    private static Drawable loadDrawable(Resources resources, String drawableName, String iconPackageName) {
        int icon = resources.getIdentifier(drawableName, "drawable", iconPackageName);
        if (icon > 0) {
            return resources.getDrawable(icon);
        }
        return null;
    }

    /**
     * Loads an icon from the icon pack based on the received package name.
     *
     * @param packageManager PackageManager object to determine the launch intent of
     *                       the package name.
     * @param appPackageName Package name of the app whose icon is to be loaded.
     *
     * @return Drawable Will return null if there is no icon associated with the package name,
     * otherwise an associated icon from the icon pack will be returned.
     */
    // Load icon from the cached appfilter.
    public static Drawable getIconDrawable(PackageManager packageManager, String appPackageName) {
        String componentName = "ComponentInfo{" + appPackageName + "}";
        Resources iconRes = null;

        try {
            iconRes = packageManager.getResourcesForApplication(iconPackageName);
        } catch (PackageManager.NameNotFoundException e) {
            Utils.sendLog(3, e.toString());
        }

        String drawable = mPackagesDrawables.get(componentName);
        if (drawable != null && iconRes != null) {
            // Load and return.
            return loadDrawable(iconRes, drawable, iconPackageName);
        }
        return null;
    }
}
