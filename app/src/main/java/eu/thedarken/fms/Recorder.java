package eu.thedarken.fms;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

public class Recorder extends Thread {
    private final String TAG = "FMS:Recorder";
    private int mBuffersize;
    private AudioTrack mAudioTrack;
    private AudioRecord mAudioRecord;
    private int mMulti = 0;
    private boolean mIsInitialising = true;
    private final RecorderCallback mCallback;

    public Recorder(RecorderCallback callback, int delay) {
        mCallback = callback;
        mMulti = delay;

        mBuffersize = AudioTrack.getMinBufferSize(44100,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mBuffersize
                        * (mMulti + 1));
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mBuffersize,
                AudioTrack.MODE_STREAM);
        mAudioTrack.setPlaybackRate(44100);
    }

    public void halt() {
        mAudioTrack.stop();
    }

    public boolean isRecording() {
        if (mAudioTrack != null && mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isInitialising() {
        return mIsInitialising;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        byte[] buffer = new byte[mBuffersize];
        try {
            mAudioRecord.startRecording();
        } catch (Exception e) {
            mCallback.onError();
            return;
        } finally {
            mIsInitialising = false;
        }

        try {
            Thread.sleep(mMulti * 50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            mAudioTrack.play();
        } catch (Exception e) {
            mCallback.onError();
            return;
        } finally {
            mIsInitialising = false;
        }


        mCallback.onRecorderStart();
        Log.i(TAG, "Started");
        while (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
            mAudioRecord.read(buffer, 0, mBuffersize);
            mAudioTrack.write(buffer, 0, buffer.length);
        }
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioTrack.release();
    }

    public interface RecorderCallback {

        public void onRecorderStart();

        public void onRecorderStop();

        public void onError();
    }
}
