/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package com.mattrayner.vuforia.app;

import java.util.ArrayList;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.vuforia.Matrix34F;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.vuforia.Vuforia;
import com.mattrayner.vuforia.app.ApplicationSession;
import com.mattrayner.vuforia.app.utils.LoadingDialogHandler;
import com.mattrayner.vuforia.app.utils.Texture;
import com.webileapps.fragments.VuforiaFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


// The renderer class for the ImageTargets sample.
public class ImageTargetRenderer implements GLSurfaceView.Renderer
{
    private static final String LOGTAG = "ImageTargetRenderer";

    private ApplicationSession vuforiaAppSession;
//    private ImageTargets mActivity;
    private VuforiaFragment mFragment;

    private Renderer mRenderer;

    public boolean mIsActive = false;

    String mTargets = "";

//    ArrayList<String> mMarkersFound = new ArrayList<String>();
//    ArrayList<Pair<String,String>> mMarkersFound = new ArrayList<Pair<String, String>>();
    JSONArray mMarkersFound = new JSONArray();

//    public ImageTargetRenderer(ImageTargets activity,
    public ImageTargetRenderer(VuforiaFragment fragment,
        ApplicationSession session, String targets)
    {
//        mActivity = activity;
        mFragment = fragment;
        vuforiaAppSession = session;
        mTargets = targets;
    }

    public void setSession(ApplicationSession session) {
        vuforiaAppSession = session;
    }

    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;

        // Call our function to render content
        renderFrame();
    }


    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");

//        initRendering(); //// TODO: 8/15/17

        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
//        if (vuforiaAppSession != null) {
            vuforiaAppSession.onSurfaceCreated();
//        }
    }


    // Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);
    }


    // Function for initializing the renderer.
    public void initRendering()
    {
        mRenderer = Renderer.getInstance();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
            : 1.0f);


        // Hide the Loading Dialog
//        mActivity.loadingDialogHandler
//            .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);

    }


    // The render function.
    private void renderFrame()
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        State state = mRenderer.begin();
        mRenderer.drawVideoBackground();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
            GLES20.glFrontFace(GLES20.GL_CW); // Front camera
        else
            GLES20.glFrontFace(GLES20.GL_CCW); // Back camera

//        mMarkersFound.clear();
        mMarkersFound = new JSONArray();

        // did we find any trackables this frame?
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++)
        {
            TrackableResult result = state.getTrackableResult(tIdx);
            Trackable trackable = result.getTrackable();
            Matrix34F pose = result.getPose();
//            Matrix44F modelViewMatrix = Tool.convertPose2GLMatrix(pose);
            Matrix44F modelViewMatrix = Tool.convert2GLMatrix(pose);

            String obj_name = trackable.getName();
            String modelViewMatrixString = stringFromMatrix(modelViewMatrix);


            Log.d(LOGTAG, "MRAY :: Found: " + obj_name);

            /**
             * Our targets array has been flattened to a string so will equal something like: ["one", "two"]
             * So, to stop weak matches such as 'two' within ["onetwothree", "two"] we wrap the term in
             * speech marks such as '"two"'
             **/
            Boolean looking_for = mTargets.toLowerCase().contains("\"" + obj_name.toLowerCase() + "\"");

            if (looking_for)
            {
//                mActivity.imageFound(obj_name);
//                mActivity.markerUpdate(obj_name, modelViewMatrix);
//                mMarkersFound.add(Pair.create(obj_name, modelViewMatrixString));
                try {
                    JSONObject marker = new JSONObject();
                    marker.put("name", obj_name);
                    marker.put("modelViewMatrix", modelViewMatrixString);
                    mMarkersFound.put(marker);
                } catch (JSONException e) {
                    Log.d(LOGTAG, "JSON ERROR: " + e);
                }

            }
        }

//        mActivity.markerUpdate(mMarkersFound);
        mFragment.markerUpdate(mMarkersFound);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        mRenderer.end();
    }

    public static String stringFromMatrix(Matrix44F matrix) {
        float[] data = matrix.getData();
        return "[" + data[0] + "," + data[1] + "," + data[2] + "," + data[3] + ","
                + data[4] + "," + data[5] + "," + data[6] + "," + data[7] + ","
                + data[8] + "," + data[9] + "," + data[10] + "," + data[11] + ","
                + data[12] + "," + data[13] + "," + data[14] + "," + data[15] + "]";
    }

    public void updateTargetStrings(String targets) {
        mTargets = targets;
    }

}
