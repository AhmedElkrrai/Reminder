package com.example.reminder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TimePicker;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener {
    private final int RECORD_CODE = 123;

    private ImageView recordAudio, stopRecording, timePicker;
    private Button setReminder;
    private OneTimeWorkRequest request;
    private MediaRecorder mediaRecorder;
    private static final String fileName = "recoded.zip";
    private static String file
            = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + fileName;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordAudio = findViewById(R.id.record);
        stopRecording = findViewById(R.id.stop_recording);
        timePicker = findViewById(R.id.open_time_picker);
        setReminder = findViewById(R.id.set_reminder);

        requestPermissions();

        stopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions();
                Log.i(TAG, "onClick: EKR st 1 ");
                stopRecording();
                Log.i(TAG, "onClick: EKR st 2");
            }
        });
        stopRecording.setClickable(false);

        recordAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording.setClickable(true);
                Log.i(TAG, "onClick: EKR re 1");
                requestPermissions();
                Log.i(TAG, "onClick: EKR re 2");

                mediaRecorder = new MediaRecorder();
                Log.i(TAG, "onClick: EKR re 3");
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                Log.i(TAG, "onClick: EKR re 4");
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                Log.i(TAG, "onClick: EKR re 5");
                mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                Log.i(TAG, "onClick: EKR re 6");

                mediaRecorder.setOutputFile(file);
                Log.i(TAG, "onClick: EKR re 7");

                record();
            }
        });

        timePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment timePicker = new TimePickerFragment();
                timePicker.show(getSupportFragmentManager(), "time picker");
            }
        });

        setReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WorkManager.getInstance().enqueue(request);
            }
        });

    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        String now = java.time.LocalTime.now().toString();
        String[] parts = now.split(":");
        int nowHour = Integer.parseInt(parts[0]);
        int nowMin = Integer.parseInt(parts[1]);

        String deferredTime = getDeferredTime(hourOfDay, minute, nowHour, nowMin);
        String[] defParts = deferredTime.split(":");
        int deferredHour = Integer.parseInt(defParts[0]);
        int deferredMin = Integer.parseInt(defParts[1]);

        request = new OneTimeWorkRequest.Builder(MyWorker.class)
                .setInitialDelay(deferredHour, TimeUnit.HOURS)
                .setInitialDelay(deferredMin, TimeUnit.SECONDS)
                .build();
    }

    private void record() {
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        mediaRecorder.stop();
        Log.i(TAG, "onClick: EKR stH 1");
        mediaRecorder.release();
        Log.i(TAG, "onClick: EKR stH 2");
    }

    private String getDeferredTime(int pickedHour, int pickedMin, int currentHour, int currentMin) {
        int deferredHour, deferredMin;
        if (pickedHour >= currentHour)
            deferredHour = pickedHour - currentHour;
        else deferredHour = 12 - (currentHour - pickedHour);

        if (pickedMin >= currentMin)
            deferredMin = pickedMin - currentMin;
        else {
            deferredMin = 60 - (currentMin - pickedMin);
            --deferredHour;
        }

        return deferredHour + ":" + deferredMin;
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                + ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                + ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsUTL();
        }
    }

    private void requestPermissionsUTL() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.RECORD_AUDIO
                                            , Manifest.permission.WRITE_EXTERNAL_STORAGE
                                            , Manifest.permission.READ_EXTERNAL_STORAGE}, RECORD_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO
                            , Manifest.permission.WRITE_EXTERNAL_STORAGE
                            , Manifest.permission.READ_EXTERNAL_STORAGE}, RECORD_CODE);
        }
    }

    public static class MyWorker extends Worker {
        private final Context mContext;

        public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
            this.mContext = context;
        }

        @NonNull
        @Override
        public Result doWork() {
            displayNotification();
            playAudio();
            return Result.success();
        }

        private void displayNotification() {

            NotificationManager manager =
                    (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("note", "note", NotificationManager.IMPORTANCE_DEFAULT);
                manager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "note")
                    .setContentTitle("Reminder")
                    .setSmallIcon(R.mipmap.ic_launcher);

            manager.notify(1, builder.build());

        }

        private void playAudio() {
            MediaPlayer mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(MainActivity.file);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}