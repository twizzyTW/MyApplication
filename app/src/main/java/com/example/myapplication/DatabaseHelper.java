package com.example.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "training.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "training";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_DISTANCE = "distance";

    private static final String CREATE_TABLE_QUERY =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_TIME + " TEXT, " +
                    COLUMN_DURATION + " TEXT, " +
                    COLUMN_DISTANCE + " TEXT)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_QUERY);

        String deleteQuery = "DELETE FROM " + TABLE_NAME + " WHERE " +
                COLUMN_TIME + " <= strftime('%s', 'now', '-30 days')";

        db.execSQL(deleteQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
