package eu.thedarken.fms;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class FMSActivity extends Activity implements Recorder.RecorderCallback {
    private final static String TAG = "FMS:Main";
    private Recorder mRecorder;
    private SharedPreferences mSettings;
    private SeekBar mDelayBar;
    private TextView mDelayDesc;
    private TextView mVersion;
    private Button mGoButton;
    private Button mSettingsButton;
    private BroadcastsHandler mUnpluggedreceiver;
    private boolean mHeadphonespluggedin = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mDelayBar = (SeekBar) findViewById(R.id.sb_delay);
        mDelayBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mSettings.edit().putInt("delay.value", progress).commit();
                }
            }
        });
        mDelayDesc = (TextView) findViewById(R.id.tv_delay);
        mVersion = (TextView) findViewById(R.id.tv_version);
        mGoButton = (Button) findViewById(R.id.bt_go);
        mGoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecorder != null && mRecorder.isInitialising()) {
                    Toast.makeText(FMSActivity.this, getString(R.string.aborting_not_possible), Toast.LENGTH_SHORT).show();
                } else {
                    if (mRecorder != null && mRecorder.isRecording()) {
                        stopReset();
                    } else {
                        if (mHeadphonespluggedin) {
                            start();
                        } else {
                            showDialog(0);
                        }
                    }
                }
            }
        });

        mSettingsButton = (Button) findViewById(R.id.bt_settings);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startPreferencesActivity = new Intent(FMSActivity.this, Settings.class);
                startActivity(startPreferencesActivity);
            }
        });

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        try {
            mVersion.setText(this.getPackageName() + " v" + getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName + "("
                    + getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode + ")");
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        mUnpluggedreceiver = new BroadcastsHandler();
        registerReceiver(mUnpluggedreceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mUnpluggedreceiver);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        if (!mSettings.getBoolean("general.background", true) && mRecorder != null && mRecorder.isRecording()) {
            stopReset();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mRecorder != null && mRecorder.isRecording()) {
            setUIRunning();
        }
        mDelayBar.setProgress(mSettings.getInt("delay.value", 0));
    }

    private void setUIRunning() {
        mGoButton.setText(R.string.stop);
        mDelayDesc.setText(getString(R.string.current_delay_x, (50 * mDelayBar.getProgress())));
        mDelayBar.setEnabled(false);
        Toast.makeText(this, getString(R.string.recording), Toast.LENGTH_SHORT).show();
    }

    public void setUIStopped() {
        mDelayDesc.setText(R.string.slider_info);
        mGoButton.setText(R.string.start);
        mDelayBar.setEnabled(true);
        Log.i(TAG, "Stopped.");
    }

    private void start() {
        Log.i(TAG, "Starting...");
        mRecorder = new Recorder(this, mSettings.getInt("delay.value", 0));
        mRecorder.start();
        setUIRunning();
    }

    private void stopReset() {
        mRecorder.halt();
        mDelayDesc.setText(getString(R.string.stopping));
        while (mRecorder.isRecording()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        setUIStopped();
    }

    @Override
    public void onRecorderStart() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setUIRunning();
            }
        });
    }

    @Override
    public void onRecorderStop() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setUIStopped();
            }
        });
    }

    @Override
    public void onError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setUIStopped();
                Toast.makeText(FMSActivity.this, getString(R.string.error_lower_delay), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public class BroadcastsHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG)) {
                if (intent.getIntExtra("state", 0) == 1) {
                    mHeadphonespluggedin = true;
                    Log.i(TAG, "headphones are plugged in");
                } else {
                    mHeadphonespluggedin = false;
                    Log.i(TAG, "no headphones plugged in");
                }
                if (intent.getIntExtra("state", 0) == 0 && mSettings.getBoolean("general.stoponunplug", true) && mRecorder != null && mRecorder.isRecording()) {
                    Log.i(TAG, "headset unplugged, stopping...");
                    stopReset();
                }
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 0:
                return new AlertDialog.Builder(this).setTitle("Warning").setCancelable(true)
                        .setMessage(getString(R.string.no_headphones_notice))
                        .setNegativeButton("Abort!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })

                        .setPositiveButton(getString(R.string.dont_care), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                start();
                            }
                        }).create();
        }
        return null;
    }
}