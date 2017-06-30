package com.example.owner.wellcalculator.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;

//This class is used read/write an array of InputParamsData (advanced) from/to SQLite DB
//This class is the exactly the same as InputParamsDao but the database is different
//Internal storage is used instead of external storage

//TODO: Put names of things into a configuration
public class InputParamsAdvDao extends SQLiteOpenHelper{

    public static final String DATABASE_NAME = "Inputs.db";
    public static final String INPUT_TABLE_NAME = "inputs_adv";
    public static final String INPUT_NAME = "name";
    public static final String INPUT_MAXVALUE = "maxvalue";
    public static final String INPUT_VALUE = "value";
    public static final String INPUT_MINVALUE = "minvalue";
    public static final String INPUT_UNIT = "unit";

    public InputParamsAdvDao(Context context)
    {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "create table if not exists "+ INPUT_TABLE_NAME +
                        " (" + INPUT_NAME + " text primary key, " +
                        INPUT_MINVALUE + " text, " +
                        INPUT_VALUE + " text, " +
                        INPUT_MAXVALUE + " text, " +
                        INPUT_UNIT + " text)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DELETE FROM " + INPUT_TABLE_NAME);
        onCreate(db);
    }

    public void recreateInputDB(SQLiteDatabase db) {
        onUpgrade(db, 0, 0);
    }

    //Every time data is saved, the old data is deleted and new data is inserted
    public void writeInputs(final InputParamsData[] inputData) {
        SQLiteDatabase db = this.getWritableDatabase();
        recreateInputDB(db);

        for (int i = 0; i < inputData.length; i++) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(INPUT_NAME , inputData[i].getFieldName());
            contentValues.put(INPUT_MINVALUE, inputData[i].getFieldMinValue());
            contentValues.put(INPUT_VALUE, inputData[i].getFieldValue());
            contentValues.put(INPUT_MAXVALUE, inputData[i].getFieldMaxValue());
            contentValues.put(INPUT_UNIT, inputData[i].getUnit());
            db.insert(INPUT_TABLE_NAME, null, contentValues);
        }
        db.close();
    }
    public InputParamsData[] readInputs() {
        SQLiteDatabase db = this.getWritableDatabase();
        onCreate(db);

        String queryString = "select * from " + INPUT_TABLE_NAME;
        Cursor res =  db.rawQuery(queryString, null);
        int rowCount = res.getCount();
        if (rowCount == 0) return null;
        InputParamsData[] inputData = new InputParamsData[rowCount];
        res.moveToFirst();

        int count = 0;
        while(!res.isAfterLast()){
            String name = res.getString(res.getColumnIndex(INPUT_NAME));
            float minvalue = Float.parseFloat(res.getString(res.getColumnIndex(INPUT_MINVALUE)));
            float value = Float.parseFloat(res.getString(res.getColumnIndex(INPUT_VALUE)));
            float maxvalue = Float.parseFloat(res.getString(res.getColumnIndex(INPUT_MAXVALUE)));
            String unit = res.getString(res.getColumnIndex(INPUT_UNIT));
            inputData[count] = new InputParamsData(name, minvalue, value, maxvalue, unit, true);
            res.moveToNext();
            count++;
        }

        res.close();
        db.close();
        return inputData;
    }
}
