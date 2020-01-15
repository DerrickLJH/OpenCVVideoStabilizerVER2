package com.example.opencvvideostabilizer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.goodFeaturesToTrack;


public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "MainActivity";
    private Mat mGray, mRgba, mPrevGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private Button btnCapture;
    private MatOfPoint features, prevFeatures;
    private Point[] nextFeatures;
    private boolean isNextFrame = false;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.cameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
    }


    public Mat onCameraFrame(final CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

         /* final Mat[] corrected = {new Mat()};
        if (isNextFrame) {
            featuresOld = new MatOfPoint();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    goodFeaturesToTrack(inputFrame.gray(), featuresOld, 100, 0.01, 0.1);
//                     corrected[0] = stabilizeImage(oldFrame, mRgba, new MatOfPoint2f(featuresOld.toArray()));
                }
            });
            oldFrame = mRgba.clone();
            return corrected[0];
        }else {
            oldFrame = mRgba.clone();
            isNextFrame = true;
            return oldFrame;
        }*/
        // Improve quality and corner detection

        Size winSize = new Size(15,15);
        MatOfFloat err = new MatOfFloat();
        MatOfByte status = new MatOfByte();
        TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 20,0.03);

        Imgproc.medianBlur(mGray,mGray,5);
        features = new MatOfPoint();
        prevFeatures = new MatOfPoint();
        // erode
        Mat dilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3));
        Imgproc.dilate(mGray,mGray,dilate);

        // Detecting corners
        Imgproc.goodFeaturesToTrack(mGray, features, 100, 0.05, 10, new Mat(), 5, false, 0.09);
        prevFeatures.fromList(features.toList());
        for (int i =0; i < prevFeatures.toArray().length ; i++){
            Imgproc.circle(mRgba, prevFeatures.toArray()[i], 10, new Scalar(0, 255, 0),5);
        }
//        Imgproc.cornerSubPix(mGray, prevFeatures, winSize, new Size(1,1), term);

//        // Optical Flow
      /*  mPrevGray = new Mat();
        MatOfPoint2f nextFeatures = new MatOfPoint2f(prevFeatures.toArray());
        Video.calcOpticalFlowPyrLK(mPrevGray, mGray, new MatOfPoint2f(prevFeatures.toArray()), nextFeatures, status, err, winSize, 3, term, 0, 0.01);
        Imgproc.putText(mRgba,Integer.toString(features.toArray().length),new Point(20,50), Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(255,0,0));
        Imgproc.putText(mRgba,Integer.toString(nextFeatures.toArray().length),new Point(20,100), Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(255,0,0));


        //           DRAWING MOTION VECTORS
        List<Point> good_new = new ArrayList<>();
        List<Point> good_old = new ArrayList<>();

        for(int i = 0; i < prevFeatures.toArray().length;i++){
            if(status.toList().get(i) == 1){
                good_old.add(prevFeatures.toArray()[i]);
            }
        }
        for(int i = 0; i < nextFeatures.toArray().length;i++){
            if(status.toList().get(i) == 1){
                good_new.add(nextFeatures.toArray()[i]);
            }
        }

        for(int i = 0; i < good_new.size() || i < good_old.size();i++){
            Point a = new Point(); Point b = new Point();
            if(i < good_new.size()){
                a.x = good_new.get(i).x;
                a.y = good_new.get(i).y;
            }
            if(i < good_old.size()){
                b.x = good_old.get(i).x;
                b.y = good_old.get(i).y;
            }
            Imgproc.line(mRgba,a,b, new Scalar(0,0,255), 2);
            Imgproc.circle(mRgba,b, 5 , new Scalar(0,0,255), -1);

        }

        nextFeatures.fromArray(prevFeatures.toArray());
        mPrevGray = mGray.clone();*/

        return mRgba;
    }

    public Mat stabilizeImage(Mat newFrame, Mat oldFrame, MatOfPoint2f featuresOld) {
        Mat greyNew = new Mat(newFrame.size(), CvType.CV_8UC3);
        Mat greyOld = new Mat(oldFrame.size(), CvType.CV_8UC3);
        Imgproc.cvtColor(newFrame, greyNew, COLOR_RGB2GRAY);
        Imgproc.cvtColor(oldFrame, greyOld, COLOR_RGB2GRAY);
        MatOfPoint2f currentFeatures = new MatOfPoint2f();
        MatOfFloat err = new MatOfFloat();
        MatOfByte status = new MatOfByte();
        Video.calcOpticalFlowPyrLK(greyOld, greyNew, featuresOld, currentFeatures, status, err);
        Mat correctionMatrix = Calib3d.estimateAffine2D(currentFeatures, featuresOld);
        Mat corrected = new Mat();
        Imgproc.warpAffine(newFrame, corrected, correctionMatrix, newFrame.size());
        return greyNew;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.stab) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}