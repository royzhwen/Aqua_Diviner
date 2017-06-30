package com.example.owner.wellcalculator.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//This class is used read/write a list of InputCalendarData from/to SQLite DB
//Internal storage is used instead of external storage
public class InputCalendarDao extends SQLiteOpenHelper{

    public static final String DATABASE_NAME = "Inputs.db";
    public static final String INPUT_TABLE_NAME = "calendar";
    public static final String DATERANGE_STARTDAY = "startDay";
    public static final String DATERANGE_ENDDAY = "endDay";
    public static final String DATERANGE_PRESSURE = "pressure";
    public static final String DATERANGE_FLOWRATE = "flowRate";

    public InputCalendarDao(Context context)
    {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "create table if not exists "+ INPUT_TABLE_NAME + " " +
                        "(" + DATERANGE_STARTDAY + " integer, " +
                        DATERANGE_ENDDAY + " integer, " +
                        DATERANGE_PRESSURE + " float, " +
                        DATERANGE_FLOWRATE + " float)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + INPUT_TABLE_NAME);
        onCreate(db);
    }

    public void recreateInputDB(SQLiteDatabase db) {
        onUpgrade(db, 0, 0);
    }

    //Every time data is saved, the old data is deleted and new data is inserted
    public void writeInputs(final List<InputCalendarData> inputData) {
        SQLiteDatabase db = this.getWritableDatabase();
        recreateInputDB(db);

        for (InputCalendarData daterange: inputData) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DATERANGE_STARTDAY , daterange.getStartDay());
            contentValues.put(DATERANGE_ENDDAY, daterange.getEndDay());
            contentValues.put(DATERANGE_PRESSURE, daterange.getPressure());
            contentValues.put(DATERANGE_FLOWRATE , daterange.getFlowRate());
            db.insert(INPUT_TABLE_NAME, null, contentValues);
        }
        db.close();
    }
    public List<InputCalendarData> readInputs() {
        SQLiteDatabase db = this.getWritableDatabase();
        onCreate(db);

        String queryString = "select * from " + INPUT_TABLE_NAME + " order by " + DATERANGE_STARTDAY ;
        Cursor res =  db.rawQuery(queryString, null);
        int rowCount = res.getCount();
        if (rowCount == 0) return null;
        List<InputCalendarData> calendarDataList = new ArrayList<InputCalendarData>();
        res.moveToFirst();
        while(!res.isAfterLast()){
            int startDay = Integer.parseInt(res.getString(res.getColumnIndex(DATERANGE_STARTDAY)));
            int endDay = Integer.parseInt(res.getString(res.getColumnIndex(DATERANGE_ENDDAY)));
            float pressure = Float.parseFloat(res.getString(res.getColumnIndex(DATERANGE_PRESSURE)));
            float flowRate = Float.parseFloat(res.getString(res.getColumnIndex(DATERANGE_FLOWRATE)));
            InputCalendarData calendarData = new InputCalendarData(startDay, endDay, pressure, flowRate);
            calendarDataList.add(calendarData);
            res.moveToNext();
        }

        res.close();
        db.close();
        return calendarDataList;
    }
}
