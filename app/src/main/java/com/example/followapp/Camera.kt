package com.example.followapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
import android.hardware.camera2.CameraCharacteristics.SENSOR_ORIENTATION
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.getDrawable
import android.support.v4.content.PermissionChecker.PERMISSION_GRANTED
import android.support.v4.content.PermissionChecker.checkSelfPermission
import android.support.v7.app.AlertDialog
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import android.widget.VideoView
import cn.gavinliu.android.lib.scale.ScaleRelativeLayout
import com.example.followapp.cameraapi.AutoFitTextureView
import com.example.followapp.cameraapi.rotationListenerHelper
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.io.IOException
import java.lang.Long.signum
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class Camera : Fragment(),View.OnClickListener, SeekBar.OnSeekBarChangeListener, ActivityCompat.OnRequestPermissionsResultCallback {
    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // TODO: Rename and change types of parameters
    private val FURL = "<html><body><iframe width=\"1280\" height=\"720\" src=\""
    private val BURL = "\" frameborder=\"0\" allowfullscreen></iframe></html></body>"
    private val CHANGE = "https://www.youtube.com/embed/"
    var URL:String? = null
    val REQUEST_VIDEO_PERMISSIONS = 1
    val VIDEO_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private val ARG_PARAM1 = "url"
    private var param1: String? = null
    private val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
    private val SENSOR_ORIENTATION_INVERSE_DEGREES = 270
    private val TAG = "Item_follow_fragment_21"
    var baseDir = ""
    var LandButton: ScaleRelativeLayout.LayoutParams? = null
    var LandCamera:ScaleRelativeLayout.LayoutParams? = null
    var LandWebView:ScaleRelativeLayout.LayoutParams? = null
    var playlayout:ScaleRelativeLayout.LayoutParams? = null
    var recordlayout:ScaleRelativeLayout.LayoutParams? = null
    var switchlayout:ScaleRelativeLayout.LayoutParams? = null
    var loadlayout:ScaleRelativeLayout.LayoutParams? = null
    var play_recordlayout:ScaleRelativeLayout.LayoutParams? = null
    var play_record: Boolean? = true //true 가 촬영모드, false 가 재생모드
    val CAMERA_FRONT = "1"
    val CAMERA_BACK = "0"
    var change: String? = null
    var youtubeprogress:Int = 0
    var youtubePlaying:Boolean = false
    var videoprogress:Int = 0
    var videoPlaying:Boolean = false
    var video_camera:Boolean = false //false = camera, true = video
    var videoPath:String = ""
    var rotationListener: rotationListenerHelper? = null;
    var popUpT: PopupWindow? = null

    private lateinit var cameraId: String

    /**
     * An [AutoFitTextureView] for camera preview.
     */
    /**
     * A [CameraCaptureSession] for camera preview.
     */
    private var captureSession: CameraCaptureSession? = null

    /**
     * A reference to the opened [CameraDevice].
     */
    private var cameraDevice: CameraDevice? = null

    /**
     * The [android.util.Size] of camera preview.
     */
    private var previewSize: Size? = null
    /**
     * The [android.util.Size] of video recording.
     */
    private lateinit var videoSize: Size
    /**
     * Whether the app is recording video now
     */
    private var isRecordingVideo = false



    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var backgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var backgroundHandler: Handler? = null

    private var nextVideoAbsolutePath: String? = null

    private var mediaRecorder: MediaRecorder? = null

    private val FRAGMENT_DIALOG = "dialog"

    private val DEFAULT_ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 90)
        append(Surface.ROTATION_90, 0)
        append(Surface.ROTATION_180, 270)
        append(Surface.ROTATION_270, 180)
    }
    private val INVERSE_ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 270)
        append(Surface.ROTATION_90, 180)
        append(Surface.ROTATION_180, 90)
        append(Surface.ROTATION_270, 0)
    }
    lateinit var mMediaPlayer: MediaPlayer
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            Log.i("camera", "onSurfaceTextureSizeChanged")
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture){
            Log.i("camera", "onSurfaceTextureUpdated")
        }

    }

    lateinit var textureView: AutoFitTextureView
    lateinit var video_View: VideoView
    lateinit var webview: WebView


    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@Camera.cameraDevice = cameraDevice
            startPreview()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@Camera.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@Camera.cameraDevice = null
            activity?.finish()
        }


    }
    override fun onClick(v:View) {
        when (v.getId()) {
            R.id.record_Btn ->//녹화
            {
                if(video_camera) {
                    video_camera = false
                }
                Log.d(TAG, "record_Btn thouch")
                if (textureView.isAvailable) {
                    if (isRecordingVideo) {
                        stopRecordingVideo()
                    } else {
                        startRecordingVideo()
                    }
                }
            }
            R.id.play_Btn//재생
            ->{
                if(!video_camera) {
                    video_camera = true
                }
                Log.d(TAG, "play_Btn thouch")
                if(video_View.isPlaying){
                    play_Btn.setImageResource(R.drawable.play)
                    video_View.pause()
                }else {
                    if(video_View.currentPosition > 100) {
                        video_View.resume()
                    }else{
                        video_View.start()
                    }
                    play_Btn.setImageResource(R.drawable.pause)
                }
            }

            R.id.load_Btn//파일불러오기
            -> {
                videoPath = ""
                videoprogress = 0
                if(!video_camera) {
                    video_camera = true
                }
                val intent = Intent(Intent.ACTION_GET_CONTENT);
                val uri = Uri.parse(Environment.getExternalStoragePublicDirectory("DIRECTORY_MOVIES").getPath() + File.separator + "bodygation" + File.separator);
                intent.setType("video/mp4");
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                startActivityForResult(Intent.createChooser(intent, "Select Video"), 3)
                Log.i(TAG, "videoPath : " + videoPath)
            }

            R.id.play_record_Btn//전후면 카메라변환
            -> {
                Log.d(TAG, "viewChange_Btn thouch");
                switchCamera()
            }

            R.id.viewChange_Btn//파일과 카메라간 변환
            -> {
                if (video_camera) {
                    video_View.visibility = View.VISIBLE
                    textureView.visibility = View.GONE
                    if(video_View.isPlaying){
                        play_Btn.setImageResource(R.drawable.play)
                        video_View.stopPlayback()
                    }
                    video_camera = false
                } else {
                    video_View.visibility = View.GONE
                    textureView.visibility = View.VISIBLE
                    video_camera = true
                    if (isRecordingVideo) {
                        stopRecordingVideo()
                    }
                    closeCamera()
                }
            }
            R.id.youtube_layout
            ->{
                if(URL == null){
                    showToast(getString(R.string.not_connected_url))
                    popUpT = PopupWindow(context)
                    popUpT.
                }
            }
        }
    }

    fun switchCamera() {
        if(!video_camera) {
            if (cameraId.equals(CAMERA_FRONT)) {
                cameraId = CAMERA_BACK;
                closeCamera()
                openCamera(textureView.width, textureView.height)
                play_record_Btn.setImageDrawable(getDrawable(this.requireActivity(), R.drawable.backcamera))
            } else if (cameraId.equals(CAMERA_BACK)) {
                cameraId = CAMERA_FRONT;
                closeCamera()
                openCamera(textureView.width, textureView.height)
                play_record_Btn.setImageDrawable(getDrawable(this.requireActivity(), R.drawable.frontcamera))
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        Thread(object:Runnable {
            override fun run() {
                // 현재 UI 스레드가 아니기 때문에 메시지 큐에 Runnable을 등록 함
                getActivity()!!.runOnUiThread(object:Runnable {
                    override fun run() {
                        // 메시지 큐에 저장될 메시지의 내용;
                        val a = (progress.toDouble() / 100.0)
                        val b = a.toFloat()
                        webview.setAlpha(b)
                    }
                })
            }
        }).start()
    }

    /**
     * [CaptureRequest.Builder] for the camera preview
     */
    private lateinit var previewRequestBuilder: CaptureRequest.Builder


    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val cameraOpenCloseLock = Semaphore(1)

    /**
     * Orientation of the camera sensor
     */
    private var sensorOrientation = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRetainInstance(true)
        if(savedInstanceState != null){
            Log.i(TAG, "onCreate savedInstanceState")
            param1 = savedInstanceState.getString(ARG_PARAM1)
        }else {
            arguments?.let {
                param1 = it.getString(ARG_PARAM1)
            }
        }
        val state = Environment.getExternalStorageState()
        if ( Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state) ) {  // we can read the External Storage...
            //Retrieve the primary External Storage:
            baseDir = Environment.getExternalStoragePublicDirectory("DIRECTORY_MOVIES").path
        }else{
            baseDir = Environment.DIRECTORY_MOVIES
        }
    }
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        startBackgroundThread()
        if(video_camera) {
            textureView.visibility = View.GONE
            video_View.visibility = View.VISIBLE
        }else{
            video_View.visibility = View.GONE
            textureView.visibility = View.VISIBLE
        }
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
        when(requireActivity().windowManager.defaultDisplay.rotation){

            0 -> {
                //. SCREEN_ORIENTATION_PORTRAIT
                if(video_camera){
                    video_View.setAlpha((1).toFloat())
                }else{
                    textureView.setAlpha((1).toFloat())
                }
                webview.setZ(2.toFloat())
                alpha_control.progress = 99
            }
            2 -> {
                //. SCREEN_ORIENTATION_REVERSE_PORTRAIT
                if(video_camera){
                    video_View.setAlpha((1).toFloat())
                }else{
                    textureView.setAlpha((1).toFloat())
                }
                webview.setZ(2.toFloat())
                alpha_control.progress = 99
            }
            //----------------------------------------
            1 -> {
                //. SCREEN_ORIENTATION_LANDSCAPE
                alpha_control.setProgress(50)
                alpha_control.setZ(0.toFloat())
                webview.setZ(2.toFloat())
            }
            //----------------------------------------
            3 -> {
                alpha_control.setProgress(50)
                alpha_control.setZ(0.toFloat())
                webview.setZ(2.toFloat())
            }
        }
        youtube_layout.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        closeCamera()
        stopBackgroundThread()
        if(videoPlaying){
            videoprogress = video_View.currentPosition
        }
        youtube_layout.onPause()
    }
    @SuppressLint("SetJavaScriptEnabled")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.i(TAG, "onActivityCreated")
        if (savedInstanceState != null) {
            //Restore the fragment's state here
            param1 = savedInstanceState.getString("url")
            youtubeprogress = savedInstanceState.getInt("progress")
            video_camera = savedInstanceState.getBoolean("playyoutube")
            if (video_camera) {
                videoPath = savedInstanceState.getString("videoPath")
                videoPlaying = savedInstanceState.getBoolean("videoPlaying")
            }
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textureView = view.findViewById(R.id.AutoView)
        video_View = view.findViewById(R.id.VideoView)
        webview = view.findViewById(R.id.youtube_layout)
        cameraId = CAMERA_FRONT
        startBackgroundThread()
        val curOrientation =  requireActivity().windowManager.defaultDisplay.rotation

        when (curOrientation) {
            0 -> {
                //. SCREEN_ORIENTATION_PORTRAIT
                PortrainSet()
            }
            2 -> {
                //. SCREEN_ORIENTATION_REVERSE_PORTRAIT
                PortrainSet()
                val matrix = Matrix()
                val viewRect = RectF(0f, 0f, textureView.width.toFloat(), textureView.height.toFloat())
                matrix.postRotate(180f, viewRect.centerX(), viewRect.centerY())
                textureView.setTransform(matrix)
            }
            //----------------------------------------
            1 -> {
                //. SCREEN_ORIENTATION_LANDSCAPE
                LandSet()
            }
            //----------------------------------------
            3 -> {
                //. SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                LandSet()
                val matrix = Matrix()
                val viewRect = RectF(0f, 0f, textureView.width.toFloat(), textureView.height.toFloat())
                matrix.postRotate(180f, viewRect.centerX(), viewRect.centerY())
                textureView.setTransform(matrix)
            }
            //----------------------------------------
        } /*endSwitch*/
        viewSet()
        rotationListener = rotationListenerHelper()
        rotationListener!!.listen(this.requireContext(), object : rotationListenerHelper.rotationCallbackFn {
            override fun onRotationChanged(lastRotation: Int, newRotation: Int) {
                Log.d(TAG, "onRotationChanged: last " + (lastRotation) +"  new " + (newRotation));
                /**
                 * no need to recreate activity if screen rotate from portrait to landscape
                 * android do the job in order to reload resources
                 */
                if (
                    (lastRotation == 0 && newRotation == 2) ||
                    (lastRotation == 2 && newRotation == 0) ||
                    (lastRotation == 1 && newRotation == 3) ||
                    (lastRotation == 3 && newRotation == 1)
                )
                    requireActivity().recreate()
            }
        })
        alpha_control.max = 99
        alpha_control.setOnSeekBarChangeListener(this)
        if(video_camera){
            if(videoPath == ""){
                val intent = Intent(Intent.ACTION_GET_CONTENT);
                val uri = Uri.parse(Environment.getExternalStoragePublicDirectory("DIRECTORY_MOVIES").getPath() + File.separator + "bodygation" + File.separator);
                intent.setType("video/mp4");
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                startActivityForResult(Intent.createChooser(intent, "Select Video"), 3)
                Log.i(TAG, "videoPath : " + videoPath)
            }
        }
    }
    private fun viewSet(){
        record_Btn.setOnClickListener(this)
        load_Btn.setOnClickListener(this)
        play_Btn.setOnClickListener(this)
        play_record_Btn.setOnClickListener(this)
        viewChange_Btn.setOnClickListener(this)
        webview.setWebChromeClient(WebChromeClient())
        webview.setWebViewClient(WebViewClient())
        val settings = youtube_layout.getSettings()
        settings.setJavaScriptEnabled(true)
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN)
        settings.setJavaScriptCanOpenWindowsAutomatically(true)
        settings.setPluginState(WebSettings.PluginState.ON)
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK)
        settings.setSupportMultipleWindows(true)
        settings.setLoadWithOverviewMode(true)
        settings.setUseWideViewPort(true)
        URL = FURL + CHANGE + param1 + BURL
        webview.loadData(URL, "text/html", "charset=utf-8");

        alpha_control.setOnSeekBarChangeListener(this)
        video_View.setOnCompletionListener(object :MediaPlayer.OnCompletionListener{
            override fun onCompletion(p0: MediaPlayer?) {
                video_View.seekTo(100)
                play_Btn.setImageResource(R.drawable.play)
            }
        })
    }
    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private fun chooseVideoSize(choices: Array<Size>): Size {
        for (size in choices) {
            if (size.getWidth() === size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size")
        return choices[choices.size - 1]
    }

    /**
     * Given `choices` of `Size`s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal `Size`, or an arbitrary one if none were big enough
     */
    private fun chooseOptimalSize( choices: Array<Size>,
                                   width: Int,
                                   height: Int,
                                   aspectRatio: Size
    ): Size {
        // Collect the supported resolutions that are at least as big as the preview Surface
        val w = aspectRatio.width
        val h = aspectRatio.height
        val bigEnough = choices.filter {
            it.height == it.width * h / w && it.width >= width && it.height >= height }

        // Pick the smallest of those, assuming we found any
        return if (bigEnough.isNotEmpty()) {
            Collections.min(bigEnough, CompareSizesByArea())
        } else {
            choices[0]
        }
    }
/**
     * Starts a background thread and its {@link Handler}.
     */
    private fun startBackgroundThread() {
    backgroundThread = HandlerThread("CameraBackground")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.getLooper())
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private fun stopBackgroundThread() {
        backgroundThread!!.quitSafely()
        try {
            backgroundThread!!.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e:InterruptedException) {
            e.printStackTrace()
        }
    }
    /**
     * Gets whether you should show UI with rationale for requesting permissions.
     *
     * @param permissions The permissions your app wants to request.
     * @return Whether you can show permission rationale UI.
     */
    /**
     * Requests permissions needed for recording video.
     */
    private fun setupPermissions() {
        val cameraP = ContextCompat.checkSelfPermission(context!!,
                Manifest.permission.CAPTURE_VIDEO_OUTPUT)
        val writeP = ContextCompat.checkSelfPermission(context!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readP = ContextCompat.checkSelfPermission(context!!,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        val recordP = ContextCompat.checkSelfPermission(context!!,
                Manifest.permission.RECORD_AUDIO)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            if (grantResults.size == VIDEO_PERMISSIONS.size) {
                for (result in grantResults) {
                    if (result != PERMISSION_GRANTED) {
                        ErrorDialog.newInstance(getString(R.string.permission_request))
                            .show(childFragmentManager, FRAGMENT_DIALOG)
                        break
                    }
                }
            } else {
                ErrorDialog.newInstance(getString(R.string.permission_request))
                    .show(childFragmentManager, FRAGMENT_DIALOG)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult");
        Log.d("requestCode", requestCode.toString());
        Log.d("resultCode", resultCode.toString());
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 3 && data != null) {
                val mVideoURI = data.getData()
                videoPath = mVideoURI.toString()
                Log.d("onActivityResult", mVideoURI.toString())
                Log.d("Result videoString", videoPath)
                //Log.d("getRealPathFromURI", getRealPathFromURI(getContext(), mVideoURI))
                video_View.setVideoPath(videoPath)
            }
        }
    }

    private fun LandSet(){
        LandWebView = ScaleRelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        LandButton = ScaleRelativeLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.portlaneimageBtnsize_item), ViewGroup.LayoutParams.MATCH_PARENT);
        LandCamera = ScaleRelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        playlayout = ScaleRelativeLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.imageBtnsize_item), getResources().getDimensionPixelSize(R.dimen.imageBtnsize_item));
        recordlayout = ScaleRelativeLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.imageBtnsize_item), getResources().getDimensionPixelSize(R.dimen.imageBtnsize_item));
        switchlayout = ScaleRelativeLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.imageBtnsize_item), getResources().getDimensionPixelSize(R.dimen.imageBtnsize_item));
        play_recordlayout = ScaleRelativeLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.imageBtnsize_item), getResources().getDimensionPixelSize(R.dimen.imageBtnsize_item));
        loadlayout = ScaleRelativeLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.imageBtnsize_item), getResources().getDimensionPixelSize(R.dimen.imageBtnsize_item));
        val seek = ScaleRelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LandButton!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_TOP);
        LandButton!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_START);
        button_layout.setLayoutParams(LandButton);
        seek.addRule(ScaleRelativeLayout.ALIGN_PARENT_END);
        seek.addRule(ScaleRelativeLayout.END_OF, R.id.button_layout);
        alpha_control.setLayoutParams(seek);
        playlayout!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_TOP);
        playlayout!!.addRule(ScaleRelativeLayout.CENTER_VERTICAL);
        playlayout!!.setMargins(getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item));
        play_Btn.setLayoutParams(playlayout);
        recordlayout!!.setMargins(getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item));
        recordlayout!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_BOTTOM);
        recordlayout!!.addRule(ScaleRelativeLayout.CENTER_VERTICAL);
        record_Btn.setLayoutParams(recordlayout);
        loadlayout!!.addRule(ScaleRelativeLayout.BELOW, R.id.play_Btn);
        loadlayout!!.addRule(ScaleRelativeLayout.CENTER_VERTICAL);
        loadlayout!!.setMargins(getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item));
        load_Btn.setLayoutParams(loadlayout);
        play_recordlayout!!.addRule(ScaleRelativeLayout.CENTER_VERTICAL)
        play_recordlayout!!.setMargins(getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item));
        play_recordlayout!!.addRule(ScaleRelativeLayout.CENTER_VERTICAL)
        play_record_Btn.setLayoutParams(play_recordlayout);
        switchlayout!!.addRule(ScaleRelativeLayout.ABOVE, R.id.record_Btn)
        switchlayout!!.addRule(ScaleRelativeLayout.CENTER_VERTICAL)
        switchlayout!!.setMargins(getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item))
        viewChange_Btn.setLayoutParams(switchlayout);
        alpha_control.setVisibility(View.VISIBLE)
        LandCamera!!.addRule(ScaleRelativeLayout.END_OF, R.id.button_layout)
        LandCamera!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_BOTTOM)
        LandCamera!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_END)
        LandWebView!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_END)
        LandWebView!!.addRule(ScaleRelativeLayout.BELOW, R.id.alpha_control)
        LandWebView!!.addRule(ScaleRelativeLayout.END_OF, R.id.button_layout)
        LandWebView!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_BOTTOM)
        webview.setLayoutParams(LandWebView)
        video_View.setLayoutParams(LandCamera)
        textureView.setLayoutParams(LandCamera)
        if(video_camera){
            video_View.visibility = View.VISIBLE
            textureView.visibility = View.GONE
            video_View.setZ(0.toFloat())
        }else{
            textureView.visibility = View.VISIBLE
            video_View.visibility = View.GONE
            textureView.setZ(0.toFloat())
        }
    }
    private fun PortrainSet(){
        LandWebView = ScaleRelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        LandButton = ScaleRelativeLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.portwidthbtn), getResources().getDimensionPixelSize(R.dimen.portbtn));
        LandCamera = ScaleRelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        playlayout = ScaleRelativeLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.portlaneimageBtnsize_item), getResources().getDimensionPixelSize(R.dimen.portlaneimageBtnsize_item));
        recordlayout = ScaleRelativeLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.portlaneimageBtnsize_item), getResources().getDimensionPixelSize(R.dimen.portlaneimageBtnsize_item));
        switchlayout = ScaleRelativeLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.portlaneimageBtnsize_item), getResources().getDimensionPixelSize(R.dimen.portlaneimageBtnsize_item));
        play_recordlayout = ScaleRelativeLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.portlaneimageBtnsize_item), getResources().getDimensionPixelSize(R.dimen.portlaneimageBtnsize_item));
        loadlayout = ScaleRelativeLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.portlaneimageBtnsize_item), getResources().getDimensionPixelSize(R.dimen.portlaneimageBtnsize_item));
        LandButton!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_END)
        LandButton!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_TOP)
        button_layout.setLayoutParams(LandButton);
        LandWebView!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_START)
        LandWebView!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_END)
        LandWebView!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_BOTTOM)
        LandWebView!!.addRule(ScaleRelativeLayout.BELOW, R.id.button_layout)
        webview.setLayoutParams(LandWebView)
        playlayout!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_BOTTOM)
        playlayout!!.addRule(ScaleRelativeLayout.CENTER_HORIZONTAL)
        playlayout!!.setMargins(getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item));
        play_Btn.setLayoutParams(playlayout);
        recordlayout!!.setMargins(getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item))
        recordlayout!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_TOP)
        recordlayout!!.addRule(ScaleRelativeLayout.CENTER_HORIZONTAL)
        record_Btn.setLayoutParams(recordlayout)
        loadlayout!!.addRule(ScaleRelativeLayout.ABOVE, R.id.play_Btn)
        loadlayout!!.addRule(ScaleRelativeLayout.CENTER_HORIZONTAL)
        loadlayout!!.setMargins(getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item))
        load_Btn.setLayoutParams(loadlayout)
        play_recordlayout!!.addRule(ScaleRelativeLayout.CENTER_VERTICAL)
        play_recordlayout!!.addRule(ScaleRelativeLayout.CENTER_HORIZONTAL)
        play_recordlayout!!.setMargins(getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item))
        play_record_Btn.setLayoutParams(play_recordlayout)
        switchlayout!!.addRule(ScaleRelativeLayout.BELOW, R.id.record_Btn)
        switchlayout!!.addRule(ScaleRelativeLayout.CENTER_HORIZONTAL)
        switchlayout!!.setMargins(getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item), getResources().getDimensionPixelSize(R.dimen.imageBtnmargine_item))
        viewChange_Btn.setLayoutParams(switchlayout)
        LandCamera!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_START)
        LandCamera!!.addRule(ScaleRelativeLayout.ALIGN_PARENT_TOP)
        LandCamera!!.addRule(ScaleRelativeLayout.START_OF, R.id.button_layout)
        LandCamera!!.addRule(ScaleRelativeLayout.ABOVE, R.id.youtube_layout)
        alpha_control.setVisibility(View.GONE)
        textureView.setLayoutParams(LandCamera)
        video_View.setLayoutParams(LandCamera)
    }
    private fun hasPermissionsGranted(permissions: Array<String>) =
            permissions.none {
                checkSelfPermission((activity as FragmentActivity), it) != PERMISSION_GRANTED
            }
    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    @SuppressWarnings("MissingPermission")
    private fun openCamera(width:Int, height:Int) {
        if (!hasPermissionsGranted(VIDEO_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this.requireActivity(), VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS)
            return
        }
        val cameraActivity = activity
        if (cameraActivity == null || cameraActivity.isFinishing) return

        val manager = cameraActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            val cameraId = cameraId

            // Choose the sizes for camera preview and video recording
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(SCALER_STREAM_CONFIGURATION_MAP) ?:
            throw RuntimeException("Cannot get available preview/video sizes")
            sensorOrientation = characteristics.get(SENSOR_ORIENTATION)
            Log.i("configureTransform", "sensorOrientation : " + sensorOrientation.toString())
            videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java))
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
                    width, height, videoSize)
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(16, 9)
                configureTransform(width, height)
            } else {
                textureView.setAspectRatio(3, 4)
                configureTransform(height, width)
            }
            Log.i("camera", "openCamera")
            mediaRecorder = MediaRecorder()
            manager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            showToast("Cannot access the camera.")
            cameraActivity.finish()
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(childFragmentManager, FRAGMENT_DIALOG)
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }
    fun closeCamera() {
        try {
        cameraOpenCloseLock.acquire()
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        mediaRecorder?.release()
        mediaRecorder = null
    } catch (e: InterruptedException) {
        throw RuntimeException("Interrupted while trying to lock camera closing.", e)
    } finally {
        cameraOpenCloseLock.release()
    }
    }

    /**
     * Start the camera preview.
     */
    private fun startPreview() {
        if (cameraDevice == null || !textureView.isAvailable) return

        try {
            closePreviewSession()
            val texture = textureView.surfaceTexture
            texture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(TEMPLATE_PREVIEW)

            val previewSurface = Surface(texture)
            previewRequestBuilder.addTarget(previewSurface)

            cameraDevice?.createCaptureSession(listOf(previewSurface),
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            updatePreview()
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            if (activity != null) showToast("Failed")
                        }
                    }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private fun updatePreview() {
        if (cameraDevice == null) return

        try {
            setUpCaptureRequestBuilder(previewRequestBuilder)
            HandlerThread("CameraPreview").start()
            captureSession?.setRepeatingRequest(previewRequestBuilder.build(),
                    null, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun setUpCaptureRequestBuilder(builder:CaptureRequest.Builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        activity ?: return
        val manager = this.requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraId

        // Choose the sizes for camera preview and video recording
        val characteristics = manager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(SCALER_STREAM_CONFIGURATION_MAP) ?:
        throw RuntimeException("Cannot get available preview/video sizes")
        sensorOrientation = characteristics.get(SENSOR_ORIENTATION)
        val rotation = activity!!.windowManager.defaultDisplay.rotation
        videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java))
        previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
                viewWidth, viewHeight, videoSize)
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize!!.height.toFloat(), previewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        Log.i(TAG, "configureTransform : " + rotation.toString())
        Log.i(TAG, "configureTransform : " + sensorOrientation.toString())
        Log.i(TAG, "configureTransform 0 : " + Surface.ROTATION_0.toString())
        Log.i(TAG, "configureTransform 90 : " + Surface.ROTATION_90.toString())
        Log.i(TAG, "configureTransform 180 : " + Surface.ROTATION_180.toString())
        Log.i(TAG, "configureTransform 270 : " + Surface.ROTATION_270.toString())
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            Log.i(TAG, "configureTransform : " + "Land")
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
                    viewHeight.toFloat() / previewSize!!.height,
                    viewWidth.toFloat() / previewSize!!.width)
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            Log.i(TAG, "configureTransform : " + "reverse")
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView.setTransform(matrix)
    }

    private fun setUpMediaRecorder() {
        val cameraActivity = activity ?: return

        if (nextVideoAbsolutePath.isNullOrEmpty()) {
            nextVideoAbsolutePath = getVideoFilePath() + "bodygation_" + System.currentTimeMillis() + ".mp4"
        }

        val rotation = cameraActivity.windowManager.defaultDisplay.rotation
        when (sensorOrientation) {
            SENSOR_ORIENTATION_DEFAULT_DEGREES ->
                mediaRecorder?.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation))
            SENSOR_ORIENTATION_INVERSE_DEGREES ->
                mediaRecorder?.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation))
        }

        mediaRecorder?.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(nextVideoAbsolutePath)
            setVideoEncodingBitRate(10000000)
            setVideoFrameRate(30)
            setVideoSize(videoSize.width, videoSize.height)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            prepare()
        }
    }

    fun getVideoFilePath() :String{
        val state = Environment.getExternalStorageState()
        var myDir = ""
        if (ContextCompat.checkSelfPermission(this.requireActivity(), // request permission when it is not granted.
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("myAppName", "permission:WRITE_EXTERNAL_STORAGE: NOT granted!")
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this.requireActivity(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this.requireActivity(),
                        VIDEO_PERMISSIONS,1
                )
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        if ( Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state) ) {  // we can read the External Storage...

            myDir = baseDir + File.separator + "bodygation" + File.separator
            if(!File(myDir).exists()) {
                val sciezka = File(myDir)
                sciezka.mkdirs()
            }
        }else {
            myDir = baseDir
            //Retrieve the External Storages root directory:

            myDir += File.separator + "bodygation" + File.separator
            if(!File(myDir).exists()) {
                val sciezka = File(myDir)
                sciezka.mkdirs()
            }
        }
        Log.i("Path", myDir + "bodygation_" + System.currentTimeMillis() + ".mp4")
        return myDir
    }

    private fun showToast(message : String) = Toast.makeText(activity, message, LENGTH_SHORT).show()

    private fun startRecordingVideo() {
        if (cameraDevice == null || !textureView.isAvailable) return

        try {
            record_Btn.setImageResource(R.drawable.record);
            closePreviewSession()
            setUpMediaRecorder()
            val texture = textureView.surfaceTexture.apply {
                setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
            }

            // Set up Surface for camera preview and MediaRecorder
            val previewSurface = Surface(texture)
            val recorderSurface = mediaRecorder!!.surface
            val surfaces = ArrayList<Surface>().apply {
                add(previewSurface)
                add(recorderSurface)
            }
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(TEMPLATE_RECORD).apply {
                addTarget(previewSurface)
                addTarget(recorderSurface)
            }

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            cameraDevice?.createCaptureSession(surfaces,
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            captureSession = cameraCaptureSession
                            updatePreview()
                            activity?.runOnUiThread {
                                record_Btn.setImageDrawable(getDrawable(requireActivity(), R.drawable.stop))
                                isRecordingVideo = true
                                mediaRecorder?.start()
                            }
                        }

                        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                            if (activity != null) showToast("Failed")
                        }
                    }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun closePreviewSession() {
        captureSession?.close()
        captureSession = null
    }

    @SuppressLint("RestrictedApi")
    private fun stopRecordingVideo() {
        isRecordingVideo = false
        record_Btn.setImageResource(R.drawable.stop);
        record_Btn.setImageDrawable(getDrawable(this.requireActivity(), R.drawable.record))
        mediaRecorder?.apply {
            stop()
            reset()
        }

        if (activity != null) showToast("Video saved: $nextVideoAbsolutePath")
        nextVideoAbsolutePath = null
        startPreview()
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    class CompareSizesByArea : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size) =
                signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
    }

    class ErrorDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
                AlertDialog.Builder(activity!!)
                        .setMessage(arguments!!.getString(ARG_MESSAGE))
                        .setPositiveButton(android.R.string.ok) { _, _ -> activity!!.finish() }
                        .create()

        companion object {

            @JvmStatic private val ARG_MESSAGE = "message"

            @JvmStatic fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Camera.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
                Camera().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                    }
                }
    }
}
