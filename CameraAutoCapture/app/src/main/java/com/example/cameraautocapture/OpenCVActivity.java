package com.example.cameraautocapture;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static java.lang.Math.sqrt;
import static java.lang.Math.abs;

public class OpenCVActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
    private String root = Environment.getExternalStorageDirectory().getPath() + File.separator + "bizpic";
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_opencv);
        mOpenCvCameraView = findViewById(R.id.opencv_camera);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        try {
            Thread.sleep(60);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Mat src = inputFrame.rgba();

        // 1.转灰度图
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        // 2.滤波增强边缘检测
        Mat dst = new Mat();
        Imgproc.medianBlur(gray, dst, 9);

        // 3. 膨胀
        Mat out = new Mat();
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(40, 4)); //第一个参数MORPH_RECT表示矩形的卷积核，当然还可以选择椭圆形的、交叉型的
        Imgproc.dilate(dst, out, element);

        // 4.转黑白图
        Mat blockWhite = new Mat();
        Imgproc.threshold(out, blockWhite, 200, 255, Imgproc.THRESH_BINARY);

        // 5.轮廓查找
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(blockWhite, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        System.out.println("轮廓数量为：" + contours.size());
//        if(contours.size() >= 2 && contours.size() <= 120){}

        List<MatOfPoint2f> squares = new ArrayList<>();
        for(int i=0;i<contours.size();i++){
            //使用图像轮廓点进行多边形拟合
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), approx, Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true)*0.02, true);

            //计算轮廓面积后，得到矩形4个顶点
            if(approx.total() == 4 && Math.abs(Imgproc.contourArea(approx)) > 1000 && Imgproc.isContourConvex(new MatOfPoint(approx.toArray()))){
                double maxCosine = 0;
                for(int j=2;j<5;j++){
                    // 求轮廓边缘之间角度的最大余弦
                    double cosine =Math.abs(angle(approx.toArray()[j % 4], approx.toArray()[j - 2], approx.toArray()[j - 1]));
                    maxCosine = Math.max(maxCosine, cosine);
                }
                if(maxCosine < 0.3){
                    squares.add(approx);
                }
            }
        }

        if(squares.size() >= 2){
            handler.sendEmptyMessage(1);
            savePicture(src);
        }

        List<MatOfPoint> pts = new ArrayList<>();
        for(int i=0;i<squares.size();i++) {
            Point p = squares.get(i).toArray()[0];
            int n = (int) squares.get(i).toArray().length;
            if (p.x > 3 && p.y > 3) {
                Imgproc.polylines(src, Arrays.asList(new MatOfPoint(squares.get(i).toArray())), true, new Scalar(0, 255, 0), 3, Imgproc.LINE_AA);
            }
        }

        return src;
    }

    static double angle(Point pt1, Point pt2, Point pt0){
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1*dx2 + dy1*dy2) / sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
    }

    private void savePicture(Mat src) {
        String filename = Environment.getExternalStorageDirectory().getPath() + File.separator + "opencv_" + System.currentTimeMillis() + ".jpg";
        System.out.println("保存路径：" + filename);
        boolean imwrite = Imgcodecs.imwrite(filename, src);
        if(!imwrite){
            System.out.println(filename + ",保存失败");
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    Toast.makeText(OpenCVActivity.this, "识别成功", Toast.LENGTH_LONG).show();
                    break;
                case 2:
                    Toast.makeText(OpenCVActivity.this, "识别失败", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };
}
