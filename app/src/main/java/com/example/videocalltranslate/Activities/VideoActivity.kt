package com.example.videocalltranslate.Activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.videocalltranslate.Presenter.CameraCapturerCompat
import com.google.android.material.snackbar.Snackbar
import com.twilio.video.*
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.content_video.*
import com.example.videocalltranslate.R
import com.twilio.video.VideoView
import java.util.*


class VideoActivity : AppCompatActivity() {
    val CAMERA_MIC_PERMISSION_REQUEST_CODE = 1
    val TAG = "VideoActivity";

    /*
     * Audio and video tracks can be created with names. This feature is useful for categorizing
     * tracks of participants. For example, if one participant publishes a video track with
     * ScreenCapturer and CameraCapturer with the names "screen" and "camera" respectively then
     * other participants can use RemoteVideoTrack#getName to determine which video track is
     * produced from the other participant's screen or camera.
     */
    val LOCAL_AUDIO_TRACK_NAME = "mic"
    val LOCAL_VIDEO_TRACK_NAME = "camera"

    /*
     * You must provide a Twilio Access Token to connect to the Video service
     */
    val TWILIO_ACCESS_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImN0eSI6InR3aWxpby1mcGE7dj0xIn0.eyJqdGkiOiJTS2M0ZWViODUwZjQyOTBhYzlhNDJjM2ZjZmUwOWZmMGJhLTE1NzI3NzEwODciLCJpc3MiOiJTS2M0ZWViODUwZjQyOTBhYzlhNDJjM2ZjZmUwOWZmMGJhIiwic3ViIjoiQUM5ZTQ3NWYxOWQ3ZGM1MjU1YzIwNzI4NGViNzIyNDEzMSIsImV4cCI6MTU3Mjc3NDY4NywiZ3JhbnRzIjp7ImlkZW50aXR5IjoiaWQyMzQ1Njc4OSIsInZpZGVvIjp7fX19.cEGPxYsQBPAY9PwH_ReWph8BGwwHAHrhcYbuFEh5S8w"
    //private static final String ACCESS_TOKEN_SERVER = BuildConfig.TWILIO_ACCESS_TOKEN_SERVER;

    /*
     * Access token used to connect. This field will be set either from the console generated token
     * or the request to the token server.
     */
    lateinit var accessToken: String

    /*
     * A Room represents communication between a local participant and one or more participants.
     */
    private  var room :Room? = null
    private var localParticipant: LocalParticipant? = null

    /*
     * AudioCodec and VideoCodec represent the preferred codec for encoding and decoding audio and
     * video.
     */
    private lateinit var audioCodec: AudioCodec
    private lateinit var videoCodec: VideoCodec

    /*
     * Encoding parameters represent the sender side bandwidth constraints.
     */
    private lateinit var encodingParameters: EncodingParameters

    /*
     * A VideoView receives frames from a local or remote video track and renders them
     * to an associated view.
     */
    private lateinit var primaryVideoView: VideoView
    private lateinit var thumbnailVideoView: VideoView

    /*
     * Android shared preferences used for settings
     */
    private lateinit var preferences: SharedPreferences

    /*
     * Android application UI elements
     */
    private lateinit var videoStatusTextView: TextView
    private lateinit var cameraCapturerCompat: CameraCapturerCompat
    private  var localAudioTrack: LocalAudioTrack? = null
    private  var localVideoTrack: LocalVideoTrack? = null
    private lateinit var  connectActionFab: Button
    private lateinit var switchCameraActionFab: Button
    private lateinit var localVideoActionFab: Button
    private lateinit var muteActionFab: Button
    private lateinit var reconnectingProgressBar: ProgressBar
    private lateinit var connectDialog: AlertDialog
    private lateinit var audioManager: AudioManager
    private lateinit var remoteParticipantIdentity: String

    private  var previousAudioMode: Int =0
    private  var previousMicrophoneMute: Boolean = false
    private lateinit var localVideoView: VideoRenderer
    private  var disconnectedFromOnDestroy: Boolean = false
    private var isSpeakerPhoneEnabled = true
    private  var enableAutomaticSubscription: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        primaryVideoView = primary_video_view
        thumbnailVideoView = thumbnail_video_view
        videoStatusTextView = video_status_textview
        reconnectingProgressBar = reconnecting_progress_bar

        connectActionFab = connect_action_fab
        switchCameraActionFab = switch_camera_action_fab
        localVideoActionFab = local_video_action_fab
        muteActionFab = mute_action_fab

