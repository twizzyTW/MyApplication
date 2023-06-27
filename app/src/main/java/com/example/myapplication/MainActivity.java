package com.example.myapplication;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button startStopButton;
    private Button setTargetButton;
    private Button historyButton;
    private TextView distanceTextView;
    private Chronometer chronometer;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private boolean isRunning = false;
    private long startTime;
    private long elapsedTime;

    private double totalDistance = 0.0;
    private double targetDistance = 5.0;
    private boolean isTargetAchieved = false;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startStopButton = findViewById(R.id.startStopButton);
        setTargetButton = findViewById(R.id.setTargetButton);
        historyButton = findViewById(R.id.historyButton);
        distanceTextView = findViewById(R.id.distanceTextView);
        chronometer = findViewById(R.id.chronometer);


        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    stopTimer();
                } else {
                    startTimer();
                }
            }
        });

        setTargetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSetTargetDialog();
            }
        });

        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHistoryActivity();
            }
        });

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateDistance(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        databaseHelper = new DatabaseHelper(this);
    }

    private void startTimer() {
        isRunning = true;
        startStopButton.setText("Stop");
        startTime = SystemClock.elapsedRealtime();
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
        totalDistance = 0.0;
        distanceTextView.setText("00 km");
    }

    private void stopTimer() {
        isRunning = false;
        startStopButton.setText("Start");
        elapsedTime = SystemClock.elapsedRealtime() - startTime;
        chronometer.stop();
        updateElapsedTime(elapsedTime);

        saveTrainingToDatabase();
    }

    private void updateElapsedTime(long time) {
        int seconds = (int) (time / 1000);
        int minutes = seconds / 60;
        seconds %= 60;
        String timeFormatted = String.format("%02d:%02d", minutes, seconds);
        chronometer.setText(timeFormatted);
    }

    private void updateDistance(Location location) {
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            if (totalDistance == 0) {
                totalDistance = 0;
            } else {
                Location prevLocation = new Location("");
                prevLocation.setLatitude(latitude);
                prevLocation.setLongitude(longitude);
                double distance = prevLocation.distanceTo(location);
                totalDistance += distance;
            }

            double distanceInKm = totalDistance / 1000;
            distanceTextView.setText(String.format("%.2f km", distanceInKm));

            if (!isTargetAchieved && totalDistance >= targetDistance) {
                isTargetAchieved = true;
                distanceTextView.setText("ЦЕЛЬ ДОСТИГНУТА!!!");
            }
        }
    }

    private void openSetTargetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Target Distance (km)");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(targetDistance));
        builder.setView(input);

        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String inputText = input.getText().toString();
                if (!inputText.isEmpty()) {
                    targetDistance = Double.parseDouble(inputText);
                    isTargetAchieved = false;
                    distanceTextView.setText(String.format("%.2f km", targetDistance));
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void saveTrainingToDatabase() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        String currentTime = String.valueOf(System.currentTimeMillis());
        String duration = chronometer.getText().toString();
        String distance = distanceTextView.getText().toString();

        String insertQuery = "INSERT INTO " + DatabaseHelper.TABLE_NAME +
                " (" + DatabaseHelper.COLUMN_TIME + ", " + DatabaseHelper.COLUMN_DURATION +
                ", " + DatabaseHelper.COLUMN_DISTANCE + ") VALUES ('" +
                currentTime + "', '" + duration + "', '" + distance + "')";

        db.execSQL(insertQuery);
    }

    private void openHistoryActivity() {
        List<String> trainingList = fetchTrainingListFromDatabase();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, trainingList);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Training History");
        builder.setAdapter(adapter, null);

        builder.setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private List<String> fetchTrainingListFromDatabase() {
        List<String> trainingList = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + DatabaseHelper.TABLE_NAME +
                " ORDER BY " + DatabaseHelper.COLUMN_TIME + " DESC LIMIT 30";
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                long timestamp = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME));
                String time = convertTimestampToDate(timestamp);
                String duration = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DURATION));
                String distance = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DISTANCE));

                String training = time + " | " + duration + " | " + distance;
                trainingList.add(training);
            } while (cursor.moveToNext());
        }

        cursor.close();

        return trainingList;
    }

    private String convertTimestampToDate(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    0, 0, locationListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }
}
