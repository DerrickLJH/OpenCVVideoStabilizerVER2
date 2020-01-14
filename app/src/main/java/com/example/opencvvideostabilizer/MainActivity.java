package com.example.opencvvideostabilizer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.goodFeaturesToTrack;


public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "MainActivity";
    private Mat mRgba, oldFrame;
    private CameraBridgeViewBase mOpenCvCameraView;
    private Button btnCapture;
    private MatOfPoint featuresOld;
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
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }


    public Mat onCameraFrame(final CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        final Mat[] corrected = {new Mat()};
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
        }
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