        /*
         * Get shared preferences to read settings
         */
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        /*
         * Enable changing the volume using the up/down keys during a conversation
         */
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        /*
         * Needed for setting/abandoning audio focus during call
         */
        audioManager =  getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setSpeakerphoneOn(isSpeakerPhoneEnabled);

        /*
         * Check camera and microphone permissions. Needed in Android M.
         */
        if (!checkPermissionForCameraAndMicrophone()) {
            requestPermissionForCameraAndMicrophone();
        } else {
            createAudioAndVideoTracks()
            setAccessToken()
        }

        /*
         * Set the initial state of the UI
         */
        intializeUI()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_video_activity, menu);
        return true;
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            R.id.menu_settings -> {
                startActivity( Intent (this, SettingsActivity::class.java))
                return true
            }
            R.id.speaker_menu_item -> {
                if (audioManager.isSpeakerphoneOn()) {
                    audioManager.setSpeakerphoneOn(false);
                    item.setIcon(R.drawable.ic_phonelink_ring_white_24dp);
                    isSpeakerPhoneEnabled = false;
                } else {
                    audioManager.setSpeakerphoneOn(true);
                    item.setIcon(R.drawable.ic_volume_up_white_24dp);
                    isSpeakerPhoneEnabled = true
                }

                    return true
            }
            else -> {
                return false
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,  permissions: Array<String>,  grantResults: IntArray) {
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE) {
            var cameraAndMicPermissionGranted = true

            for (grantResult in grantResults) {
                cameraAndMicPermissionGranted = grantResult == PackageManager.PERMISSION_GRANTED
            }

            if (cameraAndMicPermissionGranted) {
                createAudioAndVideoTracks();
                setAccessToken();
            } else {
                Toast.makeText(this,
                    R.string.permissions_needed,
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    override fun onResume() {
        super.onResume()

        /*
         * Update preferred audio and video codec in case changed in settings
         */
        audioCodec = getAudioCodecPreference(SettingsActivity.PREF_AUDIO_CODEC,
            SettingsActivity.PREF_AUDIO_CODEC_DEFAULT);
        videoCodec = getVideoCodecPreference(SettingsActivity.PREF_VIDEO_CODEC,
            SettingsActivity.PREF_VIDEO_CODEC_DEFAULT);
        enableAutomaticSubscription = getAutomaticSubscriptionPreference(SettingsActivity.PREF_ENABLE_AUTOMATIC_SUBSCRIPTION,
            SettingsActivity.PREF_ENABLE_AUTOMATIC_SUBSCRIPTION_DEFAULT)
        /*
         * Get latest encoding parameters
         */
        val newEncodingParameters: EncodingParameters = getEncodingParameters()

        /*
         * If the local video track was released when the app was put in the background, recreate.
         */
        if (localVideoTrack == null && checkPermissionForCameraAndMicrophone()) {
            localVideoTrack = LocalVideoTrack.create(this,
                true,
                cameraCapturerCompat.videoCapturer,
                LOCAL_VIDEO_TRACK_NAME)
            localVideoTrack?.addRenderer(localVideoView)

            /*
             * If connected to a Room then share the local video track.
             */
            if (localParticipant != null) {
                localVideoTrack?.let {
                    localParticipant?.publishTrack(it)
                }

                /*
                 * Update encoding parameters if they have changed.
                 */
                if (!newEncodingParameters.equals(encodingParameters)) {
                    localParticipant?.setEncodingParameters(newEncodingParameters);
                }
            }
        }

        /*
         * Update encoding parameters
         */
        encodingParameters = newEncodingParameters;

        /*
         * Route audio through cached value.
         */
        audioManager.setSpeakerphoneOn(isSpeakerPhoneEnabled);

        /*
         * Update reconnecting UI
         */
        if (room != null) {
            reconnectingProgressBar.visibility =
                if(room?.getState() != Room.State.RECONNECTING){
                    View.GONE
                }
            else{
                    View.VISIBLE
                }
            videoStatusTextView.setText("Connected to " + room?.getName());
        }
    }

    override fun onPause() {
        /*
         * Release the local video track before going in the background. This ensures that the
         * camera can be used by other applications while this app is in the background.
         */
        if (localVideoTrack != null) {
            /*
             * If this local video track is being shared in a Room, unpublish from room before
             * releasing the video track. Participants will be notified that the track has been
             * unpublished.
             */
            if (localParticipant != null) {
                localVideoTrack?.let {
                    localParticipant?.unpublishTrack(it)
                }
            }

            localVideoTrack?.release()
            localVideoTrack = null
        }
        super.onPause()
    }

    override fun onDestroy() {
        /*
         * Always disconnect from the room before leaving the Activity to
         * ensure any memory allocated to the Room resource is freed.
         */
        if (room != null && room?.getState() != Room.State.DISCONNECTED) {
            room?.disconnect();
            disconnectedFromOnDestroy = true;
        }

        /*
         * Release the local audio and video tracks ensuring any memory allocated to audio
         * or video is freed.
         */
        if (localAudioTrack != null) {
            localAudioTrack?.release();
            localAudioTrack = null
        }
        if (localVideoTrack != null) {
            localVideoTrack?.release();
            localVideoTrack = null;
        }

        super.onDestroy();
    }

    private fun checkPermissionForCameraAndMicrophone(): Boolean {
        val resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        val resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return resultCamera == PackageManager.PERMISSION_GRANTED &&
                resultMic == PackageManager.PERMISSION_GRANTED;
    }

    private fun requestPermissionForCameraAndMicrophone() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
            ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this,
                R.string.permissions_needed,
                Toast.LENGTH_LONG).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                 arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                CAMERA_MIC_PERMISSION_REQUEST_CODE)
        }
    }

    private fun createAudioAndVideoTracks() {
        // Share your microphone
        localAudioTrack = LocalAudioTrack.create(this, true, LOCAL_AUDIO_TRACK_NAME);

        // Share your camera
        cameraCapturerCompat =  CameraCapturerCompat(this, getAvailableCameraSource())
        localVideoTrack = LocalVideoTrack.create(this,
            true,
            cameraCapturerCompat.videoCapturer,
            LOCAL_VIDEO_TRACK_NAME);
        primaryVideoView.setMirror(true);
        localVideoTrack?.addRenderer(primaryVideoView);
        localVideoView = primaryVideoView;
    }

    private fun getAvailableCameraSource(): CameraCapturer.CameraSource {
        return if(CameraCapturer.isSourceAvailable(CameraCapturer.CameraSource.FRONT_CAMERA)) {
        (CameraCapturer.CameraSource.FRONT_CAMERA) }
        else {
            (CameraCapturer.CameraSource.BACK_CAMERA)
        }

    }

    fun setAccessToken() {
//        if (!BuildConfig.USE_TOKEN_SERVER) {
//            /*
//             * OPTION 1 - Generate an access token from the getting started portal
//             * https://www.twilio.com/console/video/dev-tools/testing-tools and add
//             * the variable TWILIO_ACCESS_TOKEN setting it equal to the access token
//             * string in your local.properties file.
//             */
            this.accessToken = TWILIO_ACCESS_TOKEN;
//        } else {
//            /*
//             * OPTION 2 - Retrieve an access token from your own web app.
//             * Add the variable ACCESS_TOKEN_SERVER assigning it to the url of your
//             * token server and the variable USE_TOKEN_SERVER=true to your
//             * local.properties file.
//             */
//            retrieveAccessTokenfromServer();
//        }
    }

    private fun connectToRoom(roomName: String) {
        configureAudio(true)
        val connectOptionsBuilder = ConnectOptions.Builder(accessToken)
            .roomName(roomName)

        /*
         * Add local audio track to connect options to share with participants.
         */
        if (localAudioTrack != null) {
            connectOptionsBuilder
                .audioTracks(Collections.singletonList(localAudioTrack));
        }

        /*
         * Add local video track to connect options to share with participants.
         */
        if (localVideoTrack != null) {
            connectOptionsBuilder.videoTracks(Collections.singletonList(localVideoTrack));
        }

        /*
         * Set the preferred audio and video codec for media.
         */
        connectOptionsBuilder.preferAudioCodecs(Collections.singletonList(audioCodec));
        connectOptionsBuilder.preferVideoCodecs(Collections.singletonList(videoCodec));

        /*
         * Set the sender side encoding parameters.
         */
        connectOptionsBuilder.encodingParameters(encodingParameters);

        /*
         * Toggles automatic track subscription. If set to false, the LocalParticipant will receive
         * notifications of track publish events, but will not automatically subscribe to them. If
         * set to true, the LocalParticipant will automatically subscribe to tracks as they are
         * published. If unset, the default is true. Note: This feature is only available for Group
         * Rooms. Toggling the flag in a P2P room does not modify subscription behavior.
         */
        connectOptionsBuilder.enableAutomaticSubscription(enableAutomaticSubscription);

        room = Video.connect(this, connectOptionsBuilder.build(), roomListener())
        setDisconnectAction()
    }

    /*
     * The initial state when there is no active room.
     */
    private fun intializeUI() {
        connectActionFab.background =(ContextCompat.getDrawable(this,
            R.drawable.ic_video_call_white_24dp));
        connectActionFab.setOnClickListener(connectActionClickListener());

        switchCameraActionFab.setOnClickListener(switchCameraClickListener());

        localVideoActionFab.setOnClickListener(localVideoClickListener());

        muteActionFab.setOnClickListener(muteClickListener());
    }

    /*
     * Get the preferred audio codec from shared preferences
     */
    private fun getAudioCodecPreference(key: String, defaultValue: String): AudioCodec {
       val audioCodecName = preferences.getString(key, defaultValue);

        when (audioCodecName) {
             IsacCodec.NAME-> {
                 return  IsacCodec()
             }
             OpusCodec.NAME ->{
                return  OpusCodec()
             }
             PcmaCodec.NAME -> {
                 return PcmaCodec ()
             }
            PcmuCodec.NAME -> {
                return  PcmuCodec()
            }
            G722Codec.NAME -> {
                return  G722Codec()
            }
            else-> {
                return  OpusCodec ()
            }
        }
    }

    /*
     * Get the preferred video codec from shared preferences
     */
    private fun getVideoCodecPreference(key: String, defaultValue: String): VideoCodec {
        val videoCodecName = preferences.getString(key, defaultValue)

        when (videoCodecName) {
            Vp8Codec.NAME-> {
                val simulcast = preferences.getBoolean(
                    SettingsActivity.PREF_VP8_SIMULCAST,
                    SettingsActivity.PREF_VP8_SIMULCAST_DEFAULT
                )
                return Vp8Codec (simulcast)
            }
             H264Codec.NAME-> {
                 return  H264Codec ()
             }
             Vp9Codec.NAME-> {
                 return Vp9Codec ()
             }
            else-> {
                return  Vp8Codec ()
            }
        }
    }

    private fun getAutomaticSubscriptionPreference(key: String, defaultValue: Boolean): Boolean {
        return preferences.getBoolean(key, defaultValue);
    }

    private fun getEncodingParameters(): EncodingParameters {
        val maxAudioBitrate = Integer.parseInt(
            preferences.getString(SettingsActivity.PREF_SENDER_MAX_AUDIO_BITRATE,
                SettingsActivity.PREF_SENDER_MAX_AUDIO_BITRATE_DEFAULT));
        val maxVideoBitrate = Integer.parseInt(preferences.getString(SettingsActivity.PREF_SENDER_MAX_VIDEO_BITRATE,
                SettingsActivity.PREF_SENDER_MAX_VIDEO_BITRATE_DEFAULT));

        return EncodingParameters(maxAudioBitrate, maxVideoBitrate)
    }

    /*
     * The actions performed during disconnect.
     */
    private fun setDisconnectAction() {
        connectActionFab.background = (ContextCompat.getDrawable(this,
            R.drawable.ic_call_end_white_24px));
        connectActionFab.setOnClickListener(disconnectClickListener());
    }

    /*
     * Creates an connect UI dialog
     */
    private fun showConnectDialog() {
        val roomEditText = EditText(this);
        connectDialog = com.example.videocalltranslate.Activities.Dialog.createConnectDialog(roomEditText,
            connectClickListener(roomEditText),
            cancelConnectDialogClickListener(),
            this);
        connectDialog.show();
    }

    /*
     * Called when remote participant joins the room
     */
    private fun addRemoteParticipant(remoteParticipant: RemoteParticipant) {
        /*
         * This app only displays video for one additional participant per Room
         */
        if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
            Snackbar.make(connectActionFab,
                "Multiple participants are not currently support in this UI",
                Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
            return;
        }
        remoteParticipantIdentity = remoteParticipant.getIdentity();
        videoStatusTextView.setText("RemoteParticipant " + remoteParticipantIdentity + " joined");

        /*
         * Add remote participant renderer
         */
        if (remoteParticipant.getRemoteVideoTracks().size > 0) {
            val remoteVideoTrackPublication = remoteParticipant.getRemoteVideoTracks().get(0);

            /*
             * Only render video tracks that are subscribed to
             */
            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                addRemoteParticipantVideo(remoteVideoTrackPublication.getRemoteVideoTrack() as VideoTrack);
            }
        }

        /*
         * Start listening for participant events
         */
        remoteParticipant.setListener(remoteParticipantListener());
    }

    /*
     * Set primary view as renderer for participant video track
     */
    private fun addRemoteParticipantVideo(videoTrack: VideoTrack) {
        moveLocalVideoToThumbnailView();
        primaryVideoView.setMirror(false);
        videoTrack.addRenderer(primaryVideoView);
    }

    private fun moveLocalVideoToThumbnailView() {
        if (thumbnailVideoView.getVisibility() == View.GONE) {
            thumbnailVideoView.setVisibility(View.VISIBLE);
            localVideoTrack?.removeRenderer(primaryVideoView);
            localVideoTrack?.addRenderer(thumbnailVideoView);
            localVideoView = thumbnailVideoView;
            thumbnailVideoView.setMirror(cameraCapturerCompat.cameraSource ==
                    CameraCapturer.CameraSource.FRONT_CAMERA);
        }
    }

    /*
     * Called when remote participant leaves the room
     */
    private fun removeRemoteParticipant(remoteParticipant: RemoteParticipant) {
        videoStatusTextView.setText("RemoteParticipant " + remoteParticipant.getIdentity() +
                " left.");
        if (!remoteParticipant.getIdentity().equals(remoteParticipantIdentity)) {
            return;
        }

        /*
         * Remove remote participant renderer
         */
        if (!remoteParticipant.getRemoteVideoTracks().isEmpty()) {
            val remoteVideoTrackPublication = remoteParticipant.getRemoteVideoTracks().get(0);

            /*
             * Remove video only if subscribed to participant track
             */
            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                removeParticipantVideo(remoteVideoTrackPublication.getRemoteVideoTrack() as VideoTrack);
            }
        }
        moveLocalVideoToPrimaryView();
    }

    private fun removeParticipantVideo(videoTrack: VideoTrack) {
        videoTrack.removeRenderer(primaryVideoView);
    }

    private fun moveLocalVideoToPrimaryView() {
        if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
            thumbnailVideoView.setVisibility(View.GONE);
            localVideoTrack?.let {
                it.removeRenderer(thumbnailVideoView);
                it.addRenderer(primaryVideoView);
            }
            localVideoView = primaryVideoView;
            primaryVideoView.setMirror(cameraCapturerCompat.cameraSource ==
                    CameraCapturer.CameraSource.FRONT_CAMERA);
        }
    }

    /*
     * Room events listener
     */
    private fun  roomListener(): Room.Listener {
        return object: Room.Listener {
            override fun onConnected(room: Room) {
                localParticipant = room.getLocalParticipant()
                videoStatusTextView.setText("Connected to " + room.getName());
                setTitle(room.getName());

                for (remoteParticipant in room.getRemoteParticipants()) {
                addRemoteParticipant(remoteParticipant);
                break;
                }
            }

           override fun onReconnecting(room: Room, twilioException: TwilioException) {
                videoStatusTextView.setText("Reconnecting to " + room.getName());
                reconnectingProgressBar.setVisibility(View.VISIBLE);
            }

            override fun onReconnected(room: Room) {
                videoStatusTextView.setText("Connected to " + room.getName());
                reconnectingProgressBar.setVisibility(View.GONE);
            }

            override fun onConnectFailure(room: Room, e: TwilioException) {
                videoStatusTextView.setText("Failed to connect");
                configureAudio(false);
                intializeUI();
            }

            override fun onDisconnected(room: Room, twilioException: TwilioException?) {
                localParticipant = null;
                videoStatusTextView.setText("Disconnected from " + room.getName());
                reconnectingProgressBar.setVisibility(View.GONE);
                this@VideoActivity.room = null;
                // Only reinitialize the UI if disconnect was not called from onDestroy()
                if (!disconnectedFromOnDestroy) {
                    configureAudio(false);
                    intializeUI();
                    moveLocalVideoToPrimaryView();
                }
            }

            override fun onParticipantConnected(room: Room, remoteParticipant: RemoteParticipant) {
                addRemoteParticipant(remoteParticipant);

            }

            override fun onParticipantDisconnected(room: Room, remoteParticipant: RemoteParticipant) {
                removeRemoteParticipant(remoteParticipant)
            }

            override fun onRecordingStarted(room: Room) {
                /*
                 * Indicates when media shared to a Room is being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
                Log.d(TAG, "onRecordingStarted");
            }

           override fun onRecordingStopped(room: Room) {
                /*
                 * Indicates when media shared to a Room is no longer being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
                Log.d(TAG, "onRecordingStopped");
            }
        };
    }

    private fun remoteParticipantListener(): RemoteParticipant.Listener {
        return object :RemoteParticipant.Listener {
            override fun onAudioTrackPublished(remoteParticipant: RemoteParticipant, remoteAudioTrackPublication: RemoteAudioTrackPublication) {
                Log.i(TAG, String.format("onAudioTrackPublished: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteAudioTrackPublication: sid=%s, enabled=%b, " +
                        "subscribed=%b, name=%s]",
                    remoteParticipant.getIdentity(),
                    remoteAudioTrackPublication.getTrackSid(),
                    remoteAudioTrackPublication.isTrackEnabled(),
                    remoteAudioTrackPublication.isTrackSubscribed(),
                    remoteAudioTrackPublication.getTrackName()));
                videoStatusTextView.setText("onAudioTrackPublished");
            }

            override fun onAudioTrackUnpublished(remoteParticipant: RemoteParticipant, remoteAudioTrackPublication: RemoteAudioTrackPublication) {
                Log.i(TAG, String.format("onAudioTrackUnpublished: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteAudioTrackPublication: sid=%s, enabled=%b, " +
                        "subscribed=%b, name=%s]",
                    remoteParticipant.getIdentity(),
                    remoteAudioTrackPublication.getTrackSid(),
                    remoteAudioTrackPublication.isTrackEnabled(),
                    remoteAudioTrackPublication.isTrackSubscribed(),
                    remoteAudioTrackPublication.getTrackName()));
                videoStatusTextView.setText("onAudioTrackUnpublished");
            }

            override fun onDataTrackPublished(remoteParticipant: RemoteParticipant, remoteDataTrackPublication: RemoteDataTrackPublication) {
                Log.i(TAG, String.format("onDataTrackPublished: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteDataTrackPublication: sid=%s, enabled=%b, " +
                        "subscribed=%b, name=%s]",
                    remoteParticipant.getIdentity(),
                    remoteDataTrackPublication.getTrackSid(),
                    remoteDataTrackPublication.isTrackEnabled(),
                    remoteDataTrackPublication.isTrackSubscribed(),
                    remoteDataTrackPublication.getTrackName()));
                videoStatusTextView.setText("onDataTrackPublished");
            }

            override fun onDataTrackUnpublished(remoteParticipant: RemoteParticipant, remoteDataTrackPublication: RemoteDataTrackPublication) {
                Log.i(TAG, String.format("onDataTrackUnpublished: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteDataTrackPublication: sid=%s, enabled=%b, " +
                        "subscribed=%b, name=%s]",
                    remoteParticipant.getIdentity(),
                    remoteDataTrackPublication.getTrackSid(),
                    remoteDataTrackPublication.isTrackEnabled(),
                    remoteDataTrackPublication.isTrackSubscribed(),
                    remoteDataTrackPublication.getTrackName()));
                videoStatusTextView.setText("onDataTrackUnpublished");
            }

            override fun onVideoTrackPublished(remoteParticipant: RemoteParticipant, remoteVideoTrackPublication: RemoteVideoTrackPublication) {
                Log.i(TAG, String.format("onVideoTrackPublished: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteVideoTrackPublication: sid=%s, enabled=%b, " +
                        "subscribed=%b, name=%s]",
                    remoteParticipant.getIdentity(),
                    remoteVideoTrackPublication.getTrackSid(),
                    remoteVideoTrackPublication.isTrackEnabled(),
                    remoteVideoTrackPublication.isTrackSubscribed(),
                    remoteVideoTrackPublication.getTrackName()));
                videoStatusTextView.setText("onVideoTrackPublished");
            }

           override fun onVideoTrackUnpublished(remoteParticipant: RemoteParticipant, remoteVideoTrackPublication: RemoteVideoTrackPublication) {
                Log.i(TAG, String.format("onVideoTrackUnpublished: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteVideoTrackPublication: sid=%s, enabled=%b, " +
                        "subscribed=%b, name=%s]",
                    remoteParticipant.getIdentity(),
                    remoteVideoTrackPublication.getTrackSid(),
                    remoteVideoTrackPublication.isTrackEnabled(),
                    remoteVideoTrackPublication.isTrackSubscribed(),
                    remoteVideoTrackPublication.getTrackName()));
                videoStatusTextView.setText("onVideoTrackUnpublished");
            }

            override fun onAudioTrackSubscribed(remoteParticipant: RemoteParticipant, remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                                remoteAudioTrack: RemoteAudioTrack) {
                Log.i(TAG, String.format("onAudioTrackSubscribed: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteAudioTrack: enabled=%b, playbackEnabled=%b, name=%s]",
                    remoteParticipant.getIdentity(),
                    remoteAudioTrack.isEnabled(),
                    remoteAudioTrack.isPlaybackEnabled(),
                    remoteAudioTrack.getName()));
                videoStatusTextView.setText("onAudioTrackSubscribed");
            }

            override fun onAudioTrackUnsubscribed(remoteParticipant: RemoteParticipant, remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                                  remoteAudioTrack: RemoteAudioTrack) {
                Log.i(TAG, String.format("onAudioTrackUnsubscribed: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteAudioTrack: enabled=%b, playbackEnabled=%b, name=%s]",
                    remoteParticipant.getIdentity(),
                    remoteAudioTrack.isEnabled(),
                    remoteAudioTrack.isPlaybackEnabled(),
                    remoteAudioTrack.getName()));
                videoStatusTextView.setText("onAudioTrackUnsubscribed");
            }

            override fun onAudioTrackSubscriptionFailed(remoteParticipant: RemoteParticipant, remoteAudioTrackPublication: RemoteAudioTrackPublication,
                                                        twilioException: TwilioException) {
                Log.i(TAG, String.format("onAudioTrackSubscriptionFailed: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteAudioTrackPublication: sid=%b, name=%s]" +
                        "[TwilioException: code=%d, message=%s]",
                    remoteParticipant.getIdentity(),
                    remoteAudioTrackPublication.getTrackSid(),
                    remoteAudioTrackPublication.getTrackName(),
                    twilioException.getCode(),
                    twilioException.message));
                videoStatusTextView.setText("onAudioTrackSubscriptionFailed");
            }

            override fun onDataTrackSubscribed(remoteParticipant: RemoteParticipant, remoteDataTrackPublication: RemoteDataTrackPublication,
                                               remoteDataTrack: RemoteDataTrack) {
                Log.i(TAG, String.format("onDataTrackSubscribed: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteDataTrack: enabled=%b, name=%s]",
                    remoteParticipant.getIdentity(),
                    remoteDataTrack.isEnabled(),
                    remoteDataTrack.getName()));
                videoStatusTextView.setText("onDataTrackSubscribed");
            }

            override fun onDataTrackUnsubscribed(remoteParticipant: RemoteParticipant, remoteDataTrackPublication:RemoteDataTrackPublication,
                                                 remoteDataTrack: RemoteDataTrack) {
                Log.i(TAG, String.format("onDataTrackUnsubscribed: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteDataTrack: enabled=%b, name=%s]",
                    remoteParticipant.getIdentity(),
                    remoteDataTrack.isEnabled(),
                    remoteDataTrack.getName()));
                videoStatusTextView.setText("onDataTrackUnsubscribed");
            }

            override fun onDataTrackSubscriptionFailed(remoteParticipant: RemoteParticipant, remoteDataTrackPublication: RemoteDataTrackPublication,
                                                       twilioException: TwilioException) {
                Log.i(TAG, String.format("onDataTrackSubscriptionFailed: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteDataTrackPublication: sid=%b, name=%s]" +
                        "[TwilioException: code=%d, message=%s]",
                    remoteParticipant.getIdentity(),
                    remoteDataTrackPublication.getTrackSid(),
                    remoteDataTrackPublication.getTrackName(),
                    twilioException.getCode(),
                    twilioException.message));
                videoStatusTextView.setText("onDataTrackSubscriptionFailed");
            }

            override fun onVideoTrackSubscribed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                remoteVideoTrack: RemoteVideoTrack
            ) {
                Log.i(TAG, String.format("onVideoTrackSubscribed: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteVideoTrack: enabled=%b, name=%s]",
                    remoteParticipant.getIdentity(),
                    remoteVideoTrack.isEnabled(),
                    remoteVideoTrack.getName()));
                videoStatusTextView.setText("onVideoTrackSubscribed");
                addRemoteParticipantVideo(remoteVideoTrack);
            }

            override fun onVideoTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                remoteVideoTrack: RemoteVideoTrack
            ) {
                Log.i(TAG, String.format("onVideoTrackUnsubscribed: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteVideoTrack: enabled=%b, name=%s]",
                    remoteParticipant.getIdentity(),
                    remoteVideoTrack.isEnabled(),
                    remoteVideoTrack.getName()));
                videoStatusTextView.setText("onVideoTrackUnsubscribed");
                removeParticipantVideo(remoteVideoTrack);
            }

            override fun onVideoTrackSubscriptionFailed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                twilioException: TwilioException
            ) {
                Log.i(TAG, String.format("onVideoTrackSubscriptionFailed: " +
                        "[RemoteParticipant: identity=%s], " +
                        "[RemoteVideoTrackPublication: sid=%b, name=%s]" +
                        "[TwilioException: code=%d, message=%s]",
                    remoteParticipant.getIdentity(),
                    remoteVideoTrackPublication.getTrackSid(),
                    remoteVideoTrackPublication.getTrackName(),
                    twilioException.getCode(),
                    twilioException.message));
                videoStatusTextView.setText("onVideoTrackSubscriptionFailed");
                Snackbar.make(connectActionFab,
                    String.format("Failed to subscribe to %s video track",
                        remoteParticipant.getIdentity()),
                    Snackbar.LENGTH_LONG)
                    .show();
            }

            override fun onAudioTrackEnabled(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {}

            override fun onAudioTrackDisabled(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {

            }

            override fun onVideoTrackEnabled(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {

            }

            override fun onVideoTrackDisabled(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {

            }
        };
    }

    private fun connectClickListener(roomEditText: EditText): DialogInterface.OnClickListener  {
        return object: DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                connectToRoom(roomEditText.getText().toString());
            }
        };
    }

    private fun disconnectClickListener(): View.OnClickListener {
        return object : View.OnClickListener{
            override fun onClick(v: View?) {
                /*
             * Disconnect from room
             */
                room?.let {
                    it.disconnect();
                }
                intializeUI();
            }
        };
    }

    private fun connectActionClickListener():  View.OnClickListener {
        return object : View.OnClickListener{
            override fun onClick(v: View?) {
                showConnectDialog()
            }
        }
    }

    private fun cancelConnectDialogClickListener(): DialogInterface.OnClickListener {
        return object: DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                intializeUI();
                connectDialog.dismiss();
            }
        }


    }

    private fun switchCameraClickListener(): View.OnClickListener {
        return object: View.OnClickListener {
            override fun onClick(v: View?) {
                if (cameraCapturerCompat != null) {
                    val cameraSource = cameraCapturerCompat.cameraSource
                    cameraCapturerCompat.switchCamera();
                    if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
                        thumbnailVideoView.setMirror(cameraSource == CameraCapturer.CameraSource.BACK_CAMERA);
                    } else {
                        primaryVideoView.setMirror(cameraSource == CameraCapturer.CameraSource.BACK_CAMERA);
                    }
                }
            }
        };
    }

    private fun localVideoClickListener():View.OnClickListener {
        return object: View.OnClickListener {
            override fun onClick(v: View?) {
                /*
             * Enable/disable the local video track
             */
                if (localVideoTrack != null) {
                    val enable = !(localVideoTrack?.isEnabled() ?: false)
                    localVideoTrack?.enable(enable)
                    val icon: Int
                    if (enable) {
                        icon = R.drawable.ic_videocam_white_24dp;
                    } else {
                        icon = R.drawable.ic_videocam_off_black_24dp;
                    }
                    localVideoActionFab.background = (
                        ContextCompat.getDrawable(this@VideoActivity, icon)
                    );
                }
            }
        };
    }

    private fun muteClickListener(): View.OnClickListener {
        return object: View.OnClickListener {
            override fun onClick(v: View?) {
                /*
             * Enable/disable the local audio track. The results of this operation are
             * signaled to other Participants in the same Room. When an audio track is
             * disabled, the audio is muted.
             */
                if (localAudioTrack != null) {
                    val enable = !(localAudioTrack?.isEnabled() ?: false)
                    localAudioTrack?.enable(enable);
                    val  icon = if(enable ) {
                        R.drawable.ic_mic_white_24dp
                    }
                    else {
                        R.drawable.ic_mic_off_black_24dp
                    }
                    muteActionFab.background = (
                        ContextCompat.getDrawable(
                            this@VideoActivity, icon
                        )
                    );
                }
            }
        };
    }

