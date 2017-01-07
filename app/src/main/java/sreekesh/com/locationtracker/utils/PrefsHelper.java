package sreekesh.com.locationtracker.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by sree on 6/1/17.
 */

public class PrefsHelper {

    //Preference Variables
    public static final String MAP_LOCATION_TRACK_STATUS = "location_track_status";
    public static final String PREF_NAME = "LocationPreferenceFile";

    //Constants
    public static final String GPS_ENABLED = "gps_enabled";
    public static final String GPS_NOT_ENABLED = "gps_not_enabled";
    public static final String PLUGGED_USB = "PLUGGED_USB";
    public static final String PLUGGED_AC = "PLUGGED_AC";
    public static final String PLUGGED_WIRELESS = "PLUGGED_WIRELESS";
    public static final String CHARGING = "CHARGING";
    public static final String NONE = "NONE";
    public static final String UNAVAILABLE = "UNAVAILABLE";

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context context;

    int PRIVATE_MODE = 0;

    public PrefsHelper(Context context){
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public Boolean getLocationTrackingStatus(){
        return pref.getBoolean(MAP_LOCATION_TRACK_STATUS, false);
    }

    public void setLocationTrackingStatus(Boolean seqId){
        editor.putBoolean(MAP_LOCATION_TRACK_STATUS, seqId);
        editor.apply();
    }
}
