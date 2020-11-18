package com.example.cameraautocapture;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "MainActivity";
    private TextView openCameraDefault;
    private TextView openCameraOpenCV;
    private TextView content;
    public static final int TAKE_ON_CAMERA = 1000;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    Toast toast = Toast.makeText(getApplicationContext(), "成功加载opencv！", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                  break;
                default:
                    super.onManagerConnected(status);
                    Toast toast1 = Toast.makeText(getApplicationContext(), "加载失败！", Toast.LENGTH_LONG);
                    toast1.setGravity(Gravity.CENTER, 0, 0);
                    toast1.show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        openCameraDefault = findViewById(R.id.open_camera_default);
        openCameraOpenCV = findViewById(R.id.open_camera_opencv);
        content = findViewById(R.id.content);
        openCameraDefault.setOnClickListener(this);
        openCameraOpenCV.setOnClickListener(this);

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

        // 添加权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.open_camera_default:
                Intent intent = new Intent(this, CameraActivity.class);
                this.startActivityForResult(intent, TAKE_ON_CAMERA);
                break;
            case R.id.open_camera_opencv:
                Intent intent2 = new Intent(this, OpenCVActivity.class);
                this.startActivityForResult(intent2, TAKE_ON_CAMERA);
                break;
        }
    }
}