//package com.xinbida.rtc.p2p;
//
//import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
//
//import android.Manifest;
//import android.content.Context;
//import android.content.Intent;
//import android.media.AudioManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.CountDownTimer;
//import android.os.Handler;
//import android.os.Looper;
//import android.text.TextUtils;
//import android.view.KeyEvent;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.Window;
//import android.view.WindowManager;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.widget.AppCompatImageView;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.core.view.ViewCompat;
//
//import com.xinbida.rtc.WKRTCApplication;
//import com.xinbida.rtc.RTCBaseActivity;
//import com.xinbida.rtc.WKRTCCallType;
//import com.xinbida.rtc.R;
//import com.xinbida.rtc.inters.ILocalListener;
//import com.xinbida.rtc.utils.CommonUtils;
//import com.xinbida.rtc.utils.RTCAudioPlayer;
//import com.xinbida.rtc.utils.WKRTCManager;
//import com.yhao.floatwindow.FloatWindow;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.webrtc.RendererCommon;
//import org.webrtc.SurfaceViewRenderer;
//
//import java.util.HashMap;
//import java.util.Objects;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import owt.base.ActionCallback;
//import owt.base.LocalStream;
//import owt.base.MediaConstraints;
//import owt.base.OwtError;
//import owt.p2p.P2PClient;
//import owt.p2p.Publication;
//import owt.p2p.RemoteStream;
//import owt.utils.OwtVideoCapturer;
//
///**
// * 5/10/21 4:20 PM
// * 两人通话
// */
//public class P2PCallActivity extends RTCBaseActivity implements P2PClient.P2PClientObserver {
//    private AppCompatImageView avatarIv;
//    private TextView nameTv, timeTv;
//    private TextView connectTv;
//    private SurfaceViewRenderer smallSurfaceView;
//    private SurfaceViewRenderer fullSurfaceView;
//    private float dX, dY;
//    private View answerLayout, videoLayout, audioLayout;
//    private AppCompatImageView answerIV, switchCameraIV, hangUpIV, switchAudioIV, switchVideoIV, muteIV, speakerIV;
//
//    private int callType;
//    private String loginUID;
//    private String toUID, toName;
//    private boolean isCreate;
//    private long totalDuration;
//    private P2PClient p2PClient;
//    private LocalStream localStream;
//    private RemoteStream remoteStream;
//    private final ExecutorService executor = Executors.newSingleThreadExecutor();
//    private OwtVideoCapturer capturer;
//    private Publication publication;
//    AudioManager audioManager;
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        isCreate = getIntent().getBooleanExtra("isCreate", false);
//        if (isCreate) {
//            overridePendingTransition(R.anim.top_in, R.anim.top_silent);
//        }
//        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        Window window = getWindow();
//        setContentView(R.layout.act_p2p_call_layout);
//        CommonUtils.setStatusBarColor(window, ContextCompat.getColor(this, R.color.color232323), 0);
//        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        //  audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
//        toName = getIntent().getStringExtra("toName");
//        toUID = getIntent().getStringExtra("toUID");
//        loginUID = getIntent().getStringExtra("loginUID");
//        callType = getIntent().getIntExtra("callType", 1);
//        initView();
//        initListener();
//        initP2PClient();
//    }
//
//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        initListener();
//    }
//
//    private void initView() {
//        answerLayout = findViewById(R.id.answerLayout);
//        audioLayout = findViewById(R.id.audioLayout);
//        videoLayout = findViewById(R.id.videoLayout);
//        answerIV = findViewById(R.id.answerIV);
//        speakerIV = findViewById(R.id.speakerIV);
//        muteIV = findViewById(R.id.muteIV);
//        switchVideoIV = findViewById(R.id.switchVideoIV);
//        switchCameraIV = findViewById(R.id.switchCameraIV);
//        switchAudioIV = findViewById(R.id.switchAudioIV);
//        hangUpIV = findViewById(R.id.hangUpIV);
//        connectTv = findViewById(R.id.connectTv);
//        timeTv = findViewById(R.id.timeTv);
//        nameTv = findViewById(R.id.nameTv);
//        avatarIv = findViewById(R.id.avatarIv);
//
//        smallSurfaceView = findViewById(R.id.smallRenderer);
//        smallSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
//        smallSurfaceView.setEnableHardwareScaler(true);
//        smallSurfaceView.setOnTouchListener(touchListener);
//        smallSurfaceView.setZOrderMediaOverlay(true);
//        smallSurfaceView.init(WKRTCApplication.getInstance().getRootEglBase().getEglBaseContext(), null);
//
//
//        fullSurfaceView = findViewById(R.id.fullRenderer);
//        fullSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
//        fullSurfaceView.setEnableHardwareScaler(true);
//        fullSurfaceView.setZOrderMediaOverlay(false);
//        fullSurfaceView.init(WKRTCApplication.getInstance().getRootEglBase().getEglBaseContext(), null);
//        nameTv.setText(toName);
//        if (WKRTCManager.getInstance().getImageLoader() != null) {
//            WKRTCManager.getInstance().getImageLoader().onImageLoader(this, toUID, avatarIv);
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            ViewCompat.setTransitionName(hangUpIV, "hangup");
//            ViewCompat.setTransitionName(nameTv, "nameTv");
//            ViewCompat.setTransitionName(avatarIv, "avatarIv");
//        }
//        boolean isConnect = getIntent().getBooleanExtra("is_connect", false);
//        if (callType == WKRTCCallType.audio) {
//            //音频通话
//            fullSurfaceView.setVisibility(View.GONE);
//            smallSurfaceView.setVisibility(View.GONE);
//            avatarIv.setVisibility(View.VISIBLE);
//            nameTv.setVisibility(View.VISIBLE);
//            if (!isConnect)
//                RTCAudioPlayer.getInstance().play(this, "lim_rtc_call.mp3", true);
//            else WKRTCManager.getInstance().getSendMsgListener().sendSwitchAudio(loginUID, toUID);
//            audioManager.setSpeakerphoneOn(true);//是否从扬声器播出
//        } else {
//            //  audioManager.setMode(AudioManager.MODE_IN_CALL);
//            audioManager.setSpeakerphoneOn(true);
//
//        }
//        if (isCreate) {
//            countDownTimer.start();
//        } else {
//            if (callType == WKRTCCallType.audio)
//                answerLayout.setVisibility(View.VISIBLE);
//        }
//        executor.execute(() -> {
//            if (capturer == null) {
//                capturer = OwtVideoCapturer.create(320, 240, 30, true, true);
//                localStream = new LocalStream(capturer,
//                        new MediaConstraints.AudioTrackConstraints());
//            }
//            if (callType == WKRTCCallType.audio) {
//                localStream.disableVideo();
//            }
//            localStream.attach(fullSurfaceView);
//            if (isCreate || callType == WKRTCCallType.video || isConnect)
//                connect();
//        });
//    }
//
//    private boolean isPublish = true;
//
////    private void getStatus() {
////        p2PClient.getStats(toUID, new ActionCallback<RTCStatsReport>() {
////            @Override
////            public void onSuccess(RTCStatsReport result) {
////                if (isCreate) {
////                    publish();
////                }
////            }
////
////            @Override
////            public void onFailure(OwtError error) {
////                new Thread() {
////                    @Override
////                    public void run() {
////                        super.run();
////                        try {
////                            Thread.sleep(1000);
////                        } catch (InterruptedException e) {
////                            e.printStackTrace();
////                        }
////                        getStatus();
////                    }
////                }.start();
////            }
////        });
////    }
//
//    private void initListener() {
//
//        WKRTCManager.getInstance().addLocalListener(new ILocalListener() {
//            @Override
//            public void onReceivedRTCMsg(String uid, String message) {
//                p2PClient.onMessage(uid, message);
//            }
//
//            @Override
//            public void onHangUp(String channelID, byte channelType, int second) {
//                if (channelID.equals(toUID) && channelType == 1) {
//                    hangup();
//                    // LiMRTCManager.getInstance().getSaveMsgListener().onHangUp(isCreate, toUID, LiMRTCManager.getInstance().getTotalTime(second * 1000L), callType);
//                }
//            }
//
//            @Override
//            public void onRequestSwitchVideo(String uid) {
//                AlertDialog.Builder builder = new AlertDialog.Builder(P2PCallActivity.this);
//                builder.setMessage("对方请求切换到视频通话");
//                builder.setPositiveButton("同意", (dialog, which) -> {
//                    dialog.dismiss();
//                    runOnUiThread(() -> {
//                        localStream.enableVideo();
//                        switchType(WKRTCCallType.video);
//                    });
//                    WKRTCManager.getInstance().getSendMsgListener().sendSwitchVideoRespond(loginUID, toUID, 1);
//                });
//                builder.setNegativeButton("取消", (dialog, which) -> {
//                    dialog.dismiss();
//                    WKRTCManager.getInstance().getSendMsgListener().sendSwitchVideoRespond(loginUID, toUID, 0);
//                });
//                builder.show();
//            }
//
//            @Override
//            public void onAccept(String uid, int callType) {
//                ILocalListener.super.onAccept(uid, callType);
//                if (remoteStream == null) {
//                    runOnUiThread(() -> connectTv.setVisibility(View.VISIBLE));
//                }
//            }
//
//            @Override
//            public void onRefuse(String channelID, byte channelType, String uid) {
//                if (channelType == 1 && channelID.equals(toUID)) {
//                    hangup();
//                    runOnUiThread(() -> Toast.makeText(P2PCallActivity.this, getString(R.string.other_refuse), Toast.LENGTH_SHORT).show());
//                    // LiMRTCManager.getInstance().getSaveMsgListener().onRefuse(isCreate, toUID, callType);
//                }
//            }
//
//            @Override
//            public void onSwitchVideoRespond(String uid, int status) {
//                if (status == 0)
//                    runOnUiThread(() -> Toast.makeText(P2PCallActivity.this, getString(R.string.other_refuse_switch_video), Toast.LENGTH_SHORT).show());
//                else {
//                    localStream.enableVideo();
//                    runOnUiThread(() -> switchType(WKRTCCallType.video));
//                }
//            }
//
//            @Override
//            public void onCancel(String uid) {
//                if (uid.equals(toUID)) {
//                    hangup();
//                }
//            }
//
//            @Override
//            public void onPublish() {
//                if (isPublish) {
//                    isPublish = false;
//                    new Thread() {
//                        @Override
//                        public void run() {
//                            super.run();
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
////                            getStatus();
//                            if (isCreate) {
//                                publish();
//                            }
//                        }
//                    }.start();
//                }
//            }
//
//            @Override
//            public void onSwitchAudio(String uid) {
//                if (uid.equals(toUID)) {
//                    callType = WKRTCCallType.audio;
//                    runOnUiThread(() -> {
//                        //音频通话
//                        switchType(WKRTCCallType.audio);
//                    });
//                }
//            }
//
//        });
//        WKRTCManager.getInstance().addTimerListener("p2pCall", (time, timeText) -> {
//            runOnUiThread(() -> {
//                totalDuration = time;
//                timeTv.setText(timeText);
//            });
//        });
//        findViewById(R.id.answerIV).setOnClickListener(v -> {
//            // 接听 todo
//            RTCAudioPlayer.getInstance().stopPlay();
//            answerLayout.setVisibility(View.GONE);
//            connect();
//            WKRTCManager.getInstance().getSendMsgListener().sendAccept(loginUID, toUID, WKRTCCallType.audio);
//        });
////        findViewById(R.id.waitHangUpView).setOnClickListener(v -> {
////            // 拒绝
////            LiMAudioPlayer.getInstance().stopPlay();
////            LiMRTCManager.getInstance().getSaveMsgListener().onRefuse(isCreate, toUID, LiMRTCCallType.audio);
////            LiMRTCManager.getInstance().getSendMsgListener().sendRefuse(toUID, (byte) 1);
////            new Handler(Looper.myLooper()).postDelayed(() -> {
////                LiMAudioPlayer.getInstance().play(P2PCallActivity.this, "lim_rtc_hangup.wav", false);
////                finish();
////            }, 500);
////        });
//        switchVideoIV.setOnClickListener(view -> {
//            if (remoteStream == null || localStream == null) return;
//            WKRTCManager.getInstance().getSendMsgListener().sendSwitchVideo(loginUID, toUID);
//            Toast.makeText(this, getString(R.string.request_video_send), Toast.LENGTH_SHORT).show();
//        });
//        // 切换音频
//        switchAudioIV.setOnClickListener(v -> {
//            if (remoteStream == null || localStream == null) return;
//            callType = WKRTCCallType.audio;
//            fullSurfaceView.setVisibility(View.GONE);
//            smallSurfaceView.setVisibility(View.GONE);
//            audioLayout.setVisibility(View.VISIBLE);
//            videoLayout.setVisibility(View.GONE);
//            nameTv.setVisibility(View.VISIBLE);
//            avatarIv.setVisibility(View.VISIBLE);
//            WKRTCManager.getInstance().getSendMsgListener().sendSwitchAudio(loginUID, toUID);
//        });
//        // 静音
//        muteIV.setOnClickListener(v -> {
//            if (remoteStream == null || localStream == null) return;
//            HashMap<String, String> hashMap = localStream.getAttributes();
//            if (hashMap == null) hashMap = new HashMap<>();
//            String rtc_mute = "open";
//            if (hashMap.containsKey("rtc_mute")) {
//                rtc_mute = hashMap.get("rtc_mute");
//            }
//
//            if (!TextUtils.isEmpty(rtc_mute) && TextUtils.equals(rtc_mute, "open")) {
//                localStream.disableAudio();
//                muteIV.setImageResource(R.mipmap.ic_mute_hover);
//                hashMap.put("rtc_mute", "close");
//            } else {
//                localStream.enableAudio();
//                muteIV.setImageResource(R.mipmap.ic_mute);
//                hashMap.put("rtc_mute", "open");
//            }
//            localStream.setAttributes(hashMap);
//        });
//        // 免提
//        speakerIV.setOnClickListener(v -> {
//            if (remoteStream == null || localStream == null) return;
//            //    audioManager.setMode(AudioManager.MODE_IN_CALL);
//            audioManager.setSpeakerphoneOn(!audioManager.isSpeakerphoneOn());
//            if (audioManager.isSpeakerphoneOn()) {
//                speakerIV.setImageResource(R.mipmap.ic_handfree_hover);
//            } else speakerIV.setImageResource(R.mipmap.ic_handfree);
//        });
//        // 切换前后摄像头
//        switchCameraIV.setOnClickListener(v -> {
//            if (remoteStream == null || localStream == null) return;
//            capturer.switchCamera();
//        });
//        // 小窗口
//        findViewById(R.id.minimizeIv).setOnClickListener(v -> showFloatingView(callType == WKRTCCallType.audio ? null : remoteStream, true));
//        // 挂断
//        hangUpIV.setOnClickListener(v -> {
//            RTCAudioPlayer.getInstance().stopPlay();
//
//            if (!isCreate) {
//                if (localStream == null || remoteStream == null) {
//                    // 拒绝
//                    //  LiMRTCManager.getInstance().getSaveMsgListener().onRefuse(isCreate, toUID, LiMRTCCallType.audio);
//                    WKRTCManager.getInstance().getSendMsgListener().sendRefuse(toUID, (byte) 1, callType);
//                    new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(() -> {
//                        RTCAudioPlayer.getInstance().play(P2PCallActivity.this, "lim_rtc_hangup.wav", false);
//                        finish();
//                    }, 500);
//                } else {
//                    WKRTCManager.getInstance().getSendMsgListener().sendHangUpMsg(toUID, (int) (totalDuration / 1000), callType, isCreate ? 1 : 0);
//                    //  WKRTCManager.getInstance().getSaveMsgListener().onHangUp(isCreate, toUID, timeTv.getText().toString(), callType);
//                }
//            } else {
//                if (remoteStream == null) {
//                    WKRTCManager.getInstance().getSendMsgListener().sendCancel(toUID, callType);
//                    //  WKRTCManager.getInstance().getSaveMsgListener().onCancel(isCreate, toUID, callType);
//                } else {
//                    WKRTCManager.getInstance().getSendMsgListener().sendHangUpMsg(toUID, (int) (totalDuration / 1000), callType, isCreate ? 1 : 0);
//                    // WKRTCManager.getInstance().getSaveMsgListener().onHangUp(isCreate, toUID, timeTv.getText().toString(), callType);
//                }
//            }
//            hangup();
//        });
//        smallSurfaceView.setOnClickListener(v -> {
//            if (remoteStream == null || localStream == null) return;
//            if (isAttachLocal) {
//                localStream.detach(fullSurfaceView);
//                remoteStream.detach(smallSurfaceView);
//
//                localStream.attach(smallSurfaceView);
//                remoteStream.attach(fullSurfaceView);
//            } else {
//                localStream.detach(smallSurfaceView);
//                remoteStream.detach(fullSurfaceView);
//
//                localStream.attach(fullSurfaceView);
//                remoteStream.attach(smallSurfaceView);
//            }
//            isAttachLocal = !isAttachLocal;
//        });
//    }
//
//    private boolean isAttachLocal = true;
//
//    private void hangup() {
//        audioManager.setSpeakerphoneOn(false);
//        stopTimer();
//        isPublish = true;
//        if (publication != null) {
//            publication.stop();
//            publication = null;
//        }
//        if (capturer != null) {
//            capturer.stopCapture();
//            capturer.dispose();
//            capturer = null;
//        }
//        if (localStream != null) {
//            localStream.dispose();
//            localStream = null;
//        }
//        executor.execute(() -> {
//            p2PClient.removeObserver(this);
//            p2PClient.onServerDisconnected();
//            p2PClient.stop(toUID);
//            p2PClient.disconnect();
//        });
//        WKRTCManager.getInstance().isCalling = false;
//        WKRTCApplication.getInstance().getRootEglBase().releaseSurface();
//        FloatWindow.destroy();
//        runOnUiThread(() -> {
//            // RTCAudioPlayer.getInstance().play(P2PCallActivity.this, "lim_rtc_hangup.wav", false);
//            WKRTCManager.getInstance().stopTimer();
//            finish();
//        });
//
//    }
//
//    private void initP2PClient() {
//        p2PClient = WKRTCApplication.getInstance().getP2PClient();
//        p2PClient.addObserver(this);
//    }
//
//    private void connect() {
//        executor.execute(() -> {
//            JSONObject loginObj = new JSONObject();
//            try {
//                loginObj.put("uid", loginUID);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            p2PClient.addAllowedRemotePeer(toUID);
//
//            p2PClient.connect(loginObj.toString(), new ActionCallback<String>() {
//                @Override
//                public void onSuccess(String result) {
//                    requestPermission();
//                }
//
//                @Override
//                public void onFailure(OwtError error) {
//                    new Thread() {
//                        @Override
//                        public void run() {
//                            super.run();
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            connect();
//                        }
//                    }.start();
//                }
//            });
//        });
//    }
//
//    private void requestPermission() {
//        String[] permissions = new String[]{Manifest.permission.CAMERA,
//                Manifest.permission.RECORD_AUDIO};
//
//        for (String permission : permissions) {
//            if (ContextCompat.checkSelfPermission(P2PCallActivity.this,
//                    permission) != PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(P2PCallActivity.this,
//                        permissions,
//                        100);
//                return;
//            }
//        }
//        if (!isCreate) {
//            runOnUiThread(() -> {
//                //  waitView.setVisibility(View.GONE);
//                answerIV.setVisibility(View.VISIBLE);
//            });
//            publish();
//        }
//
//    }
//
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == 100
//                && grantResults.length == 2
//                && grantResults[0] == PERMISSION_GRANTED
//                && grantResults[1] == PERMISSION_GRANTED) {
//            executor.execute(new Runnable() {
//                @Override
//                public void run() {
////                    if (capturer == null) {
//                        capturer = OwtVideoCapturer.create(320, 240, 30, true, true);
//                        localStream = new LocalStream(capturer,
//                                new MediaConstraints.AudioTrackConstraints());
////                    }
//                    if (callType == WKRTCCallType.audio) {
//                        localStream.disableVideo();
//                    }
//                    localStream.attach(fullSurfaceView);
//                }
//            });
//            if (!isCreate) {
//                runOnUiThread(() -> {
//                    // waitView.setVisibility(View.GONE);
//                    answerIV.setVisibility(View.VISIBLE);
//                });
//                publish();
//            }
//        }
//
//    }
//
//    private void publish() {
//        if (publication != null || localStream == null) return;
//        //  p2PClient.addAllowedRemotePeer(toUID);
//        executor.execute(
//                () -> p2PClient.publish(toUID, localStream, new ActionCallback<Publication>() {
//                    @Override
//                    public void onSuccess(Publication result) {
//                        publication = result;
//                    }
//
//                    @Override
//                    public void onFailure(OwtError error) {
//                        new Thread() {
//                            @Override
//                            public void run() {
//                                super.run();
//                                try {
//                                    Thread.sleep(1000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                                publish();
//                            }
//                        }.start();
//
//                    }
//                }));
//
//    }
//
//
//    boolean isClick = false;
//    long startTime;
//    private final View.OnTouchListener touchListener = new View.OnTouchListener() {
//        @Override
//        public boolean onTouch(View v, MotionEvent event) {
//            if (v.getId() == R.id.smallRenderer) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        isClick = false;
//                        startTime = System.currentTimeMillis();
//                        dX = v.getX() - event.getRawX();
//                        dY = v.getY() - event.getRawY();
//                        break;
//                    case MotionEvent.ACTION_MOVE:
//                        isClick = true;
//                        v.animate()
//                                .x(event.getRawX() + dX)
//                                .y(event.getRawY() + dY)
//                                .setDuration(0)
//                                .start();
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        long endTime = System.currentTimeMillis();
//                        isClick = (endTime - startTime) > 0.1 * 1000L;
////                        v.animate()
////                                .x(event.getRawX() + dX >= event.getRawY() + dY ? event.getRawX()
////                                        + dX : 0)
////                                .y(event.getRawX() + dX >= event.getRawY() + dY ? 0
////                                        : event.getRawY() + dY)
////                                .setDuration(10)
////                                .start();
//                        v.animate()
//                                .x(event.getRawX()
//                                        + dX)
//                                .y(event.getRawY() + dY)
//                                .setDuration(10)
//                                .start();
//                        break;
//                }
//            }
//            return isClick;
//        }
//    };
//
//    @Override
//    public void onServerDisconnected() {
//    }
//
//    @Override
//    public void onStreamAdded(RemoteStream remoteStream) {
//        stopTimer();
//        RTCAudioPlayer.getInstance().stopPlay();
//        this.remoteStream = remoteStream;
//        WKRTCApplication.getInstance().remoteStream = remoteStream;
//        runOnUiThread(() -> {
//            if (smallSurfaceView != null) {
//                remoteStream.attach(smallSurfaceView);
//            }
//            answerLayout.setVisibility(View.GONE);
//            connectTv.setVisibility(View.GONE);
//            if (callType == WKRTCCallType.video) {
//                nameTv.setVisibility(View.GONE);
//                avatarIv.setVisibility(View.GONE);
//                audioLayout.setVisibility(View.GONE);
//                videoLayout.setVisibility(View.VISIBLE);
//            } else {
//                videoLayout.setVisibility(View.GONE);
//                audioLayout.setVisibility(View.VISIBLE);
//                nameTv.setVisibility(View.VISIBLE);
//                avatarIv.setVisibility(View.VISIBLE);
//            }
//            WKRTCManager.getInstance().startTimer();
//        });
//        remoteStream.addObserver(new owt.base.RemoteStream.StreamObserver() {
//            @Override
//            public void onEnded() {
//                hangup();
//            }
//
//            @Override
//            public void onUpdated() {
//            }
//        });
//    }
//
//    @Override
//    public void onDataReceived(String peerId, String message) {
//    }
//
//    private void stopTimer() {
//        if (countDownTimer != null) {
//            countDownTimer.cancel();
//            countDownTimer = null;
//        }
//    }
//
//    private CountDownTimer countDownTimer = new CountDownTimer(1000 * 30, 1000) {
//        @Override
//        public void onTick(long millisUntilFinished) {
//
//        }
//
//        @Override
//        public void onFinish() {
//            if (remoteStream == null) {
//                // WKRTCManager.getInstance().getSaveMsgListener().onMissed(isCreate, toUID, callType);
//                WKRTCManager.getInstance().getSendMsgListener().sendCancel(toUID, callType);
//                hangup();
//            }
//        }
//    };
//
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK && remoteStream != null) {
//            showFloatingView(remoteStream, true);
//        }
//        return true;
////        return super.onKeyDown(keyCode, event);
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        WKRTCManager.getInstance().removeTimeListener("p2pCall");
//    }
//
//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        showFloatingView(callType == WKRTCCallType.audio ? null : remoteStream, true);
//    }
//
//    @Override
//    public void finish() {
//        super.finish();
//        overridePendingTransition(R.anim.top_silent, R.anim.top_out);
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (fullSurfaceView != null) {
//            fullSurfaceView.release();
//            fullSurfaceView = null;
//        }
//        if (smallSurfaceView != null) {
//            smallSurfaceView.release();
//            smallSurfaceView = null;
//        }
//    }
//
//    private void switchType(int callType) {
//        if (callType == WKRTCCallType.audio) {
//            fullSurfaceView.setVisibility(View.GONE);
//            smallSurfaceView.setVisibility(View.GONE);
//            avatarIv.setVisibility(View.VISIBLE);
//            nameTv.setVisibility(View.VISIBLE);
//            audioLayout.setVisibility(View.VISIBLE);
//            videoLayout.setVisibility(View.GONE);
//
//            //  audioManager.setMode(AudioManager.MODE_IN_CALL);
//            audioManager.setSpeakerphoneOn(true);
//            if (audioManager.isSpeakerphoneOn()) {
//                speakerIV.setImageResource(R.mipmap.ic_handfree_hover);
//            } else speakerIV.setImageResource(R.mipmap.ic_handfree);
//        } else {
//            fullSurfaceView.setVisibility(View.VISIBLE);
//            smallSurfaceView.setVisibility(View.VISIBLE);
//            avatarIv.setVisibility(View.GONE);
//            nameTv.setVisibility(View.GONE);
//            audioLayout.setVisibility(View.GONE);
//            videoLayout.setVisibility(View.VISIBLE);
//        }
//    }
//}
