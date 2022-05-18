package com.example.pocket_solar_distance;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.google.ar.core.Session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainRenderer implements GLSurfaceView.Renderer {

    CameraPreView mCamera;
    PointCloudRenderer mPointCloud;
//    PlaneRenderer mPlane;
    boolean mViewportChanged;
    int mViewPortWidth, mViewPortHeight;
    RenderCallback mRenderCallback;

//    HashMap<String, ObjRenderer> mObjs = new HashMap<String, ObjRenderer>();
    List<ObjRenderer> mObjs = new ArrayList<ObjRenderer>();


    MainRenderer(Context context, RenderCallback callBack){
        mRenderCallback = callBack;
        mCamera = new CameraPreView();
        mPointCloud = new PointCloudRenderer();
//        mPlane = new PlaneRenderer(Color.BLUE, 0.7f);
//        mObjs.put("andy",new ObjRenderer(context, "andy.obj", "andy.png"));
//        mObjs.put("earth", new ObjRenderer(context, "earth.obj", "earth.png"));
        mObjs.add(new ObjRenderer(context,"andy.obj", "andy.png"));
        mObjs.add(new ObjRenderer(context,"earth.obj", "earth.png"));
    }

    interface RenderCallback {
        void preRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);

        mCamera.init();
        mPointCloud.init();
//        mPlane.init();
//        mObjs.get("andy").init();
//        mObjs.get("earth").init();
        for (int i = 0; i<mObjs.size(); i++){
            mObjs.get(i).init();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mViewportChanged = true;
        mViewPortWidth = width;
        mViewPortHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mRenderCallback.preRender();
        GLES20.glDepthMask(false);
        mCamera.draw();
        GLES20.glDepthMask(true);
        mPointCloud.draw();
//        mPlane.draw();
//        mObjs.get("andy").draw();
//        mObjs.get("earth").draw();
        for (int i = 0; i<mObjs.size(); i++){
            mObjs.get(i).draw();
        }
    }

    void updateSession(Session session, int displayRotation){
        if (mViewportChanged){
            session.setDisplayGeometry(displayRotation, mViewPortWidth, mViewPortHeight);
            mViewportChanged = false;
        }
    }
    void setProjectionMatrix(float[] matrix){
        mPointCloud.updateProjMatrix(matrix);
//        mPlane.setProjectionMatrix(matrix);
//        mObjs.get("andy").setProjectionMatrix(matrix);
//        mObjs.get("earth").setProjectionMatrix(matrix);
        for (int i = 0; i<mObjs.size(); i++){
            mObjs.get(i).setProjectionMatrix(matrix);
        }
    }
    void updateViewMatrix(float[] matrix){
        mPointCloud.updateViewMatrix(matrix);
//        mPlane.setViewMatrix(matrix);
//        mObjs.get("andy").setViewMatrix(matrix);
//        mObjs.get("earth").setViewMatrix(matrix);
        for (int i = 0; i<mObjs.size(); i++){
            mObjs.get(i).setViewMatrix(matrix);
        }
    }

    int getTextureId(){
        return mCamera == null ? - 1 : mCamera.mTextures[0];
    }


}
