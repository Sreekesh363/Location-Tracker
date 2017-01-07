package sreekesh.com.locationtracker.model;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by sree on 7/1/17.
 */

public class Contract {
    public static final String CONTENT_AUTHORITY = "sreekesh.com.locationtracker";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://"+CONTENT_AUTHORITY);
    public static final String PATH_LOCATION_DATA = "location_data";

    public static final class LocationDataEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION_DATA).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION_DATA;

        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION_DATA;

        public static final String TABLE_NAME = "location_data";
        public static final String COLUMN_LOCATION_DATA_TIMESTAMP = "timestamp";
        public static final String COLUMN_LOCATION_DATA_LATITUDE = "latitude";
        public static final String COLUMN_LOCATION_DATA_LONGITUDE = "longitude";
        public static final String COLUMN_LOCATION_DATA_PROVIDER = "provider";
        public static final String COLUMN_LOCATION_DATA_ACCURACY = "accuracy";
        public static final String COLUMN_LOCATION_DATA_SPEED = "speed";
        public static final String COLUMN_LOCATION_DATA_GPS_ENABLED = "gps_enabled";
        public static final String COLUMN_LOCATION_DATA_BATTERY_STATUS_REMAINING = "battery_status_remaining";
        public static final String COLUMN_LOCATION_DATA_CHARGING_STATUS = "charging_status";
        public static final String COLUMN_LOCATION_DATA_BATTERY_STATUS_TIME = "battery_status_time";

        public static final Uri buildMapDataUri(long _id){
            return ContentUris.withAppendedId(CONTENT_URI, _id);
        }
    }
}
