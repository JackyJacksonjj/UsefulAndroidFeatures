package useful.be.can.useful.utils;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * Created by jenea on 8/8/16.
 */
public class ImageFetcher {

    private static ImageFetcher imageFetcherForRareUse;
    private Context context;
    private int prefWidth, prefHeight, drawableId;
    private boolean animate, isSizeFixed = false;
    public static WeakHashMap<Integer, Bitmap> bitmapHashMap;

    public ImageFetcher(Context context){
        this.context = context;
        if(bitmapHashMap == null)
            bitmapHashMap = new WeakHashMap<>();
    }

    public void setPreferableSize(int width, int height){
        prefHeight = height;
        prefWidth = width;
    }

    public void setPreferableSize(int side){
        prefHeight = side;
        prefWidth = side;
    }

    public void setPreferableSize(View view){
        int width = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
        int height = view.getHeight() - view.getPaddingTop() - view.getPaddingBottom();
        prefWidth = prefHeight = Math.min(width > 0 ? width : 0, height > 0 ? height : 0);

        if(prefWidth == 0) {
            prefWidth = prefHeight = HelperClass.getPx(context, 25);
        }
    }

    public static ImageFetcher getImageFetcherForRareUse(Context context) {
        if(imageFetcherForRareUse == null)
            imageFetcherForRareUse = new ImageFetcher(context);
        return imageFetcherForRareUse;
    }

    public void setSizeFixed(boolean isSizeFixed){
        this.isSizeFixed = isSizeFixed;
    }

    public int getPrefWidth() {
        return prefWidth;
    }

    public int getPrefHeight() {
        return prefHeight;
    }

    public static int getPrefSizeForTarget(View target){
        int leftRightHeight = target.getHeight() - target.getPaddingLeft() - target.getPaddingRight();
        int topBottomWidth = target.getWidth() - target.getPaddingTop() - target.getPaddingBottom();
        return Math.min(leftRightHeight,topBottomWidth);
    }

    public void loadBitmap(int resId, ImageView target){
        if(!isSizeFixed) {
            int width = target.getWidth() - target.getPaddingLeft() - target.getPaddingRight();
            int height = target.getHeight() - target.getPaddingTop() - target.getPaddingBottom();
            prefWidth = prefHeight = Math.min(width > 0 ? width : 0, height > 0 ? height : 0);
        }

        if(prefWidth == 0 || prefHeight == 0) {
            prefWidth = prefHeight = HelperClass.getPx(context, 25);
        }

        target.setImageBitmap(decodeSampledBitmapFromResource(context.getResources(),resId));
    }

    public void loadBitmap(int drawableId, ImageView mPlaceHolder, boolean animate, int ... specialIntegerArr){
        this.animate = animate;
        int specialInteger = 0;
        if(specialIntegerArr != null && specialIntegerArr.length > 0)
            specialInteger = specialIntegerArr[0];

        if(specialInteger != 0 && bitmapHashMap.containsKey(specialInteger)){
            mPlaceHolder.setImageBitmap(bitmapHashMap.get(specialInteger));
        } else if (cancelPotentialWork(drawableId, mPlaceHolder)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(mPlaceHolder);
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(context.getResources(), Bitmap.createBitmap(new int[]{Color.WHITE},1,1, Bitmap.Config.RGB_565), task);
            mPlaceHolder.setImageDrawable(asyncDrawable);
            task.execute(drawableId, specialInteger);
        }
    }

    public static boolean cancelPotentialWork(int data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final int bitmapData = bitmapWorkerTask.data;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == 0 || bitmapData != data) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }


    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private int data = 0, specialInteger = 0;
        private int width, height;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
            width = imageView.getWidth() - imageView.getPaddingLeft() - imageView.getPaddingRight();

            if(width <= 0)
                width = prefWidth;

            height = imageView.getHeight() - imageView.getPaddingBottom() - imageView.getPaddingTop();

            if(height <= 0)
                height = prefHeight;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params) {
            data = params[0];
            specialInteger = params[1];
            prefHeight = height;
            prefWidth = width;
            return decodeSampledBitmapFromResource(context.getResources(), data);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask =
                        getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask) {
                    imageView.setImageBitmap(bitmap);

                    if(specialInteger != 0 && !bitmapHashMap.containsKey(specialInteger)){
                        bitmapHashMap.put(specialInteger, bitmap);
                    }

                    if(animate){
                        ValueAnimator anim = ObjectAnimator.ofFloat(imageView, "alpha", 0f,1f);
                        anim.setDuration(324);
                        anim.start();
                    }
                }
            }
        }
    }

    public BitmapDrawable getBitmapDrawable(Resources res, int resId){
        if(prefWidth <= 0 || prefHeight <= 0){
            prefHeight = prefWidth = HelperClass.getPx(context, 25);
        }
        return new BitmapDrawable(decodeSampledBitmapFromResource(res, resId));
    }

    public BitmapDrawable getBitmapDrawable(Resources res, int resId, int prefWidth, int prefHeight){
        if(prefWidth <= 0 || prefHeight <= 0){
            this.prefHeight = this.prefWidth = HelperClass.getPx(context, 25);
        } else {
            this.prefHeight = prefHeight;
            this.prefWidth = prefWidth;
        }
        return new BitmapDrawable(decodeSampledBitmapFromResource(res, resId));
    }

    public Bitmap decodeSampledBitmapFromResource(Resources res, int resId) {
        return decodeSampledBitmapFromResource(res, resId, prefWidth,prefHeight);
    }

    public Bitmap decodeSampledBitmapFromResource (Resources res, int resId, int prefWidth, int prefHeight) {
        if(bitmapHashMap.containsKey(resId) && bitmapHashMap.get(resId) != null){
            return bitmapHashMap.get(resId);
        }

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, prefWidth, prefHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        bitmapHashMap.put(resId, BitmapFactory.decodeResource(res, resId, options));
        return bitmapHashMap.get(resId);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
