package sreekesh.com.locationtracker.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import sreekesh.com.locationtracker.model.Contract;

/**
 * Created by sree on 7/1/17.
 */

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "location_data.db";
    private static final int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        final String SQL_CREATE_LOCATION_TABLE = "CREATE TABLE " + Contract.LocationDataEntry.TABLE_NAME + " (" +
                Contract.LocationDataEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                Contract.LocationDataEntry.COLUMN_LOCATION_DATA_TIMESTAMP + " INTEGER UNIQUE, " +
                Contract.LocationDataEntry.COLUMN_LOCATION_DATA_LATITUDE + " TEXT NOT NULL," +
                Contract.LocationDataEntry.COLUMN_LOCATION_DATA_LONGITUDE + " TEXT NOT NULL," +
                Contract.LocationDataEntry.COLUMN_LOCATION_DATA_PROVIDER + " TEXT, " +
                Contract.LocationDataEntry.COLUMN_LOCATION_DATA_BATTERY_STATUS_REMAINING + " TEXT," +
                Contract.LocationDataEntry.COLUMN_LOCATION_DATA_CHARGING_STATUS + " TEXT," +
                Contract.LocationDataEntry.COLUMN_LOCATION_DATA_ACCURACY + " TEXT," +
                Contract.LocationDataEntry.COLUMN_LOCATION_DATA_SPEED + " TEXT," +
                Contract.LocationDataEntry.COLUMN_LOCATION_DATA_GPS_ENABLED + " TEXT," +
                Contract.LocationDataEntry.COLUMN_LOCATION_DATA_BATTERY_STATUS_TIME + " TEXT," +
                "UNIQUE (" + Contract.LocationDataEntry.COLUMN_LOCATION_DATA_TIMESTAMP +") ON CONFLICT REPLACE"+
                " );";
        sqLiteDatabase.execSQL(SQL_CREATE_LOCATION_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Contract.LocationDataEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
