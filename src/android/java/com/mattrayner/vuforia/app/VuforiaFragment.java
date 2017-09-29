package com.webileapps.fragments;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.mattrayner.vuforia.VuforiaPlugin;
import com.mattrayner.vuforia.app.ApplicationControl;
import com.mattrayner.vuforia.app.ApplicationException;
import com.mattrayner.vuforia.app.ApplicationSession;
import com.mattrayner.vuforia.app.ImageTargetRenderer;
import com.mattrayner.vuforia.app.utils.ApplicationGLView;

import com.vuforia.CameraCalibration;
import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.Matrix44F;
import com.vuforia.ObjectTracker;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;

import org.json.JSONArray;

import java.util.ArrayList;

import io.cordova.hellocordova.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VuforiaFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VuforiaFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VuforiaFragment extends Fragment implements ApplicationControl {

    // TODO: replace this with your vuforia license key
    private final String LICENSE_KEY = "__PLACEHOLDER_ADD_YOUR_LICENSE_KEY_HERE__";

    ///////
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    ///////

    public static final String TAG = "VuforiaFragment";
    private static final String LOGTAG = "VuforiaFragment";
    private static final String FILE_PROTOCOL = "file://";

    private String mTargetFile;
    private String mTargets;
    private String mOverlayText;
    private String mVuforiaLicense;

    private ArrayList<String> mDatasetStrings = new ArrayList<String>();

    boolean mIsDroidDevice = false;

    ApplicationSession vuforiaAppSession;

    private DataSet mCurrentDataset;
    private int mCurrentDatasetSelectionIndex = 0;
    private boolean mContAutofocus = false;
    private boolean mExtendedTracking = false;

    // Our OpenGL view:
    private ApplicationGLView mGlView;

    // Our renderer:
    private ImageTargetRenderer mRenderer;


    public VuforiaFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment VuforiaFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static VuforiaFragment newInstance(String param1, String param2) {
        VuforiaFragment fragment = new VuforiaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_vuforia, container, false);

        //            addContentView(mGlView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.MATCH_PARENT));

        try {
            vuforiaAppSession = new ApplicationSession(this, LICENSE_KEY); //mVuforiaLicense);
        } catch (Exception e) {
            Log.d("BEN", "Error starting vuforia app session: " + VuforiaPlugin.ERROR_RESULT);
        }

        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGlView = new ApplicationGLView(this.getActivity());
        mGlView.init(translucent, depthSize, stencilSize);

        mTargets = "[\"logo\",\"iceland\",\"canterbury-grass\",\"brick-lane\",\"cordovaVuforiaTarget\"]";

        mRenderer = new ImageTargetRenderer(this, vuforiaAppSession, mTargets);
        mGlView.setRenderer(mRenderer);

        return mGlView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void setTargetFile(String targetFile) {
        mTargetFile = targetFile;
    }

    public void setTargets(String targets) {
        mTargets = targets;
    }

    public void setOverlayText(String overlayText) {
        mOverlayText = overlayText;
    }

    public void setVuforiaLicense(String vuforiaLicense) {
        mVuforiaLicense = vuforiaLicense;
    }

    public void startVuforiaForResult(int requestCode) {

//        //Remove title bar
//        getActivity().requestWindowFeature(Window.FEATURE_NO_TITLE);
//
//        //Remove notification bar
//        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//        //Force Landscape
//        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // todo: undo comment
//        try {
//            vuforiaAppSession = new ApplicationSession(this, mVuforiaLicense);
//        } catch (Exception e) {
//            Log.d("BEN", "Error starting vuforia app session: " + VuforiaPlugin.ERROR_RESULT);
//        }

        // TODO: startLoadingAnimation

        mDatasetStrings.add(mTargetFile);

        vuforiaAppSession.initAR(getActivity(), ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        // TODO: mGestureDetector

        // TODO: mTextures

        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith("droid");

    }

    boolean isExtendedTrackingActive()
    {
        return mExtendedTracking;
    }

    // To be called to initialize the trackers
    @Override
    public boolean doInitTrackers() {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;

        // Trying to initialize the image tracker
        tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null)
        {
            Log.e(LOGTAG, "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else
        {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }
        return result;
    }

    // To be called to load the trackers' data
    @Override
    public boolean doLoadTrackersData() {
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        if (mCurrentDataset == null)
            mCurrentDataset = objectTracker.createDataSet();

        if (mCurrentDataset == null)
            return false;

        //Determine the storage type.
        int storage_type;
        String dataFile = mDatasetStrings.get(mCurrentDatasetSelectionIndex);

        if(dataFile.startsWith(FILE_PROTOCOL)){
            storage_type = STORAGE_TYPE.STORAGE_ABSOLUTE;
            dataFile = dataFile.substring(FILE_PROTOCOL.length(), dataFile.length());
            mDatasetStrings.set(mCurrentDatasetSelectionIndex, dataFile);
            Log.d(LOGTAG, "Reading the absolute path: " + dataFile);
        }else{
            storage_type = STORAGE_TYPE.STORAGE_APPRESOURCE;
            Log.d(LOGTAG, "Reading the path " + dataFile + " from the assets folder.");
        }

        if (!mCurrentDataset.load(
                mDatasetStrings.get(mCurrentDatasetSelectionIndex), storage_type))
            return false;


        if (!objectTracker.activateDataSet(mCurrentDataset))
            return false;

        int numTrackables = mCurrentDataset.getNumTrackables();
        for (int count = 0; count < numTrackables; count++)
        {
            Trackable trackable = mCurrentDataset.getTrackable(count);
            if(isExtendedTrackingActive())
            {
                trackable.startExtendedTracking();
            }

            String obj_name = trackable.getName();

            String name = "Current Dataset : " + obj_name;
            trackable.setUserData(name);
            Log.d(LOGTAG, "UserData:Set the following user data "
                    + (String) trackable.getUserData());
        }

        return true;
    }

    // To be called to start tracking with the initialized trackers and their
    // loaded data
    @Override
    public boolean doStartTrackers() {
        // Indicate if the trackers were started correctly
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.start();

        return result;
    }

    // To be called to stop the trackers
    @Override
    public boolean doStopTrackers() {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.stop();

        return result;
    }

    // To be called to destroy the trackers' data
    @Override
    public boolean doUnloadTrackersData() {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        if (mCurrentDataset != null && mCurrentDataset.isActive())
        {
            if (objectTracker.getActiveDataSet().equals(mCurrentDataset)
                    && !objectTracker.deactivateDataSet(mCurrentDataset))
            {
                result = false;
            } else if (!objectTracker.destroyDataSet(mCurrentDataset))
            {
                result = false;
            }

            mCurrentDataset = null;
        }

        return result;
    }

    // To be called to deinitialize the trackers
    @Override
    public boolean doDeinitTrackers() {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());

        return result;
    }

    // This callback is called after the Vuforia initialization is complete,
    // the trackers are initialized, their data loaded and
    // tracking is ready to start
    @Override
    public void onInitARDone(ApplicationException exception) {
        if (exception == null)
        {
            initApplicationAR();

            mRenderer.initRendering();
            mRenderer.mIsActive = true;

//            FragmentManager fragManager = getChildFragmentManager(); //getFragmentManager();
//            FragmentTransaction fragTransaction = fragManager.beginTransaction();
//            fragTransaction.add(android.R.id.content, new VuforiaGLViewFragment(mGlView));
//            fragTransaction.commit();

//            // Now add the GL surface view. It is important
//            // that the OpenGL ES surface view gets added
//            // BEFORE the camera is started and video
//            // background is configured.
//            addContentView(mGlView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.MATCH_PARENT));

            // Sets the UILayout to be drawn in front of the camera
//            mUILayout.bringToFront();

            // Sets the layout background to transparent
//            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            try
            {
                vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
            } catch (ApplicationException e)
            {
                Log.e(LOGTAG, e.getString());
            }

            boolean result = CameraDevice.getInstance().setFocusMode(
                    CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

            if (result)
                mContAutofocus = true;
            else
                Log.e(LOGTAG, "Unable to enable continuous autofocus");

        } else
        {
            Log.e(LOGTAG, exception.getString());
//            showInitializationErrorMessage(exception.getString());
        }
    }

    // Initializes AR application components.
    private void initApplicationAR()
    {
//        // Create OpenGL ES view:
//        int depthSize = 16;
//        int stencilSize = 0;
//        boolean translucent = Vuforia.requiresAlpha();
//
//        mGlView = new ApplicationGLView(this.getActivity());
//        mGlView.init(translucent, depthSize, stencilSize);
//
//        mRenderer = new ImageTargetRenderer(this, vuforiaAppSession, mTargets);
//        mGlView.setRenderer(mRenderer);

//        mRenderer.setSession(vuforiaAppSession);

    }


    // This callback is called every cycle
    @Override
    public void onVuforiaUpdate(State state) {

    }

    public void markerUpdate(JSONArray markersFound) {
//    public void markerUpdate(ArrayList<Pair<String,String>> markersFound) {
        Log.d(LOGTAG, "Sending repeat callback");

        String projectionMatrixString = getProjectionMatrix();
        VuforiaPlugin.sendMarkerUpdate(markersFound, projectionMatrixString);
    }

    private String getProjectionMatrix() {
        float nearPlane = 2;
        float farPlane = 2000;
        CameraCalibration cameraCalibration = CameraDevice.getInstance().getCameraCalibration();
        Matrix44F projectionMatrix = Tool.getProjectionGL(cameraCalibration, nearPlane, farPlane);
        return ImageTargetRenderer.stringFromMatrix(projectionMatrix);
    }

    public static class VuforiaGLViewFragment extends Fragment {

//        private final GLSurfaceView mFragGLView;
        private ApplicationGLView mGlView;

        public VuforiaGLViewFragment(ApplicationGLView glView) {
            super();
            mGlView = glView;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return mGlView;
        }

        @Override
        public void onPause() {
            super.onPause();
            mGlView.onPause();
        }

        @Override
        public void onResume() {
            super.onResume();
            mGlView.onResume();
        }

    }

}
