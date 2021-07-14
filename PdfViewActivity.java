package com.yunolearning.learn.activity;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yunolearning.learn.R;

import java.io.File;
import java.io.IOException;

public class PdfViewActivity extends BaseActivity implements View.OnTouchListener {

    ImageView imageViewPdf;
    FloatingActionButton prePageButton, nextPageButton;

    private int pageIndex;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor parcelFileDescriptor;

    private String dest_file_path;

    private File file_name;

    private static final String TAG = "Touch";

    // These matrices will be used to scale points of the image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();

    // The 3 states (events) which the user is trying to perform
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    // these PointF objects are used to record the point(s) the user is touching
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;


    @Override
    int getLayout() {
        return R.layout.activity_pdf_view;
    }

    @Override
    void init(Bundle savedInstanceState) {

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        dest_file_path = getIntent().getExtras().getString("pdf_name");

        setTitle(dest_file_path);

        imageViewPdf = findViewById(R.id.pdf_image);
        prePageButton = findViewById(R.id.button_pre_doc);
        nextPageButton = findViewById(R.id.button_next_doc);

        prePageButton.setOnClickListener(this);
        nextPageButton.setOnClickListener(this);

        pageIndex = 0;

       imageViewPdf.setOnTouchListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            openRenderer(getApplicationContext());
            showPage(pageIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        try {
            closeRenderer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        if (view.getId() == R.id.button_pre_doc) {
            showPage(currentPage.getIndex() - 1);
        } else if (view.getId() == R.id.button_next_doc) {
            showPage(currentPage.getIndex() + 1);
        }
    }

    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        file_name = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), dest_file_path);
        parcelFileDescriptor = ParcelFileDescriptor.open(file_name, ParcelFileDescriptor.MODE_READ_WRITE);
        // This is the PdfRenderer we use to render the PDF.
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }
    }

    private void closeRenderer() throws IOException {
        if (currentPage != null) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }
        // Make sure to close the current page before opening another one.
        if (currentPage != null) {
            currentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
       /* Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(),
                Bitmap.Config.ARGB_8888); */
       Bitmap bitmap = Bitmap.createBitmap(
                getResources().getDisplayMetrics().densityDpi * currentPage.getWidth() / 72,
                getResources().getDisplayMetrics().densityDpi * currentPage.getHeight() / 72,
                Bitmap.Config.ARGB_8888
        );
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        // We are ready to show the Bitmap to user.
        imageViewPdf.setImageBitmap(bitmap);
        updateUi();
    }

    /**
     * Updates the state of 2 control buttons in response to the current page index.
     */
    private void updateUi() {
        int index = currentPage.getIndex();
        int pageCount = pdfRenderer.getPageCount();
        prePageButton.setEnabled(0 != index);
        nextPageButton.setEnabled(index + 1 < pageCount);
    }

    public int getPageCount() {
        return pdfRenderer.getPageCount();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        ImageView view = (ImageView) v;
        view.setScaleType(ImageView.ScaleType.MATRIX);
        float scale;
        dumpEvent(event);
        // Handle touch events here...
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:   // first finger down only
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                Log.d(TAG, "mode=DRAG"); // write to LogCat
                mode = DRAG;
                break;
            case MotionEvent.ACTION_UP: // first finger lifted
            case MotionEvent.ACTION_POINTER_UP: // second finger lifted
                mode = NONE;
                Log.d(TAG, "mode=NONE");
                break;
            case MotionEvent.ACTION_POINTER_DOWN: // first and second finger down
                oldDist = spacing(event);
                Log.d(TAG, "oldDist=" + oldDist);
                if (oldDist > 5f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                    Log.d(TAG, "mode=ZOOM");
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(event.getX() - start.x, event.getY() - start.y); // create the transformation in the matrix  of points
                } else if (mode == ZOOM) {
                    // pinch zooming
                    float newDist = spacing(event);
                    Log.d(TAG, "newDist=" + newDist);
                    if (newDist > 5f) {
                        matrix.set(savedMatrix);
                        scale = newDist / oldDist; // setting the scaling of the
                        // matrix...if scale > 1 means
                        // zoom in...if scale < 1 means
                        // zoom out
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
                break;
        }
        view.setImageMatrix(matrix); // display the transformation on screen
        return true; // indicate event was handled
    }

    /*
     * --------------------------------------------------------------------------
     * Method: spacing Parameters: MotionEvent Returns: float Description:
     * checks the spacing between the two fingers on touch
     * ----------------------------------------------------
     */

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /*
     * --------------------------------------------------------------------------
     * Method: midPoint Parameters: PointF object, MotionEvent Returns: void
     * Description: calculates the midpoint between the two fingers
     * ------------------------------------------------------------
     */

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /**
     * Show an event in the LogCat view, for debugging
     */
    private void dumpEvent(MotionEvent event) {
        String names[] = {"DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE", "POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?"};
        StringBuilder sb = new StringBuilder();
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        sb.append("event ACTION_").append(names[actionCode]);

        if (actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP) {
            sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
            sb.append(")");
        }

        sb.append("[");
        for (int i = 0; i < event.getPointerCount(); i++) {
            sb.append("#").append(i);
            sb.append("(pid ").append(event.getPointerId(i));
            sb.append(")=").append((int) event.getX(i));
            sb.append(",").append((int) event.getY(i));
            if (i + 1 < event.getPointerCount())
                sb.append(";");
        }

        sb.append("]");
        Log.d("Touch Events ---------", sb.toString());
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

}
