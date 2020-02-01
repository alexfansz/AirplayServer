package com.fang.myapplication.player;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.fang.myapplication.model.NALPacket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VideoPlayer extends Thread {

    private static final String TAG = "VideoPlayer";
    private static final boolean VERBOSE = false; // lots of logging

    private String mMimeType = "video/avc";
    private int mVideoWidth  = 720;
    private int mVideoHeight = 1280;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private MediaCodec mDecoder = null;
    private Surface mSurface = null;
    private boolean mIsEnd = false;
    private List<NALPacket> mListBuffer = Collections.synchronizedList(new ArrayList<NALPacket>());

    public VideoPlayer(Surface surface) {
        mSurface = surface;

        //initDecoder();
    }

    public void initDecoder() {
        try {
            MediaFormat format = MediaFormat.createVideoFormat(mMimeType, mVideoWidth, mVideoHeight);
            /*mDecoder = MediaCodec.createDecoderByType(mMimeType);
            mDecoder.configure(format, mSurface, null, 0);
            mDecoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            mDecoder.start();*/

            mDecoder = createVideoDecoder(format,mSurface);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addPacker(NALPacket nalPacket) {
        mListBuffer.add(nalPacket);
    }
    /*public void addPacker(NALPacket nalPacket) {
        doDecode(nalPacket);
    }*/
    
    @Override
    public void run() {
        super.run();
        initDecoder();
        /*
        while (!mIsEnd) {
            if (mListBuffer.size() == 0) {
                try {
                    sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            doDecode(mListBuffer.remove(0));
        }*/

        while (!mIsEnd) {

                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }

    private void doDecode(NALPacket nalPacket) {
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] decoderInputBuffers = mDecoder.getInputBuffers();
        int inputBufIndex = -10000;
        try {
            inputBufIndex = mDecoder.dequeueInputBuffer(TIMEOUT_USEC);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (inputBufIndex >= 0) {
            ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
            inputBuf.clear();
            inputBuf.put(nalPacket.nalData);
            mDecoder.queueInputBuffer(inputBufIndex, 0, nalPacket.nalData.length, nalPacket.pts, 0);
        } else {
            Log.d(TAG, "dequeueInputBuffer failed");
        }

        int outputBufferIndex = -10000;
        try {
            outputBufferIndex = mDecoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (outputBufferIndex >= 0) {
            mDecoder.releaseOutputBuffer(outputBufferIndex, true);
            /*try{
                Thread.sleep(50);
            }  catch (InterruptedException ie){
                ie.printStackTrace();
            }*/
        } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            /*try{
                Thread.sleep(10);
            }  catch (InterruptedException ie){
                ie.printStackTrace();
            }*/
        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            // not important for us, since we're using Surface

        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

        } else {

        }

        /*while (outputBufferIndex>=0) {
            mDecoder.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mDecoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        }*/
    }


    private static String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }
    static class CallbackHandler extends Handler {
        CallbackHandler(Looper l) {
            super(l);
        }
        private MediaCodec mCodec;
        private boolean mEncoder;
        private MediaCodec.Callback mCallback;
        private String mMime;
        private boolean mSetDone;
        @Override
        public void handleMessage(Message msg) {
            try {
                mCodec = mEncoder ? MediaCodec.createEncoderByType(mMime) : MediaCodec.createDecoderByType(mMime);
            } catch (IOException ioe) {
            }
            mCodec.setCallback(mCallback);
            synchronized (this) {
                mSetDone = true;
                notifyAll();
            }
        }
        void create(boolean encoder, String mime, MediaCodec.Callback callback) {
            mEncoder = encoder;
            mMime = mime;
            mCallback = callback;
            mSetDone = false;
            sendEmptyMessage(0);
            synchronized (this) {
                while (!mSetDone) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                    }
                }
            }
        }
        MediaCodec getCodec() {
            return mCodec;
        }
    }
    private HandlerThread mVideoDecoderHandlerThread;
    private CallbackHandler mVideoDecoderHandler;
    private MediaFormat mDecoderOutputVideoFormat = null;
    /**
     * Creates a decoder for the given format, which outputs to the given surface.
     *
     * @param inputFormat the format of the stream to decode
     * @param surface into which to decode the frames
     */
    private MediaCodec createVideoDecoder(MediaFormat inputFormat, Surface surface) throws IOException {
        mVideoDecoderHandlerThread = new HandlerThread("DecoderThread");
        mVideoDecoderHandlerThread.start();
        mVideoDecoderHandler = new CallbackHandler(mVideoDecoderHandlerThread.getLooper());
        MediaCodec.Callback callback = new MediaCodec.Callback() {
            public void onError(MediaCodec codec, MediaCodec.CodecException exception) {
            }
            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                mDecoderOutputVideoFormat = codec.getOutputFormat();
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: output format changed: "
                            + mDecoderOutputVideoFormat);
                }
            }
            public void onInputBufferAvailable(MediaCodec codec, int index) {
                // Extract video from file and feed to decoder.
                // We feed packets regardless of whether the muxer is set up or not.
                // If the muxer isn't set up yet, the encoder output will be queued up,
                // finally blocking the decoder as well.

                /* it is must,,,not good fix me!!!!*/
                while (mListBuffer.size() == 0 && !mIsEnd) {
                    try {
                        sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }/**/

                if (mListBuffer.size() > 0){
                    //ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                    //inputBuf.clear();
                    //inputBuf.put(nalPacket.nalData);

                    ByteBuffer decoderInputBuffer = codec.getInputBuffer(index);

                    int size = mListBuffer.get(0).nalData.length;
                    long presentationTimeUs = mListBuffer.get(0).pts;
                    decoderInputBuffer.put(mListBuffer.remove(0).nalData);

                    codec.queueInputBuffer(index, 0, size, presentationTimeUs, 0);
                }


                /*
                if (mVideoExtractorDone) {
                    if (VERBOSE) Log.d(TAG, "video extractor: EOS");
                    codec.queueInputBuffer(
                            index,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }*/
            }
            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: returned output buffer: " + index);
                    Log.d(TAG, "video decoder: returned buffer of size " + info.size);
                }
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (VERBOSE) Log.d(TAG, "video decoder: codec config buffer");
                    codec.releaseOutputBuffer(index, false);
                    return;
                }
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: returned buffer for time "
                            + info.presentationTimeUs);
                }
                boolean render = info.size != 0;
                codec.releaseOutputBuffer(index, render);

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) Log.d(TAG, "video decoder: EOS");

                    mIsEnd = true;
                }
            }
        };
        // Create the decoder on a different thread, in order to have the callbacks there.
        // This makes sure that the blocking waiting and rendering in onOutputBufferAvailable
        // won't block other callbacks (e.g. blocking encoder output callbacks), which
        // would otherwise lead to the transcoding pipeline to lock up.

        // Since API 23, we could just do setCallback(callback, mVideoDecoderHandler) instead
        // of using a custom Handler and passing a message to create the MediaCodec there.

        // When the callbacks are received on a different thread, the updating of the variables
        // that are used for state logging (mVideoExtractedFrameCount, mVideoDecodedFrameCount,
        // mVideoExtractorDone and mVideoDecoderDone) should ideally be synchronized properly
        // against accesses from other threads, but that is left out for brevity since it's
        // not essential to the actual transcoding.
        mVideoDecoderHandler.create(false, getMimeTypeFor(inputFormat), callback);
        MediaCodec decoder = mVideoDecoderHandler.getCodec();
        decoder.configure(inputFormat, surface, null, 0);
        decoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        decoder.start();
        return decoder;
    }
}
