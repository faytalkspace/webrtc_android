package com.xinbida.rtc.p2p;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.xinbida.rtc.R;
import com.xinbida.rtc.WKRTCApplication;
import com.xinbida.rtc.WKRTCCallType;
import com.xinbida.rtc.utils.CommonUtils;
import com.xinbida.rtc.utils.P2PDataProvider;
import com.xinbida.rtc.utils.RTCAudioPlayer;
import com.xinbida.rtc.utils.WKFloatingViewManager;
import com.xinbida.rtc.utils.WKRTCManager;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.HashMap;
import java.util.Objects;

public class CallActivity extends AppCompatActivity {
    private final int OVERLAY_PERMISSION_REQUEST_CODE = 1024;
    private AppCompatImageView avatarIv;
    private TextView nameTv, timeTv;
    private TextView connectTv;
    private SurfaceViewRenderer smallSurfaceView;
    private SurfaceViewRenderer fullSurfaceView;
    private float dX, dY;
    private View answerLayout, videoLayout, audioLayout;
    private AppCompatImageView answerIV, switchCameraIV, hangUpIV, switchAudioIV, switchVideoIV, muteIV, speakerIV;
    private boolean isCreate;

    private int callType;
    private String loginUID;
    private String toUID, toName;
    private AudioManager audioManager;
    private boolean isConnect = false;
    private long totalDuration;
    private boolean isRestart = false;
    private boolean isAttachLocal = true;
    private Vibrator vibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        isRestart = getIntent().getBooleanExtra("isRestart", false);
        isCreate = getIntent().getBooleanExtra("isCreate", false);
        if (isCreate && !isRestart) {
            overridePendingTransition(R.anim.top_in, R.anim.top_silent);
        }
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Window window = getWindow();
        setContentView(R.layout.act_p2p_call_layout);
        CommonUtils.setStatusBarColor(window, ContextCompat.getColor(this, R.color.color232323), 0);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        toName = getIntent().getStringExtra("toName");
        toUID = getIntent().getStringExtra("toUID");
        loginUID = getIntent().getStringExtra("loginUID");
        callType = getIntent().getIntExtra("callType", 1);
        initView();

    }

    private void initView() {
        answerLayout = findViewById(R.id.answerLayout);
        audioLayout = findViewById(R.id.audioLayout);
        videoLayout = findViewById(R.id.videoLayout);
        answerIV = findViewById(R.id.answerIV);
        speakerIV = findViewById(R.id.speakerIV);
        muteIV = findViewById(R.id.muteIV);
        switchVideoIV = findViewById(R.id.switchVideoIV);
        switchCameraIV = findViewById(R.id.switchCameraIV);
        switchAudioIV = findViewById(R.id.switchAudioIV);
        hangUpIV = findViewById(R.id.hangUpIV);
        connectTv = findViewById(R.id.connectTv);
        timeTv = findViewById(R.id.timeTv);
        nameTv = findViewById(R.id.nameTv);
        avatarIv = findViewById(R.id.avatarIv);

        smallSurfaceView = findViewById(R.id.smallRenderer);
        smallSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        smallSurfaceView.setEnableHardwareScaler(true);
        smallSurfaceView.setOnTouchListener(touchListener);
        smallSurfaceView.setZOrderMediaOverlay(true);
        smallSurfaceView.init(WKRTCApplication.getInstance().getRootEglBase().getEglBaseContext(), null);


        fullSurfaceView = findViewById(R.id.fullRenderer);
        fullSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        fullSurfaceView.setEnableHardwareScaler(true);
        fullSurfaceView.setZOrderMediaOverlay(false);
        fullSurfaceView.init(WKRTCApplication.getInstance().getRootEglBase().getEglBaseContext(), null);
        nameTv.setText(toName);
        if (WKRTCManager.getInstance().getImageLoader() != null) {
            WKRTCManager.getInstance().getImageLoader().onImageLoader(this, toUID, avatarIv);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewCompat.setTransitionName(hangUpIV, "hangup");
            ViewCompat.setTransitionName(nameTv, "nameTv");
            ViewCompat.setTransitionName(avatarIv, "avatarIv");
        }
        isConnect = getIntent().getBooleanExtra("is_connect", false);
        if (callType == WKRTCCallType.audio) {
            audioManager.setSpeakerphoneOn(true);//是否从扬声器播出
            //音频通话
            fullSurfaceView.setVisibility(View.GONE);
            smallSurfaceView.setVisibility(View.GONE);
            avatarIv.setVisibility(View.VISIBLE);
            nameTv.setVisibility(View.VISIBLE);
            if (!isRestart) {
                if (!isConnect) {
                    RTCAudioPlayer.getInstance().play(this, "lim_rtc_call.mp3", true);
                    if (vibrator.hasVibrator() && !isCreate) {
                        long[] pattern = {0, 500, 1000};
                        vibrator.vibrate(pattern, 0);
                    }
                } else {
                    WKRTCManager.getInstance().getSendMsgListener().sendSwitchAudio(loginUID, toUID);
                }
            }
        } else {
            //  audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(true);
            if (!isRestart) {
                if (!isConnect) {
                    RTCAudioPlayer.getInstance().play(this, "lim_rtc_call.mp3", true);
                }
            }
        }
        if (isCreate) {
            countDownTimer.start();
        } else {
            if (callType == WKRTCCallType.audio)
                answerLayout.setVisibility(View.VISIBLE);
        }

        if (isRestart) {
            if (P2PDataProvider.getInstance().localStream != null) {
                P2PDataProvider.getInstance().localStream.attach(fullSurfaceView);
            }
            if (P2PDataProvider.getInstance().remoteStream != null) {
                if (callType == WKRTCCallType.video){
                    P2PDataProvider.getInstance().remoteStream.attach(smallSurfaceView);
                }
                answerLayout.setVisibility(View.GONE);
                connectTv.setVisibility(View.GONE);
                if (callType == WKRTCCallType.video) {
                    nameTv.setVisibility(View.GONE);
                    avatarIv.setVisibility(View.GONE);
                    audioLayout.setVisibility(View.GONE);
                    videoLayout.setVisibility(View.VISIBLE);
                } else {
                    videoLayout.setVisibility(View.GONE);
                    audioLayout.setVisibility(View.VISIBLE);
                    nameTv.setVisibility(View.VISIBLE);
                    avatarIv.setVisibility(View.VISIBLE);
                }
            }
            initListener();
        } else {
            requestPermission();
        }
    }

    private void initListener() {
        answerIV.setOnClickListener(v -> {
            // 接听
            if (vibrator != null) {
                vibrator.cancel();
            }
            RTCAudioPlayer.getInstance().stopPlay();
            answerLayout.setVisibility(View.GONE);
            P2PDataProvider.getInstance().connect();
            WKRTCManager.getInstance().getSendMsgListener().sendAccept(loginUID, toUID, WKRTCCallType.audio);
        });
        switchCameraIV.setOnClickListener(v -> {
            P2PDataProvider.getInstance().switchCamera();
        });
        switchAudioIV.setOnClickListener(v -> {
            if (P2PDataProvider.getInstance().remoteStream == null || P2PDataProvider.getInstance().localStream == null)
                return;
            callType = WKRTCCallType.audio;
            P2PDataProvider.getInstance().callType = WKRTCCallType.audio;
            fullSurfaceView.setVisibility(View.GONE);
            smallSurfaceView.setVisibility(View.GONE);
            audioLayout.setVisibility(View.VISIBLE);
            videoLayout.setVisibility(View.GONE);
            nameTv.setVisibility(View.VISIBLE);
            avatarIv.setVisibility(View.VISIBLE);
            WKRTCManager.getInstance().getSendMsgListener().sendSwitchAudio(loginUID, toUID);
        });
        switchVideoIV.setOnClickListener(view -> {
            if (P2PDataProvider.getInstance().remoteStream == null || P2PDataProvider.getInstance().localStream == null)
                return;
            WKRTCManager.getInstance().getSendMsgListener().sendSwitchVideo(loginUID, toUID);
            Toast.makeText(this, getString(R.string.request_video_send), Toast.LENGTH_SHORT).show();
        });
        // 静音
        muteIV.setOnClickListener(v -> {
            if (P2PDataProvider.getInstance().remoteStream == null || P2PDataProvider.getInstance().localStream == null)
                return;
            HashMap<String, String> hashMap = P2PDataProvider.getInstance().localStream.getAttributes();
            if (hashMap == null) hashMap = new HashMap<>();
            String rtc_mute = "open";
            if (hashMap.containsKey("rtc_mute")) {
                rtc_mute = hashMap.get("rtc_mute");
            }

            if (!TextUtils.isEmpty(rtc_mute) && TextUtils.equals(rtc_mute, "open")) {
                P2PDataProvider.getInstance().localStream.disableAudio();
                muteIV.setImageResource(R.mipmap.ic_mute_hover);
                hashMap.put("rtc_mute", "close");
            } else {
                P2PDataProvider.getInstance().localStream.enableAudio();
                muteIV.setImageResource(R.mipmap.ic_mute);
                hashMap.put("rtc_mute", "open");
            }
            P2PDataProvider.getInstance().localStream.setAttributes(hashMap);
        });
        // 免提
        speakerIV.setOnClickListener(v -> {
            if (P2PDataProvider.getInstance().remoteStream == null || P2PDataProvider.getInstance().localStream == null)
                return;
            //    audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(!audioManager.isSpeakerphoneOn());
            if (audioManager.isSpeakerphoneOn()) {
                speakerIV.setImageResource(R.mipmap.ic_handfree_hover);
            } else speakerIV.setImageResource(R.mipmap.ic_handfree);
        });
        // 小窗口
        findViewById(R.id.minimizeIv).setOnClickListener(v -> {
            if (vibrator != null) {
                vibrator.cancel();
            }
            showFloatingView();
        });
        smallSurfaceView.setOnClickListener(v -> {
            if (P2PDataProvider.getInstance().remoteStream == null || P2PDataProvider.getInstance().localStream == null)
                return;
            if (isAttachLocal) {
                P2PDataProvider.getInstance().localStream.detach(fullSurfaceView);
                P2PDataProvider.getInstance().remoteStream.detach(smallSurfaceView);

                P2PDataProvider.getInstance().localStream.attach(smallSurfaceView);
                P2PDataProvider.getInstance().remoteStream.attach(fullSurfaceView);
            } else {
                P2PDataProvider.getInstance().localStream.detach(smallSurfaceView);
                P2PDataProvider.getInstance().remoteStream.detach(fullSurfaceView);

                P2PDataProvider.getInstance().localStream.attach(fullSurfaceView);
                P2PDataProvider.getInstance().remoteStream.attach(smallSurfaceView);
            }
            isAttachLocal = !isAttachLocal;
        });
        hangUpIV.setOnClickListener(v -> {
            activeHangup();
        });
        WKRTCManager.getInstance().addTimerListener("p2pCall", (time, timeText) -> {
            runOnUiThread(() -> {
                totalDuration = time;
                timeTv.setText(timeText);
            });
        });
        P2PDataProvider.getInstance().addP2PListener(new P2PDataProvider.IP2PListener() {
            @Override
            public void hangup() {

                CallActivity.this.hangup();
            }

            @Override
            public void onStreamAdded() {
                RTCAudioPlayer.getInstance().stopPlay();
                runOnUiThread(() -> {
                    if (smallSurfaceView != null && callType == WKRTCCallType.video) {
                        P2PDataProvider.getInstance().remoteStream.attach(smallSurfaceView);
                        switchType(WKRTCCallType.video);
                        Log.e("添加大哦原创","-->");
                    }

                    answerLayout.setVisibility(View.GONE);
                    connectTv.setVisibility(View.GONE);
                    if (callType == WKRTCCallType.video) {
                        nameTv.setVisibility(View.GONE);
                        avatarIv.setVisibility(View.GONE);
                        audioLayout.setVisibility(View.GONE);
                        videoLayout.setVisibility(View.VISIBLE);
                    } else {
                        videoLayout.setVisibility(View.GONE);
                        audioLayout.setVisibility(View.VISIBLE);
                        nameTv.setVisibility(View.VISIBLE);
                        avatarIv.setVisibility(View.VISIBLE);
                    }
                    WKRTCManager.getInstance().startTimer();
                });
            }

            @Override
            public void onShowDialog() {
                if (!CallActivity.this.isFinishing() && !CallActivity.this.isDestroyed()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(CallActivity.this);
                    builder.setMessage(getString(R.string.request_video));
                    builder.setPositiveButton(getString(R.string.agree), (dialog, which) -> {
                        dialog.dismiss();
                        runOnUiThread(() -> {
                            P2PDataProvider.getInstance().remoteStream.attach(smallSurfaceView);
                            P2PDataProvider.getInstance().localStream.enableVideo();
                            switchType(WKRTCCallType.video);
                        });
                        WKRTCManager.getInstance().getSendMsgListener().sendSwitchVideoRespond(loginUID, toUID, 1);
                    });
                    builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                        dialog.dismiss();
                        WKRTCManager.getInstance().getSendMsgListener().sendSwitchVideoRespond(loginUID, toUID, 0);
                    });
                    builder.show();
                }
            }

            @Override
            public void onSwitchVideoRespond(String uid, int status) {
                if (status == 0)
                    runOnUiThread(() -> Toast.makeText(CallActivity.this, getString(R.string.other_refuse_switch_video), Toast.LENGTH_SHORT).show());
                else {
                    runOnUiThread(() -> {
                        P2PDataProvider.getInstance().remoteStream.attach(smallSurfaceView);
                        CallActivity.this.switchType(WKRTCCallType.video);
                    });
                }
            }

            @Override
            public void switchType(int callType) {
                runOnUiThread(() -> CallActivity.this.switchType(callType));
            }


        });
    }


    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }


    private CountDownTimer countDownTimer = new CountDownTimer(1000 * 30, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            if (P2PDataProvider.getInstance().remoteStream == null) {
                WKRTCManager.getInstance().getSendMsgListener().sendCancel(toUID, callType);
                 hangup();
            }
        }
    };


    boolean isClick = false;
    long startTime;
    private final View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (v.getId() == R.id.smallRenderer) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isClick = false;
                        startTime = System.currentTimeMillis();
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        isClick = true;
                        v.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        break;
                    case MotionEvent.ACTION_UP:
                        long endTime = System.currentTimeMillis();
                        isClick = (endTime - startTime) > 0.1 * 1000L;
                        v.animate()
                                .x(event.getRawX()
                                        + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(10)
                                .start();
                        break;
                }
            }
            return isClick;
        }

    };

    private void activeHangup() {
        RTCAudioPlayer.getInstance().stopPlay();
        if (!isCreate) {
            if (P2PDataProvider.getInstance().localStream == null || P2PDataProvider.getInstance().remoteStream == null) {
                // 拒绝
                //  WKRTCManager.getInstance().getSaveMsgListener().onRefuse(isCreate, toUID, LiMRTCCallType.audio);
                WKRTCManager.getInstance().getSendMsgListener().sendRefuse(toUID, (byte) 1, callType);
                new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(() -> {
                    RTCAudioPlayer.getInstance().play(CallActivity.this, "lim_rtc_hangup.wav", false);
                    finish();
                }, 500);
            } else {
                WKRTCManager.getInstance().getSendMsgListener().sendHangUpMsg(toUID, (int) (totalDuration / 1000), callType, isCreate ? 1 : 0);
            }
        } else {
            if (P2PDataProvider.getInstance().remoteStream == null) {
                WKRTCManager.getInstance().getSendMsgListener().sendCancel(toUID, callType);
            } else {
                WKRTCManager.getInstance().getSendMsgListener().sendHangUpMsg(toUID, (int) (totalDuration / 1000), callType, isCreate ? 1 : 0);
            }
        }
        hangup();
    }

    private void hangup() {

        if (fullSurfaceView != null) {
            fullSurfaceView.release();
            fullSurfaceView = null;
        }
        if (smallSurfaceView != null) {
            smallSurfaceView.release();
            smallSurfaceView = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        P2PDataProvider.getInstance().hangup();
        audioManager.setSpeakerphoneOn(false);
        stopTimer();
        WKRTCManager.getInstance().stopTimer();
        finish();
    }

    private void requestPermission() {
        String[] permissions = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO};

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(CallActivity.this,
                    permission) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(CallActivity.this,
                        permissions,
                        100);
                // 发送取消消息
//                WKRTCManager.getInstance().getSendMsgListener().sendCancel(toUID, callType);
//                hangup();
                return;
            }
        }
        initListener();
        P2PDataProvider.getInstance().init(loginUID, toUID, toName, callType, isCreate, isConnect, fullSurfaceView);
        if (!isCreate) {
            runOnUiThread(() -> {
                answerIV.setVisibility(View.VISIBLE);
            });
            //  P2PDataProvider.getInstance().publish("UI去宣泄");
        }

    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 100
                && grantResults.length == 2
                && grantResults[0] == PERMISSION_GRANTED
                && grantResults[1] == PERMISSION_GRANTED) {
            initListener();
            P2PDataProvider.getInstance().init(loginUID, toUID, toName, callType, isCreate, isConnect, fullSurfaceView);

            if (!isCreate) {
                runOnUiThread(() -> {
                    answerIV.setVisibility(View.VISIBLE);
                });
                //  P2PDataProvider.getInstance().publish("ui 通过");
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.open_camera_microphone));
            builder.setPositiveButton(getString(R.string.rtc_go_to_setting), (dialog, which) -> {
                dialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + CallActivity.this.getPackageName()));
                startActivity(intent);
                activeHangup();
