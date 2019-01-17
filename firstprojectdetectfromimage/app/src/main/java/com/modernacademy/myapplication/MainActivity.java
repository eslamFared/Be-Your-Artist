package com.modernacademy.myapplication;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private static final int RQS_LOADIMAGE = 1;
    private Button btnLoad, btnDetFace;
    private ImageView imgView;
    private Bitmap myBitmap;
    private Bitmap resized;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnLoad = findViewById(R.id.btn_load);
        btnDetFace = findViewById(R.id.btn_detect);
        imgView = findViewById(R.id.iv_image);


        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, RQS_LOADIMAGE);
            }
        });

        btnDetFace.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                if (myBitmap == null) {
                    Toast.makeText(MainActivity.this, "Your Image isn't ok", Toast.LENGTH_LONG).show();
                } else {
                    detectFace();
                }
            }
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RQS_LOADIMAGE
                && resultCode == RESULT_OK) {

            if (myBitmap != null) {
                myBitmap.recycle();
            }

            try {
                InputStream inputStream =
                        getContentResolver().openInputStream(data.getData());
                myBitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                imgView.setImageBitmap(myBitmap);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }




// ده method عشان نحول vector ل bitmap
    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void detectFace() {

        //paint for face
        final Paint faceBorder = new Paint();
        faceBorder.setStrokeWidth(5);
        faceBorder.setColor(Color.GREEN);
        faceBorder.setStyle(Paint.Style.STROKE);


        // Create a paint for allPointsBig
        final Paint landPaint = new Paint();
        landPaint.setStrokeWidth(15);
        landPaint.setColor(Color.rgb(0, 0, 0));

        // Create a paint for EyeShadow
        final Paint landShadow = new Paint();

        // Create a paint for Eye
        final Paint eyePaint = new Paint();

        // Create a paint for Lips
        final Paint lipPaint = new Paint();

        // Create a paint for Eyebrow
        final Paint eyeBrowPaint = new Paint();


        //Create a Canvas object for drawing on
        final Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.ARGB_8888);

        final Canvas faceCanvas = new Canvas(tempBitmap);
        faceCanvas.drawBitmap(myBitmap, 0, 0, null);



        //detect Face Fire Base Vision
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(tempBitmap);

        FirebaseVisionFaceDetectorOptions options = new FirebaseVisionFaceDetectorOptions.Builder()
                .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                .build();

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);

        detector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {

                        int count = 0;
                        for (FirebaseVisionFace face : firebaseVisionFaces) {

                            Rect bounds = face.getBoundingBox();
                            // ارسم Round Rect عشان ال face
                            faceCanvas.drawRoundRect(new RectF(bounds.left, bounds.top, bounds.right, bounds.bottom), 1, 1, faceBorder);

                            // رسم النقط بتاعت كل contours
//                            List<FirebaseVisionPoint> landPoints =
//                                    face.getContour(FirebaseVisionFaceContour.ALL_POINTS).getPoints();

                            Bitmap bitUpper = getBitmapFromVectorDrawable(MainActivity.this,R.drawable.ic_upper1);
//                            Bitmap bitLower = getBitmapFromVectorDrawable(MainActivity.this,R.drawable.ic_lower);

                            List<FirebaseVisionPoint> lipUpperTop =
                                    face.getContour(FirebaseVisionFaceContour.UPPER_LIP_TOP).getPoints();


                            List<FirebaseVisionPoint> lipUpperBottom =
                                    face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).getPoints();

                            List<FirebaseVisionPoint> lipLowerTop =
                                    face.getContour(FirebaseVisionFaceContour.LOWER_LIP_TOP).getPoints();

                            List<FirebaseVisionPoint> lipLowerBottom =
                                    face.getContour(FirebaseVisionFaceContour.LOWER_LIP_BOTTOM).getPoints();


                            faceCanvas.drawBitmap(bitUpper,lipUpperTop.get(0).getX(),lipUpperTop.get(4).getY(),lipPaint);
//                            faceCanvas.drawBitmap(bitLower,lipLowerBottom.get(8).getX(),lipLowerBottom.get(4).getY(),lipPaint);

                            faceCanvas.drawBitmap(bitUpper,new Rect(0,0,0,0),new Rect(0,0,0,0),lipPaint);




//                            for (int i = 0; i < landPoints.size(); i++) {
//
//                                float x = landPoints.get(i).getX();
//                                float y = landPoints.get(i).getY();
//
//                                // عشان ال resolution بتاع ال Bitmap
//                                // رسم النقط
//                                faceCanvas.drawPoint(x, y, landPaint);
//
//                            }


//                            //Eye shadow left النقط بتاعت
//                            List<FirebaseVisionPoint> pointLeftEye =
//                                    face.getContour(FirebaseVisionFaceContour.LEFT_EYEBROW_TOP).getPoints();
//
//                            List<FirebaseVisionPoint> pointLeftEyeBrowBottom =
//                                    face.getContour(FirebaseVisionFaceContour.LEFT_EYEBROW_BOTTOM).getPoints();
//
//                            float left = pointLeftEye.get(1).getX();
//
//
//                            float top = pointLeftEyeBrowBottom.get(2).getY();
//
//                            faceCanvas.drawBitmap(bitEyeShadowLeft, left, top, landShadow);
//
//
//
//
//
//                            //Eye Shadow Right النقط بتاعت
//                            List<FirebaseVisionPoint> pointRightEye =
//                                    face.getContour(FirebaseVisionFaceContour.FACE).getPoints();
//
//
//                            float left1 = pointRightEye.get(1).getX();
//
//
//                            List<FirebaseVisionPoint> pointRightEyeBrowBottom =
//                                    face.getContour(FirebaseVisionFaceContour.RIGHT_EYEBROW_BOTTOM).getPoints();
//
//                            float top1 = pointRightEyeBrowBottom.get(2).getY();
//
//
//                            faceCanvas.drawBitmap(bitEyeShadowRight, left1, top1, landShadow);
//
//
//
//
//
//                            // left Eye النقط بتاعت
//                            List<FirebaseVisionPoint> pointsLeftEye =
//                                    face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints();
//
//                            List<FirebaseVisionPoint> pointsNose =
//                                    face.getContour(FirebaseVisionFaceContour.FACE).getPoints();
//
//                            float leftRightEye = pointsLeftEye.get(3).getX();
//                            float topRightEye = pointsNose.get(6).getY();
//
//
//                            faceCanvas.drawBitmap(bitEyeLeft, leftRightEye, topRightEye, eyePaint);
//

                            count++;
                        }


                        //تغيير ال Bitmap بحجم مظيوط

                        Toast.makeText(MainActivity.this, String.format("Detected %d faces in image", count), Toast.LENGTH_SHORT).show();
                        resized = Bitmap.createScaledBitmap(tempBitmap,1000,1400,true);

                        imgView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
