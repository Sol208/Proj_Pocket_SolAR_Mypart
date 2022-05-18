package com.example.pocket_solar_distance;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView myCatchView, myCatchView2;
    GLSurfaceView mSurfaceView;
    MainRenderer mRenderer;
    Session mSession;
    Config mConfig;

    boolean mUserRequestInstall = true, mTouched = false, isModelInit = false, mCatched = false;

    float mCurrentX, mCurrentY, mCatchX, mCatchY;
    float borderPointY;

    // 이동, 회전 이벤트 처리할 객체
    GestureDetector mGestureDetector;

    float[] modelMatrix = new float[16];
    float[] firstFlag = new float[16];
    float[] secondFlag = new float[16];


    final String INIT = "init";
    final String PLANET_MOVE_OFF = "off";
    final String PLANET_MOVE_ON = "on";
    final String PLANET_DISTANCE_ONE = "distanceOne";
    final String PLANET_DISTANCE_INFO = "distanceInfo";
    final String PLANET_INFO = "info";
    final String PLANET_REMOVE = "remove";
    String state = INIT;
    String prevState = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBarAndTitleBar();
        setContentView(R.layout.activity_main);

        mSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
        myCatchView = (TextView) findViewById(R.id.myCatchView);
        myCatchView2 = (TextView) findViewById(R.id.myCatchView2);

        // 제스처 이벤트 콜백함수 객체를 생성자 매개변수로 처리 (이벤트 핸들러)
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){

             @Override
            public void onLongPress(MotionEvent event) {
                if (state.equals(PLANET_MOVE_ON) || state.equals(PLANET_MOVE_OFF))
                mCatched = true;
                mCatchX =event.getX();
                mCatchY =event.getY();

                prevState = PLANET_MOVE_OFF;

                String msg = state;

                String finalMsg = msg;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        myCatchView2.setText(finalMsg);
                    }
                });
            }

            @Override
            public boolean onSingleTapUp(MotionEvent event) {

                state = PLANET_MOVE_OFF;

                String msg = state;

                String finalMsg = msg;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        myCatchView2.setText(finalMsg);
                    }
                });
                return true;
            }

//             따닥 처리(이동)
            @Override
            public boolean onDoubleTap(MotionEvent event) {
                mTouched =true; // 그려주세요
                isModelInit = false; // 좌표를 새로 받아주세요
                mCurrentX =event.getX();
                mCurrentY =event.getY();
                return true;
            }
        });


        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if(displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int i) {}

                @Override
                public void onDisplayRemoved(int i) {}

                @Override
                public void onDisplayChanged(int i) {
                    synchronized (this) {
                        mRenderer.mViewportChanged = true;
                    }
                }
            }, null);
        }


        mRenderer = new MainRenderer(this, new MainRenderer.RenderCallback() {
            @Override
            public void preRender() {
                if (mRenderer.mViewportChanged) {
                    Display display = getWindowManager().getDefaultDisplay();
                    int displayRotation = display.getRotation();
                    mRenderer.updateSession(mSession, displayRotation);
                }

                mSession.setCameraTextureName(mRenderer.getTextureId());

                Frame frame = null;

                try {
                    frame = mSession.update();
                } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                }

                if (frame.hasDisplayGeometryChanged()) {
                    mRenderer.mCamera.transformDisplayGeometry(frame);
                }

                PointCloud pointCloud = frame.acquirePointCloud();
                mRenderer.mPointCloud.update(pointCloud);
                pointCloud.release();


                if(mTouched) {
                    mTouched = false;
                    if (!state.equals(PLANET_INFO)||state.equals(PLANET_DISTANCE_ONE)||
                            state.equals(PLANET_DISTANCE_INFO)||state.equals(PLANET_REMOVE)) {
                        List<HitResult> results = frame.hitTest(mCurrentX, mCurrentY);
                        for (HitResult result : results) {
                            Pose pose = result.getHitPose(); // 증강공간에서의 좌표
                            if (!isModelInit) {
                                isModelInit = true;
                                pose.toMatrix(modelMatrix, 0); // 좌표를 가지고 matrix화 함
                                Matrix.scaleM(modelMatrix, 0,
                                        0.03f, 0.03f, 0.03f);
                                System.arraycopy(modelMatrix, 0, firstFlag, 0, 16);
                            }
                            mRenderer.mObjs.get(1).setModelMatrix(modelMatrix);
                        }
                    }
                }

                if (mCatched){
                    if (state.equals(PLANET_DISTANCE_ONE) && prevState.equals(PLANET_MOVE_OFF)){
                        setFlag(secondFlag, frame);
                        state = PLANET_DISTANCE_INFO;


                        String msg = state;
                        String finalMsg = msg;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                myCatchView2.setText(finalMsg);
                            }
                        });
                    } else if(state.equals(PLANET_MOVE_OFF)){
                        setFlag(firstFlag, frame);
                        state = PLANET_DISTANCE_ONE;
                        prevState = PLANET_MOVE_OFF;


                        String msg = state;
                        String finalMsg = msg;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                myCatchView2.setText(finalMsg);
                            }
                        });
                    }
                }


                Camera camera = frame.getCamera();
                float [] projMatrix = new float[16];
                camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f);

                float [] viewMatrix = new float[16];
                camera.getViewMatrix(viewMatrix, 0);

                mRenderer.setProjectionMatrix(projMatrix);
                mRenderer.updateViewMatrix(viewMatrix);
            }
        });

        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8,8,8,8,16,0);
        mSurfaceView.setRenderer(mRenderer);
    }

    public void setFlag(float[] flagModel, Frame frame){
            mCatched = false;
            List<HitResult> results = frame.hitTest(mCatchX, mCatchY);

            for(HitResult result:results) {
                Pose pose = result.getHitPose(); // 증강공간에서의 좌표
                if (catchCheck(pose.tx(), pose.ty(), pose.tz())) {
                    Matrix.scaleM(flagModel, 0, 10.0f, 10.0f, 10.0f);
                    Matrix.translateM(flagModel, 0, 0f, borderPointY, 0f);
                    mRenderer.mObjs.get(0).setModelMatrix(flagModel);
                    break;
                }
            }
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestCameraPermission();
        try {
            if(mSession == null) {
                switch (ArCoreApk.getInstance().requestInstall(this, true)) {
                    case INSTALLED:
                        mSession = new Session(this);
                        Log.d("메인", " ARCore session 생성");
                        break;

                    case INSTALL_REQUESTED:
                        Log.d("메인", " ARCore 설치 필요");
                        mUserRequestInstall = false;
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mConfig = new Config(mSession);

        mSession.configure(mConfig);

        try {
            mSession.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }

        mSurfaceView.onResume();
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSurfaceView.onPause();
        mSession.pause();
    }

    private void hideStatusBarAndTitleBar() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
    }

    private void requestCameraPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.CAMERA},
                    0
            );
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event); // 위임해서 받아옴

        return true;
    }


    boolean catchCheck(float x, float y, float z){
        float[][] resAll = mRenderer.mObjs.get(1).getMinMaxPoint();
        float[] minPoint = resAll[0];
        float[] maxPoint = resAll[1];


        if (x >= minPoint[0]-0.2f && x <= maxPoint[0]+0.2f &&
                y >= minPoint[1]-1.5f && y <= maxPoint[1]+1.5f &&
                z >= minPoint[2]-5.5f && z <= maxPoint[2]+5.5f){
            borderPointY = Math.abs((minPoint[1] + maxPoint[1])/2);
//            borderPointY = maxPoint[1];
            return true;
        }



        return false;
    }
}