//                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            });
            builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                dialog.dismiss();
                activeHangup();
            });
            builder.show();

        }

    }

    private void showFloatingView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //判断是否拥有悬浮窗权限，无则跳转悬浮窗权限授权页面
            if (Settings.canDrawOverlays(this)) {
                show();
            } else {
                showDialog();
            }
        } else {
            show();
        }
    }


    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.open_floating_desc));
        builder.setPositiveButton(getString(R.string.open), (dialog, which) -> {
            dialog.dismiss();
            Intent intent = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + CallActivity.this.getPackageName()));
            } else {
                intent = new Intent(Settings.ACTION_APN_SETTINGS, Uri.parse("package:" + CallActivity.this.getPackageName()));
            }
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && P2PDataProvider.getInstance().remoteStream != null) {
            showFloatingView();
        }
        return true;
//        return super.onKeyDown(keyCode, event);
    }


    private void switchType(int callType) {
        P2PDataProvider.getInstance().callType = callType;
        CallActivity.this.callType = callType;
        if (callType == WKRTCCallType.audio) {
            fullSurfaceView.setVisibility(View.GONE);
            smallSurfaceView.setVisibility(View.GONE);
            avatarIv.setVisibility(View.VISIBLE);
            nameTv.setVisibility(View.VISIBLE);
            audioLayout.setVisibility(View.VISIBLE);
            videoLayout.setVisibility(View.GONE);

            //  audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(true);
            if (audioManager.isSpeakerphoneOn()) {
                speakerIV.setImageResource(R.mipmap.ic_handfree_hover);
            } else speakerIV.setImageResource(R.mipmap.ic_handfree);
        } else {
            fullSurfaceView.setVisibility(View.VISIBLE);
            smallSurfaceView.setVisibility(View.VISIBLE);
            avatarIv.setVisibility(View.GONE);
            nameTv.setVisibility(View.GONE);
            audioLayout.setVisibility(View.GONE);
            videoLayout.setVisibility(View.VISIBLE);
        }
    }

    private void show() {
//        if (isAttachLocal) {
//            P2PDataProvider.getInstance().localStream.detach(fullSurfaceView);
//            if (callType == WKRTCCallType.video) {
//                P2PDataProvider.getInstance().remoteStream.detach(smallSurfaceView);
//            }
//
////            P2PDataProvider.getInstance().localStream.attach(smallSurfaceView);
////            P2PDataProvider.getInstance().remoteStream.attach(fullSurfaceView);
//        } else {
//            P2PDataProvider.getInstance().localStream.detach(smallSurfaceView);
//            if (callType == WKRTCCallType.video) {
//                P2PDataProvider.getInstance().remoteStream.detach(fullSurfaceView);
//            }
////            P2PDataProvider.getInstance().localStream.attach(fullSurfaceView);
////            P2PDataProvider.getInstance().remoteStream.attach(smallSurfaceView);
//        }
        WKFloatingViewManager.getInstance().showFloatingView(true);
        finish();
    }


    @Override
    public void finish() {
        super.finish();
        WKRTCManager.getInstance().isCalling = false;
    }
}
