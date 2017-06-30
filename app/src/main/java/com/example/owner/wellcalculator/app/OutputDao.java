package com.example.owner.wellcalculator.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;

//TODO: Put names of things into a configuration
//This class is used read/write an array of OutputData from/to SQLite DB
//Internal storage is used instead of external storage
public class OutputDao extends SQLiteOpenHelper{

    public static final String DATABASE_NAME = "Inputs.db";
    public static final String OUTPUT_TABLE_NAME = "outputs";
    public static final String OUTPUT_ID = "id";
    public static final String OUTPUT_TSOLVE = "tsolve";
    public static final String OUTPUT_VOLUMETRICFLOWRATE = "volumetricflowrate";
    public static final String OUTPUT_P = "p";
    public static final String OUTPUT_T = "t";
    public static final String OUTPUT_H = "h";
    public static final String OUTPUT_X = "x";

    public OutputDao(Context context)
    {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "create table if not exists "+ OUTPUT_TABLE_NAME +
                        " (" + OUTPUT_ID + " integer primary key, " +
                        OUTPUT_TSOLVE + " integer, " +
                        OUTPUT_VOLUMETRICFLOWRATE + " double, " +
                        OUTPUT_P + " double, " +
                        OUTPUT_T + " double, " +
                        OUTPUT_H + " double, " +
                        OUTPUT_X + " double)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DELETE FROM " + OUTPUT_TABLE_NAME);
        onCreate(db);
    }

    public void recreateInputDB(SQLiteDatabase db) {
        onUpgrade(db, 0, 0);
    }

    //Every time data is saved, the old data is deleted and new data is inserted
    public void writeOutputs(final OutputData[] outputData) {
        SQLiteDatabase db = this.getWritableDatabase();
        recreateInputDB(db);

        for (int i = 0; i < outputData.length; i++) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(OUTPUT_ID , i);
            contentValues.put(OUTPUT_TSOLVE , outputData[i].getTsolve());
            contentValues.put(OUTPUT_VOLUMETRICFLOWRATE , outputData[i].getVolumetricFlowRate());
            contentValues.put(OUTPUT_P , outputData[i].getP());
            contentValues.put(OUTPUT_T , outputData[i].getT());
            contentValues.put(OUTPUT_H , outputData[i].getH());
            contentValues.put(OUTPUT_X , outputData[i].getX());
            db.insert(OUTPUT_TABLE_NAME, null, contentValues);
        }
        db.close();
    }
    public OutputData[] readOutputs() {
        SQLiteDatabase db = this.getWritableDatabase();
        onCreate(db);

        String queryString = "select * from " + OUTPUT_TABLE_NAME;
        Cursor res =  db.rawQuery(queryString, null);
        int rowCount = res.getCount();
        if (rowCount == 0) return null;
        OutputData[] outputData = new OutputData[rowCount];
        res.moveToFirst();

        int count = 0;
        while(!res.isAfterLast()){
            int tsolve = Integer.parseInt(res.getString(res.getColumnIndex(OUTPUT_TSOLVE)));
            double volumetricflowrate = Double.parseDouble(res.getString(res.getColumnIndex(OUTPUT_VOLUMETRICFLOWRATE)));
            double p = Double.parseDouble(res.getString(res.getColumnIndex(OUTPUT_P)));
            double t = Double.parseDouble(res.getString(res.getColumnIndex(OUTPUT_T)));
            double h = Double.parseDouble(res.getString(res.getColumnIndex(OUTPUT_H)));
            double x = Double.parseDouble(res.getString(res.getColumnIndex(OUTPUT_X)));
            outputData[count] = new OutputData(tsolve, volumetricflowrate, p, t, h, x);
            res.moveToNext();
            count++;
        }

        res.close();
        db.close();
        return outputData;
    }
}
