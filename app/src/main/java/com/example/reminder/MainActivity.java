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
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TimePicker;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener {
    private final int RECORD_CODE = 123;

    private OneTimeWorkRequest request;
    private MediaRecorder mediaRecorder;
    private static final String fileName = "recoded.zip";
    private static final String file
            = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton recordAudio = findViewById(R.id.record);
        ImageButton stopRecording = findViewById(R.id.stop_recording);
        ImageView timePicker = findViewById(R.id.open_time_picker);
        Button setReminder = findViewById(R.id.set_reminder);

        requestPermissions();

        stopRecording.setOnClickListener(v -> {
            requestPermissions();
            stopRecording();
        });
        stopRecording.setClickable(false);

        recordAudio.setOnClickListener(v -> {
            stopRecording.setClickable(true);
            requestPermissions();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);

            mediaRecorder.setOutputFile(file);

            record();
        });

        timePicker.setOnClickListener(v -> {
            DialogFragment timePicker1 = new TimePickerFragment();
            timePicker1.show(getSupportFragmentManager(), "time picker");
        });

        setReminder.setOnClickListener(v -> WorkManager.getInstance().enqueue(request));

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
                .setInitialDelay(deferredMin, TimeUnit.MINUTES)
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
        mediaRecorder.release();
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
                    .setPositiveButton("ok", (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO
                                    , Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    , Manifest.permission.READ_EXTERNAL_STORAGE}, RECORD_CODE))
                    .setNegativeButton("cancel", (dialog, which) -> dialog.dismiss())
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO
                            , Manifest.permission.WRITE_EXTERNAL_STORAGE
                            , Manifest.permission.READ_EXTERNAL_STORAGE}, RECORD_CODE);
        }
    }

    public static class MyWorker extends Worker {

        public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
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
                    .setSmallIcon(R.drawable.ic_record_voice);

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