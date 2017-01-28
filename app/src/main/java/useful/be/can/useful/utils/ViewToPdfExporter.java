package useful.be.can.useful.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jenea on 11/1/16.
 */
public class ViewToPdfExporter {

    private static final int END_TAG = 989;
    private OnPdfDocumentUpdateListener listener;
    private ArrayList<Fragment> fragments;
    private DetailedSession context;
    private String pathToPdf;
    private boolean share, fileExists;
    private Document document;
    private PdfWriter writer;
    private float perfectHeight = -1, x = 0, y = 0;

    public static ViewToPdfExporter getInstance(@NonNull String pathToPdf, @NonNull ArrayList<Fragment> mFragments, @NonNull Listener context, boolean toShare, boolean fileExists) {
        ViewToPdfExporter viewToPdfExporter = new ViewToPdfExporter();

        viewToPdfExporter.setListener(context);
        viewToPdfExporter.setFragments(mFragments);
        viewToPdfExporter.setPathToPdf(pathToPdf);
        viewToPdfExporter.setContext(context);
        viewToPdfExporter.setShare(toShare);
        viewToPdfExporter.setFileExists(fileExists);
        viewToPdfExporter.implement();

        return viewToPdfExporter;
    }

    public static Bitmap getBitmapFromView(View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null)
            bgDrawable.draw(canvas);
        else
            canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        return returnedBitmap;
    }

    private void implement() {
        document = new Document();
        try {
            if (!isFileExists())
                writer = PdfWriter.getInstance(document, new FileOutputStream(pathToPdf, false));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getWholeListViewItemsToBitmap(int index, RecyclerView mFocusedListView) {
        RecyclerView.Adapter adapter = mFocusedListView.getAdapter();
        int itemsCount = adapter.getItemCount();
        int allItemsHeight = 0;
        List<Bitmap> bitmaps = new ArrayList<>();

        for (int i = 0; i < itemsCount; i++) {
            //get each item of the list views
            View childView = andPutItHere;
            childView.measure(View.MeasureSpec.makeMeasureSpec(mFocusedListView.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

            childView.layout(0, 0, childView.getMeasuredWidth(), childView.getMeasuredHeight());
            childView.setDrawingCacheEnabled(true);
            childView.buildDrawingCache();
            bitmaps.add(scaleBitmap(childView.getDrawingCache()));
            allItemsHeight += bitmaps.get(i).getHeight();
            Log.d(ViewToPdfExporter.class.getSimpleName(), String.format("Item %d height = %d, width = %d", i, bitmaps.get(i).getHeight(), bitmaps.get(i).getWidth()));
        }
        Log.d(ViewToPdfExporter.class.getSimpleName(), "All items height = " + Integer.toString(allItemsHeight));

        if (bitmaps.size() > 1 && allItemsHeight > (bitmaps.get(0).getHeight() + bitmaps.get(1).getHeight() * 50) * 1.5f) {
            Paint paint = new Paint();

            int headerHeight = bitmaps.get(0).getHeight();

            int maxPageHeight = headerHeight + bitmaps.get(1).getHeight() * 50, totalHeightLeft = allItemsHeight;
            int itemsPassed = 0, itemsLeftForNextPage = 51;

            int totalPages = Math.round(((float) (allItemsHeight - headerHeight) / (float) (maxPageHeight - headerHeight)) + 0.3f);

            for (int g = 1; g <= totalPages; g++) {

                Bitmap bigbitmap = Bitmap.createBitmap(480, (totalHeightLeft < maxPageHeight ? totalHeightLeft : (g == 1 ? maxPageHeight : maxPageHeight - headerHeight)), Bitmap.Config.RGB_565);
                Canvas bigcanvas = new Canvas(bigbitmap);

                int heightPassed = 0;

                for (; itemsPassed < itemsLeftForNextPage; itemsPassed++) {
                    Bitmap bmp = bitmaps.get(itemsPassed);
                    bigcanvas.drawBitmap(bmp, 0, heightPassed, paint);
                    heightPassed += bmp.getHeight();

                    bmp.recycle();
                }

                itemsLeftForNextPage += (itemsPassed + 50 > bitmaps.size() ? bitmaps.size() - itemsLeftForNextPage : 50);

                if (bitmaps.size() - itemsLeftForNextPage < 9) {
                    itemsLeftForNextPage += bitmaps.size() - itemsLeftForNextPage;
                }

                totalHeightLeft -= heightPassed;
                Log.d(ViewToPdfExporter.class.getSimpleName(), "for next page = " + Integer.toString(itemsLeftForNextPage) + ", heightLeft" + Integer.toString(totalHeightLeft) + ", totalPages" + Integer.toString(totalPages) + ", " + Integer.toString(g));

                BitmapAndIndex b = new BitmapAndIndex(bigbitmap, index);

                if (g == totalPages)
                    new AddImageToDocument().execute(b, b);
                else
                    new AddImageToDocument().execute(b);
                bigbitmap = null;
            }

        } else {
            Bitmap bigbitmap = Bitmap.createBitmap(480, allItemsHeight, Bitmap.Config.RGB_565);
            Canvas bigcanvas = new Canvas(bigbitmap);

            Paint paint = new Paint();
            int iHeight = 0;

            for (int i = 0; i < bitmaps.size(); i++) {
                Bitmap bmp = bitmaps.get(i);
                bigcanvas.drawBitmap(bmp, 0, iHeight, paint);
                iHeight += bmp.getHeight();

                bmp.recycle();
            }

            BitmapAndIndex b = new BitmapAndIndex(bigbitmap, index);

            new AddImageToDocument().execute(b);
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap) {
        return scaleBitmap(bitmap, false);
    }

    private Bitmap scaleBitmap(Bitmap bitmap, boolean scaleTo70) {
        float originalWidth = bitmap.getWidth(), originalHeight = bitmap.getHeight();
        Log.d(ViewToPdfExporter.class.getSimpleName(), String.format("originalWidth = %f, originalHeight = %f", originalWidth, originalHeight));
        float scaleX = (scaleTo70 ? (1024 / originalWidth) * 0.65f : 1024 / originalWidth);
        int preferredHeight = (int) (originalHeight * scaleX);
        int preferredWidth = (int) (originalWidth * scaleX);
        Log.d(ViewToPdfExporter.class.getSimpleName(), String.format("preferredWidth = %d, preferredHeight = %d", preferredWidth, preferredHeight));
        Log.d(ViewToPdfExporter.class.getSimpleName(), String.format("scaleX = %f", scaleX));
//        Bitmap bitmapToReturn = Bitmap.createBitmap(576, (int)(perfectHeight < 0 ? preferredHeight : (preferredHeight < perfectHeight ? perfectHeight : preferredHeight)), Bitmap.Config.RGB_565);
        Bitmap bitmapToReturn = Bitmap.createBitmap(preferredWidth, preferredHeight, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmapToReturn);
        Matrix transformation = new Matrix();
        transformation.preScale(scaleX, scaleX);
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap, transformation, paint);
        Log.d(ViewToPdfExporter.class.getSimpleName(), String.format("bitmapWidth = %d, bitmapHeight = %d", bitmapToReturn.getWidth(), bitmapToReturn.getHeight()));
        Log.d(ViewToPdfExporter.class.getSimpleName(),"---------------------------");
        return bitmapToReturn;
    }

    private Bitmap[] scaleBitmaps(Bitmap bitmap) {
        Bitmap[] bmp = new Bitmap[1];
        if (perfectHeight == -1) {
            bmp[0] = scaleBitmap(bitmap);
        } else {
            if (bitmap.getHeight() > perfectHeight * 1.5f) {

                int numberOfPages = Math.round((bitmap.getHeight() / perfectHeight) + 0.5f);
                int heightPassed = 0;
                bmp = new Bitmap[numberOfPages];

                for (int i = 1; i <= numberOfPages; i++) {
                    Bitmap bitmapToReturn = Bitmap.createBitmap(bitmap, 0, heightPassed, bitmap.getWidth(),
                            (bitmap.getHeight() - heightPassed >= (int) perfectHeight ? (int) perfectHeight : bitmap.getHeight() - heightPassed));
                    Canvas canvas = new Canvas(bitmapToReturn);
                    Paint paint = new Paint();
                    paint.setFilterBitmap(true);
                    canvas.drawBitmap(bitmapToReturn, 0, 0, paint);
                    heightPassed += bitmapToReturn.getHeight();
                    bmp[i - 1] = scaleBitmap(bitmapToReturn);
                }
            } else {
                bmp[0] = scaleBitmap(bitmap);
            }
        }
        return bmp;
    }

    public boolean isShare() {
        return share;
    }

    public void setShare(boolean share) {
        this.share = share;
    }

    public DetailedSession getContext() {
        return context;
    }

    public void setContext(DetailedSession context) {
        this.context = context;
    }

    public OnPdfDocumentUpdateListener getListener() {
        return listener;
    }

    public void setListener(OnPdfDocumentUpdateListener listener) {
        this.listener = listener;
    }

    public boolean isFileExists() {
        return fileExists;
    }

    public void setFileExists(boolean fileExists) {
        this.fileExists = fileExists;
    }

    public ArrayList<Fragment> getFragments() {
        return fragments;
    }

    public void setFragments(ArrayList<Fragment> fragments) {
        this.fragments = fragments;
    }

    public String getPathToPdf() {
        return pathToPdf;
    }

    public void setPathToPdf(String pathToPdf) {
        this.pathToPdf = pathToPdf;
    }

    public void startLoadingScreens() {
        getBitmapFromScrollView(0, fragments.get(0).getView().findViewById(R.id.part_one));
        getBitmapFromScrollView(1, fragments.get(0).getView().findViewById(R.id.part_two));
    }

    public void loadNextPage(int index) {
        switch (index) {
            case 2:
                getBitmapFromScrollView(2, fragments.get(1).getView().findViewById(R.id.page_one), true);
                getBitmapFromScrollView(3, fragments.get(1).getView().findViewById(R.id.page_two), true);
                getBitmapFromScrollView(END_TAG, fragments.get(1).getView().findViewById(R.id.page_three), true);
                break;
//            case 2:
//                getWholeListViewItemsToBitmap(2, ((SessionSchemeFragment) fragments.get(2)).getCustomRecyclerView());
//                break;
        }
    }

    public void createPdf() {
        new AddImageToDocument().execute();
    }

    public void getBitmapFromScrollView(int index, View view) {
        getBitmapFromScrollView(index, view, false);
    }

    public void getBitmapFromScrollView(int index, View view, boolean scaleTo70) {
        View v1 = view;

        if (view instanceof ScrollView)
            v1 = ((ScrollView) view).getChildAt(0);

        v1.setDrawingCacheEnabled(true);
        BitmapAndIndex b = new BitmapAndIndex(scaleBitmap(getBitmapFromView(v1), scaleTo70), index);

//        if(index == 0){
//            perfectHeight = b.getBitmap().getHeight();
//        }

        if (index != 1) {
            if (index == END_TAG)
                new AddImageToDocument().execute(b, b);
            else
                new AddImageToDocument().execute(b);
        } else {
            new AddImageToDocument().execute(b);
            listener.loadNextPage(index);
        }
    }

    private void addImage(int index, final Document document, final byte[] byteArray, final PdfWriter writer, final boolean isLast) {
        Image image = null;
        try {
            image = Image.getInstance(byteArray);
        } catch (BadElementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            float width = image.getScaledWidth();
            float height = image.getScaledHeight();
            if (!document.isOpen()) {
                document.setPageSize(new Rectangle(width * 2, height * 3.3f));
                document.open();
            }

            Log.d(ViewToPdfExporter.class.getSimpleName(), String.format("Page size = %f, %f", width, height));

            PdfContentByte canvas = writer.getDirectContentUnder();

            if (index < 2) {
                if (index == 0) {
                    x = 0;
                    y = height * 2f;
                }

                canvas.addImage(image, width, 0, 0, height, x, y);

                x += width;
                if(index == 1)
                    y = 0;
            } else {
                if(index == 2)
                    x = 0;
                canvas.addImage(image, width, 0, 0, height, x, y);
                x += width;
            }

        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public interface OnPdfDocumentUpdateListener {
        void onLastPageLoaded();

        void onDocumentSaved(File file, boolean isShare);

        void loadNextPage(int index);
    }

    private class BitmapAndIndex {
        private Bitmap bitmap;
        private int index;

        public BitmapAndIndex(Bitmap bitmap, int index) {
            this.bitmap = bitmap;
            this.index = index;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public int getIndex() {
            return index;
        }
    }

    private class AddImageToDocument extends AsyncTask<BitmapAndIndex, Void, Document> {

        @Override
        protected Document doInBackground(BitmapAndIndex... bitmap) {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap[0].getBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream);
                bitmap[0].getBitmap().recycle();
                System.gc();
                byte[] byteArray = stream.toByteArray();

                addImage(bitmap[0].getIndex(), document, byteArray, writer, false);

                if (bitmap.length == 2)
                    return document;
                else
                    return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Document document) {
            super.onPostExecute(document);

            if (document != null) {
                document.close();
                listener.onDocumentSaved(new File(pathToPdf), isShare());
            }

        }
    }
}
