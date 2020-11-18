package com.example.cameraautocapture;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.camerakit.CameraKitView;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class CameraActivity extends Activity implements View.OnClickListener{
    private String root = Environment.getExternalStorageDirectory().getPath() + File.separator + "bizpic";
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private CameraKitView cameraKitView;
    private ImageView mTakeCamera;
    private ImageView mTakeBack;
    private ImageView mContinuityCamera;
    private boolean CONTINUITY_STATUS = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        cameraKitView = findViewById(R.id.camera);
        mTakeCamera = findViewById(R.id.take_camera);
        mTakeBack = findViewById(R.id.take_back);
        mContinuityCamera = findViewById(R.id.continuity_camera);
        mTakeCamera.setOnClickListener(this);
        mTakeBack.setOnClickListener(this);
        mContinuityCamera.setOnClickListener(this);
        checkDir();

    }

    @Override
    protected void onStart() {
        super.onStart();
        cameraKitView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraKitView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraKitView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cameraKitView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.take_back:
                CONTINUITY_STATUS = false;
                cancelCamera();
                break;
            case R.id.take_camera:
                CONTINUITY_STATUS = false;
                takePicture();
                break;
            case R.id.continuity_camera:
                CONTINUITY_STATUS = true;
                continuityPicture();
                break;
        }
    }

    private void continuityPicture(){
//        while (CONTINUITY_STATUS){
        cameraKitView.captureImage(new CameraKitView.ImageCallback() {
            @Override
            public void onImage(CameraKitView cameraKitView, byte[] bytes) {
                recognizeHandle(cameraKitView, bytes);
            }
        });

//            cameraKitView.captureFrame(new CameraKitView.FrameCallback() {
//                @Override
//                public void onFrame(CameraKitView cameraKitView, byte[] bytes) {
//                    recognizeHandle(cameraKitView, bytes);
//                }
//            });
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//        }

    }

    private void recognizeHandle(CameraKitView cameraKitView, byte[] bytes) {
        Mat src = new Mat(cameraKitView.getWidth(), cameraKitView.getHeight(), CvType.CV_8UC3, ByteBuffer.wrap(bytes));
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
        if(contours.size() >= 4 && contours.size() <= 120){
            pictureHandler(bytes);
        }else {
            Toast.makeText(CameraActivity.this, "识别失败", Toast.LENGTH_LONG).show();
        }
    }

    private void takePicture(){
        cameraKitView.captureImage(new CameraKitView.ImageCallback() {
            @Override
            public void onImage(CameraKitView cameraKitView, byte[] bytes) {
                pictureHandler(bytes);
            }
        });

    }

    private void pictureHandler(byte[] bytes) {
        // 原图
        final Date date = new Date();
        final String suffix = sdf.format(date) + "_" + date.getTime() + ".jpg";
        File picFile = new File(root + File.separator + "origin", suffix);
        try {
            FileOutputStream outputStream = new FileOutputStream(picFile.getPath());
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
//                    ContinuityGraph.mPresentCameraPicListOrigin.add(picFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(CameraActivity.this, "图片保存错误", Toast.LENGTH_LONG).show();
        }

        // 压缩
        Luban.with(CameraActivity.this).load(picFile).ignoreBy(60).setCompressListener(new OnCompressListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onSuccess(File file) {
                File newFile = new File(root + File.separator + "compress", suffix);
                file.renameTo(newFile);
//                        ContinuityGraph.mPresentCameraPicListCompress.add(newFile.getAbsolutePath());
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(CameraActivity.this, "图片压缩错误", Toast.LENGTH_LONG).show();
            }
        }).launch();

        // 提示
        Toast.makeText(CameraActivity.this, "拍照成功", Toast.LENGTH_LONG).show();
    }

    private void cancelCamera(){
        this.finish();
    }


    private void checkDir(){
        File rootFile = new File(root);
        if(!rootFile.exists()) rootFile.mkdirs();
        File originFile = new File(root + File.separator + "origin");
        if(!originFile.exists()) originFile.mkdir();
        File compressFile = new File((root + File.separator + "compress"));
        if(!compressFile.exists()) compressFile.mkdir();
    }



}
