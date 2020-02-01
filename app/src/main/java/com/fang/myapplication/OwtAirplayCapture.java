package com.fang.myapplication;

import android.content.Context;

import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;

import java.io.IOException;

import owt.base.Stream;
import owt.base.VideoCapturer;

public class OwtAirplayCapture implements VideoCapturer {
    protected CapturerObserver capturerObserver;

    public OwtAirplayCapture()  {
    }

    @Override
    public int getWidth() {
        // ignored
        return 720;
    }

    @Override
    public int getHeight() {
        // ignored
        return 1280;
    }

    @Override
    public int getFps() {
        return 0;
    }


    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext, CapturerObserver capturerObserver) {
        this.capturerObserver = capturerObserver;
    }

    public void startCapture(int width, int height, int framerate) {

    }

    public void stopCapture() throws InterruptedException {

    }

    @Override
    public boolean isScreencast() { return false; }
    public void dispose() {

    }
    public void changeCaptureFormat(int width, int height, int framerate) {
    }

    @Override
    public Stream.StreamSourceInfo.VideoSourceInfo getVideoSource() {
        return Stream.StreamSourceInfo.VideoSourceInfo.ENCODED_FILE;
    }
}