//    private fun retrieveAccessTokenfromServer() {
//        Ion.with(this)
//            .load(String.format("%s?identity=%s", ACCESS_TOKEN_SERVER,
//                UUID.randomUUID().toString()))
//            .asString()
//            .setCallback((e, token) -> {
//            if (e == null) {
//                VideoActivity.this.accessToken = token;
//            } else {
//                Toast.makeText(VideoActivity.this,
//                    R.string.error_retrieving_access_token, Toast.LENGTH_LONG)
//                    .show();
//            }
//        });
//    }

    private fun configureAudio(enable: Boolean) {
        if (enable) {
            previousAudioMode = audioManager.getMode();
            // Request audio focus before making any device switch
            requestAudioFocus();
            /*
             * Use MODE_IN_COMMUNICATION as the default audio mode. It is required
             * to be in this mode when playout and/or recording starts for the best
             * possible VoIP performance. Some devices have difficulties with
             * speaker mode if this is not set.
             */
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            /*
             * Always disable microphone mute during a WebRTC call.
             */
            previousMicrophoneMute = audioManager.isMicrophoneMute();
            audioManager.setMicrophoneMute(false);
        } else {
            audioManager.setMode(previousAudioMode);
            audioManager.abandonAudioFocus(null);
            audioManager.setMicrophoneMute(previousMicrophoneMute);
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes =  AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(object : AudioManager.OnAudioFocusChangeListener{
                    override fun onAudioFocusChange(focusChange: Int) {} })
                .build()
            audioManager.requestAudioFocus(focusRequest);
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
    }
}
