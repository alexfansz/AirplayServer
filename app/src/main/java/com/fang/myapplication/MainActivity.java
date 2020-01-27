package com.fang.myapplication;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import owt.base.LocalStream;

public class MainActivity extends Activity implements View.OnClickListener {

    public static String TAG = "MainActivity";

    private AirPlayServer mAirPlayServer;
    private RaopServer mRaopServer;
    private DNSNotify mDNSNotify;

    private SurfaceView mSurfaceView;
    private Button mBtnControl;
    private TextView mTxtDevice;
    private boolean mIsStart = false;

    private SurfaceViewRenderer smallRenderer;
    private LocalStream localStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSystemService(Context.NSD_SERVICE);
        mBtnControl = findViewById(R.id.btn_control);
        mTxtDevice = findViewById(R.id.txt_device);
        mBtnControl.setOnClickListener(this);
        mSurfaceView = findViewById(R.id.surface);
        mAirPlayServer = new AirPlayServer();
        mRaopServer = new RaopServer(mSurfaceView);
        mDNSNotify = new DNSNotify();

        smallRenderer = findViewById(R.id.small_renderer);
        smallRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        smallRenderer.setEnableHardwareScaler(true);
        smallRenderer.setZOrderMediaOverlay(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_control: {
                if (!mIsStart) {
                    startServer();
                    mTxtDevice.setText("设备名称:" + mDNSNotify.getDeviceName());
                } else {
                    stopServer();
                    mTxtDevice.setText("未启动");
                }
                mIsStart = !mIsStart;
                mBtnControl.setText(mIsStart ? "结束" : "开始");
                break;
            }
        }
    }

    private void startServer() {
        mDNSNotify.changeDeviceName();
        mAirPlayServer.startServer();
        int airplayPort = mAirPlayServer.getPort();
        if (airplayPort == 0) {
            Toast.makeText(this.getApplicationContext(), "启动airplay服务失败", Toast.LENGTH_SHORT).show();
        } else {
            mDNSNotify.registerAirplay(airplayPort);
        }
        mRaopServer.startServer();
        int raopPort = mRaopServer.getPort();
        if (raopPort == 0) {
            Toast.makeText(this.getApplicationContext(), "启动raop服务失败", Toast.LENGTH_SHORT).show();
        } else {
            mDNSNotify.registerRaop(raopPort);
        }
        Log.d(TAG, "airplayPort = " + airplayPort + ", raopPort = " + raopPort);
    }

    private void stopServer() {
        mDNSNotify.stop();
        mAirPlayServer.stopServer();
        mRaopServer.stopServer();
    }

}


