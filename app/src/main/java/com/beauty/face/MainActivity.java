/*
*  Copyright (C) 2015-present TzuTaLin
*/

package com.beauty.face;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.provider.BigImageCardProvider;
import com.dexafree.materialList.view.MaterialListView;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.PedestrianDet;
import com.tzutalin.dlib.VisionDetRet;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;
import timber.log.Timber;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {
    private static final int RESULT_LOAD_IMG = 1;
    private static final int REQUEST_CODE_PERMISSION = 2;

    private static final String TAG = "MainActivity";

    // Storage Permissions
    private static String[] PERMISSIONS_REQ = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    protected String mTestImgPath;
    // UI
    @ViewById(R.id.material_listview)
    protected MaterialListView mListView;
    @ViewById(R.id.fab)
    protected FloatingActionButton mFabActionBt;
    @ViewById(R.id.fab_cam)
    protected FloatingActionButton mFabCamActionBt;
    @ViewById(R.id.toolbar)
    protected Toolbar mToolbar;

    FaceDet mFaceDet;
    PedestrianDet mPersonDet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListView = (MaterialListView) findViewById(R.id.material_listview);
        setSupportActionBar(mToolbar);
        // Just use hugo to print log
        isExternalStorageWritable();
        isExternalStorageReadable();

        // For API 23+ you need to request the read/write permissions even if they are already in your manifest.
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;

        if (currentapiVersion >= Build.VERSION_CODES.M) {
            verifyPermissions(this);
        }

        //startActivity(new Intent(this, CameraActivity.class));

    }

    @AfterViews
    protected void setupUI() {
        mToolbar.setTitle(getString(R.string.app_name));
        Toast.makeText(MainActivity.this, getString(R.string.description_info), Toast.LENGTH_LONG).show();
    }

    @Click({R.id.fab})
    protected void launchGallery() {
        Toast.makeText(MainActivity.this, "Pick one image", Toast.LENGTH_SHORT).show();
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }

    @Click({R.id.fab_cam})
    protected void launchCameraPreview() {
        startActivity(new Intent(this, CameraActivity.class));
    }

    /**
     * Checks if the app has permission to write to device storage or open camera
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    @DebugLog
    private static boolean verifyPermissions(Activity activity) {
        // Check if we have write permission
        int write_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_persmission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int camera_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (write_permission != PackageManager.PERMISSION_GRANTED ||
                read_persmission != PackageManager.PERMISSION_GRANTED ||
                camera_permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_REQ,
                    REQUEST_CODE_PERMISSION
            );
            return false;
        } else {
            return true;
        }
    }

    /* Checks if external storage is available for read and write */
    @DebugLog
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    @DebugLog
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    @DebugLog
    protected void demoStaticImage() {
        if (mTestImgPath != null) {
            Timber.tag(TAG).d("demoStaticImage() launch a task to det");
            runDetectAsync(mTestImgPath);
        } else {
            Timber.tag(TAG).d("demoStaticImage() mTestImgPath is null, go to gallery");
            Toast.makeText(MainActivity.this, "Pick an image to run algorithms", Toast.LENGTH_SHORT).show();
            // Create intent to Open Image applications like Gallery, Google Photos
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            Toast.makeText(MainActivity.this, "Demo using static images", Toast.LENGTH_SHORT).show();
            demoStaticImage();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK && null != data) {
                // Get the Image from data
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                mTestImgPath = cursor.getString(columnIndex);
                cursor.close();
                if (mTestImgPath != null) {
                    runDetectAsync(mTestImgPath);
                    //Toast.makeText(this, "Img Path:" + mTestImgPath, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "You haven't picked Image", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
        }
    }

    // ==========================================================
    // Tasks inner class
    // ==========================================================
    private ProgressDialog mDialog;




    @Background
    @NonNull
    protected void runDetectAsync(@NonNull String imgPath) {
        showDiaglog();

        final String targetPath = Constants.getFaceShapeModelPath();
        if (!new File(targetPath).exists()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Copy landmark model to " + targetPath, Toast.LENGTH_SHORT).show();
                }
            });
            FileUtils.copyFileFromRawToOthers(getApplicationContext(), R.raw.shape_predictor_68_face_landmarks, targetPath);
        }
        // Init
        if (mPersonDet == null) {
            mPersonDet = new PedestrianDet();
        }
        if (mFaceDet == null) {
            mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
        }

        Timber.tag(TAG).d("Image path: " + imgPath);
        List<Card> cardrets = new ArrayList<>();
        List<VisionDetRet> faceList = mFaceDet.detect(imgPath);
        if (faceList.size() > 0) {
            Card card = new Card.Builder(MainActivity.this)
                    .withProvider(BigImageCardProvider.class)
                    .setDrawable(drawRect(imgPath, faceList, Color.GREEN))
                    .setTitle("Face det")
                    .endConfig()
                    .build();
            cardrets.add(card);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "No face", Toast.LENGTH_SHORT).show();
                }
            });
        }

        /*List<VisionDetRet> personList = mPersonDet.detect(imgPath);
        if (personList.size() > 0) {
            Card card = new Card.Builder(MainActivity.this)
                    .withProvider(BigImageCardProvider.class)
                    .setDrawable(drawRect(imgPath, personList, Color.BLUE))
                    .setTitle("Person det")
                    .endConfig()
                    .build();
            cardrets.add(card);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "No person", Toast.LENGTH_SHORT).show();
                }
            });
        }*/

        addCardListView(cardrets);
        dismissDialog();
    }

    @UiThread
    protected void addCardListView(List<Card> cardrets) {
        for (Card each : cardrets) {
            mListView.add(each);
        }
    }

    @UiThread
    protected void showDiaglog() {
        mDialog = ProgressDialog.show(MainActivity.this, "Wait", "Face detection", true);
    }

    @UiThread
    protected void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    @DebugLog
    protected BitmapDrawable drawRect(String path, List<VisionDetRet> results, int color) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        Bitmap bm = BitmapFactory.decodeFile(path, options);
        android.graphics.Bitmap.Config bitmapConfig = bm.getConfig();
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bm = bm.copy(bitmapConfig, true);
        int width = bm.getWidth();
        int height = bm.getHeight();
        // By ratio scale
        float aspectRatio = bm.getWidth() / (float) bm.getHeight();

        final int MAX_SIZE = 512;
        int newHeight;
        float resizeRatio = 1;
        newHeight = Math.round(MAX_SIZE / aspectRatio);
        if (bm.getWidth() > MAX_SIZE && bm.getHeight() > MAX_SIZE) {
            Timber.tag(TAG).d("Resize Bitmap");
            bm = getResizedBitmap(bm, MAX_SIZE, newHeight);
            resizeRatio = (float) bm.getWidth() / (float) width;
            Timber.tag(TAG).d("resizeRatio " + resizeRatio);
        }

        // Create canvas to draw
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);

        Paint mFaceLandmardkLipsPaint = new Paint();
        mFaceLandmardkLipsPaint.setColor(Color.RED);
        mFaceLandmardkLipsPaint.setAlpha(80);
        mFaceLandmardkLipsPaint.setStyle(Paint.Style.FILL);

        // Loop result list
        for (VisionDetRet ret : results) {
//            Rect bounds = new Rect();
//            bounds.left = (int) (ret.getLeft() * resizeRatio);
//            bounds.top = (int) (ret.getTop() * resizeRatio);
//            bounds.right = (int) (ret.getRight() * resizeRatio);
//            bounds.bottom = (int) (ret.getBottom() * resizeRatio);
//            canvas.drawRect(bounds, paint);
//            // Get landmark
//            ArrayList<Point> landmarks = ret.getFaceLandmarks();
//            for (Point point : landmarks) {
//                int pointX = (int) (point.x * resizeRatio);
//                int pointY = (int) (point.y * resizeRatio);
//                canvas.drawCircle(pointX, pointY, 2, paint);
//            }

            drawLips(ret, canvas, mFaceLandmardkLipsPaint, resizeRatio);
        }

        return new BitmapDrawable(getResources(), bm);
    }

    private void drawLips(final VisionDetRet ret, Canvas canvas, Paint mFaceLandmardkLipsPaint, float resizeRatio) {
        //float resizeRatio = 1.0f;
        ArrayList<Point> landmarks = ret.getFaceLandmarks();

        Path lipsUpperPath = new Path();

        Point point48 = landmarks.get(48);
        int point48X = (int) (point48.x * resizeRatio);
        int point48Y = (int) (point48.y * resizeRatio);
        lipsUpperPath.moveTo(point48X, point48Y);

        for (int i = 48; i < 54; i++) {
            Point pointI = landmarks.get(i);
            int pointIX = (int) (pointI.x * resizeRatio);
            int pointIY = (int) (pointI.y * resizeRatio);

            Point pointINext = landmarks.get(i + 1);
            int pointIXNext = (int) (pointINext.x * resizeRatio);
            int pointIYNext = (int) (pointINext.y * resizeRatio);

            lipsUpperPath.quadTo(pointIX, pointIY, pointIXNext, pointIYNext);
        }

//        Point point54 = landmarks.get(54);
//        int point54X = (int) (point54.x * resizeRatio);
//        int point54Y = (int) (point54.y * resizeRatio);
//        lipsUpperPath.moveTo(point54X, point54Y);
//
//        Point point64 = landmarks.get(64);
//        int point64X = (int) (point64.x * resizeRatio);
//        int point64Y = (int) (point64.y * resizeRatio);
//        lipsUpperPath.quadTo(point54X, point54Y, point64X, point64Y);
//
//        lipsUpperPath.moveTo(point64X, point64Y);
//
//        for (int i = 64; i > 60; i--) {
//            Point pointI = landmarks.get(i);
//            int pointIX = (int) (pointI.x * resizeRatio);
//            int pointIY = (int) (pointI.y * resizeRatio);
//
//            Point pointINext = landmarks.get(i + 1);
//            int pointIXNext = (int) (pointINext.x * resizeRatio);
//            int pointIYNext = (int) (pointINext.y * resizeRatio);
//
//            lipsUpperPath.quadTo(pointIX, pointIY, pointIXNext, pointIYNext);
//        }
//
//        canvas.drawPath(lipsUpperPath, mFaceLandmardkLipsPaint);

        //Path lipsBottomPath = new Path();
//        Point point54 = landmarks.get(54);
//        int point54X = (int) (point54.x * resizeRatio);
//        int point54Y = (int) (point54.y * resizeRatio);
//        lipsUpperPath.moveTo(point54X, point54Y);

        for (int i = 54; i < 59; i++) {
            Point pointI = landmarks.get(i);
            int pointIX = (int) (pointI.x * resizeRatio);
            int pointIY = (int) (pointI.y * resizeRatio);

            Point pointINext = landmarks.get(i + 1);
            int pointIXNext = (int) (pointINext.x * resizeRatio);
            int pointIYNext = (int) (pointINext.y * resizeRatio);

            lipsUpperPath.quadTo(pointIX, pointIY, pointIXNext, pointIYNext);
        }

        Point point60 = landmarks.get(59);
        int point60X = (int) (point60.x * resizeRatio);
        int point60Y = (int) (point60.y * resizeRatio);

        lipsUpperPath.quadTo(point60X, point60Y, point48X, point48Y);

//        Point point64 = landmarks.get(64);
//        int point64X = (int) (point64.x * resizeRatio);
//        int point64Y = (int) (point64.y * resizeRatio);
//        lipsUpperPath.moveTo(point64X, point64Y);

//        for (int i = 64; i < 67; i++) {
//            Point pointI = landmarks.get(i);
//            int pointIX = (int) (pointI.x * resizeRatio);
//            int pointIY = (int) (pointI.y * resizeRatio);
//
//            Point pointINext = landmarks.get(i + 1);
//            int pointIXNext = (int) (pointINext.x * resizeRatio);
//            int pointIYNext = (int) (pointINext.y * resizeRatio);
//
//            lipsBottomPath.quadTo(pointIX, pointIY, pointIXNext, pointIYNext);
//            //lipsBottomPath.quadTo(pointIXNext, pointIYNext, point60X, point60Y);
//        }

        canvas.drawPath(lipsUpperPath, mFaceLandmardkLipsPaint);

    }

    @DebugLog
    protected Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
        return resizedBitmap;
    }
}
