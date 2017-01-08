package sreekesh.com.locationtracker.service;

import android.Manifest;
import android.app.Dialog;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import sreekesh.com.locationtracker.ResolverActivity;
import sreekesh.com.locationtracker.model.Contract;
import sreekesh.com.locationtracker.utils.PrefsHelper;
import sreekesh.com.locationtracker.utils.VolleyNetworkUtils;

import static sreekesh.com.locationtracker.ResolverActivity.CONN_STATUS_KEY;

public class LocationSyncService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String[] LOCATION_DATA_PROJECTION = {
            Contract.LocationDataEntry._ID,
            Contract.LocationDataEntry.COLUMN_LOCATION_DATA_TIMESTAMP,
            Contract.LocationDataEntry.COLUMN_LOCATION_DATA_LATITUDE,
            Contract.LocationDataEntry.COLUMN_LOCATION_DATA_LONGITUDE,
            Contract.LocationDataEntry.COLUMN_LOCATION_DATA_PROVIDER,
            Contract.LocationDataEntry.COLUMN_LOCATION_DATA_SPEED,
            Contract.LocationDataEntry.COLUMN_LOCATION_DATA_ACCURACY,
            Contract.LocationDataEntry.COLUMN_LOCATION_DATA_GPS_ENABLED,
            Contract.LocationDataEntry.COLUMN_LOCATION_DATA_BATTERY_STATUS_REMAINING,
            Contract.LocationDataEntry.COLUMN_LOCATION_DATA_CHARGING_STATUS,
            Contract.LocationDataEntry.COLUMN_LOCATION_DATA_BATTERY_STATUS_TIME,
    };
    public static final int COL_ID = 0;
    public static final int COL_LOCATION_TIMESTAMP = 1;
    public static final int COL_LOCATION_LATITUDE = 2;
    public static final int COL_LOCATION_LONGITUDE = 3;
    public static final int COL_LOCATION_PROVIDER = 4;
    public static final int COL_LOCATION_SPEED = 5;
    public static final int COL_LOCATION_ACCURACY = 6;
    public static final int COL_LOCATION_GPS_ENABLED = 7;
    public static final int COL_LOCATION_BATTERY_STATUS_REMAINING_TIME = 8;
    public static final int COL_LOCATION_CHARGING_STATUS = 9;
    public static final int COL_LOCATION_BATTERY_STATUS_TIME = 10;

    private static final String TAG = LocationSyncService.class.getSimpleName();
    private static final String SUBMIT_LOCATION = "submit_location";
    SharedPreferences preferences;

    private static final int REQUEST_CHECK_SETTINGS = 1;
    private static final int REQUEST_RESOLVE_ERROR = 2;
    private static final String DIALOG_ERROR = "dialog_error_play_services";
    private static final String DIALOG_TAG = "dialog_tag_error_play_services";

    GoogleApiClient mGoogleApiClient;
    private static final int TWO_MINUTES = 3000;
    private static final float MIN_BOUND_METERS = 10;
    boolean firstServerUpdate = true;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private boolean mResolvingError = false;

    public LocationSyncService() {
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Log.e(TAG,"Preference Change Got");
        if (s.equals(PrefsHelper.MAP_LOCATION_TRACK_STATUS)) {
            checkLocationTrackState(s);
        }
    }

    public void checkLocationTrackState(String s) {
        boolean a = preferences.getBoolean(s, false);
        mLocationRequest = new LocationRequest();
        if (a) {
            Log.e(TAG, "tracking changed to true");
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(TWO_MINUTES);
            mLocationRequest.setFastestInterval(1000);
            startLocationUpdates();
        } else {
            Log.e(TAG, "tracking changed to false");
            stopLocationUpdates();
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        if (preferences.getBoolean(PrefsHelper.MAP_LOCATION_TRACK_STATUS, false)) {
            Log.e(TAG,"Setting Location Request Object");
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(TWO_MINUTES);
            mLocationRequest.setFastestInterval(1000);
            startLocationUpdates();
        }else{
            Log.e(TAG,"Track Location is false");
        }
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Starting Location Updates");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }else{
            Log.e(TAG,"Permissions not granted");
        }
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "starting location update service");
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        preferences= getSharedPreferences(PrefsHelper.PREF_NAME,0);
        preferences.registerOnSharedPreferenceChangeListener(this);
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e(TAG, "------------------On Connected got");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Location location = null;
                    if (ActivityCompat.checkSelfPermission(LocationSyncService.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(LocationSyncService.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    if (location != null) {
                        mLastLocation = location;
                        createLocationRequest();
                        Log.e("getLocation", "lat:" + mLastLocation.getLatitude() + " and long:" + mLastLocation.getLongitude());
                    } else {
                        Log.e("failLocation", "No Location.");
                    }
                }
            },2000);
            if(mLastLocation!=null) {
                Log.e(TAG, "Last Location is: " + mLastLocation.getLatitude() + ":" + mLastLocation.getLongitude() + ": Provider:" + mLastLocation.getProvider());
            }else {
                Log.e(TAG, "Last Location is null");
            }
        }else{
            Log.e(TAG,"Permission not granted in onConnected");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "location update service called");
        if(intent!=null && intent.getIntExtra(CONN_STATUS_KEY,0)==1){
            mResolvingError = false;
            if(!mGoogleApiClient.isConnected()){
                mGoogleApiClient.connect();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "On Destroy Called");
        stopLocationUpdates();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {

        if (currentBestLocation == null || firstServerUpdate) {
            // A new location is always better than no location
            return true;
        }

        if (!isOutOfDistanceThreshold(location, currentBestLocation)){
            return false;
        }
        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private boolean isOutOfDistanceThreshold(Location newLocation, Location oldLocation){
        if (oldLocation==null){
            return true;
        }
        float dist = newLocation.distanceTo(mLastLocation);
        Log.e(TAG,"update distance : " + dist);
        return dist >= MIN_BOUND_METERS ;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e(TAG, "Location change got");
        if (isBetterLocation(location, mLastLocation)) {
            mLastLocation=location;
            location.getProvider();
            location.getTime();
            location.getLatitude();
            location.getLongitude();
            location.getAccuracy();
            location.getSpeed();

            //Check if gps is enabled
            LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            //Get Intent for Battery Manager
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, ifilter);

            int batteryStatusTime = (int)(System.currentTimeMillis())/1000;

            String chargingStatus;
            int batteryPct=-1;
            // Are we charging / charged?
            if(batteryStatus!=null) {
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    // How are we charging?
                    int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    switch (chargePlug) {
                        case BatteryManager.BATTERY_PLUGGED_USB:
                            chargingStatus = PrefsHelper.PLUGGED_USB;
                            break;

                        case BatteryManager.BATTERY_PLUGGED_AC:
                            chargingStatus = PrefsHelper.PLUGGED_AC;
                            break;

                        case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                            chargingStatus = PrefsHelper.PLUGGED_WIRELESS;
                            break;

                        default:
                            chargingStatus = PrefsHelper.CHARGING;
                            break;
                    }
                } else {
                    chargingStatus = PrefsHelper.NONE;
                }
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                batteryPct = level/scale;
            }else{
                chargingStatus = PrefsHelper.UNAVAILABLE;
            }

            ContentValues location_cv = new ContentValues();
            location_cv.put(Contract.LocationDataEntry.COLUMN_LOCATION_DATA_TIMESTAMP,(int)(location.getTime()/1000));
            location_cv.put(Contract.LocationDataEntry.COLUMN_LOCATION_DATA_LATITUDE,location.getLatitude());
            location_cv.put(Contract.LocationDataEntry.COLUMN_LOCATION_DATA_LONGITUDE,location.getLongitude());
            if(location.getProvider()!=null)
                location_cv.put(Contract.LocationDataEntry.COLUMN_LOCATION_DATA_PROVIDER,location.getProvider());
            if(location.getAccuracy()!=0.0)
                location_cv.put(Contract.LocationDataEntry.COLUMN_LOCATION_DATA_ACCURACY,location.getAccuracy());
            if(location.getSpeed()!=0.0)
                location_cv.put(Contract.LocationDataEntry.COLUMN_LOCATION_DATA_SPEED,location.getSpeed());
            location_cv.put(Contract.LocationDataEntry.COLUMN_LOCATION_DATA_GPS_ENABLED,(isGPSEnabled)?PrefsHelper.GPS_ENABLED:PrefsHelper.GPS_NOT_ENABLED);
            location_cv.put(Contract.LocationDataEntry.COLUMN_LOCATION_DATA_CHARGING_STATUS,chargingStatus);
            location_cv.put(Contract.LocationDataEntry.COLUMN_LOCATION_DATA_BATTERY_STATUS_REMAINING,batteryPct);
            location_cv.put(Contract.LocationDataEntry.COLUMN_LOCATION_DATA_BATTERY_STATUS_TIME,batteryStatusTime);
            Log.e(TAG,"///////////////////////////////////");
            Log.e(TAG,"Got Location change at :"+location.getTime()/1000+" location is: "+location.getLatitude()+":"+location.getLongitude());
            Log.e(TAG,"Location Provider:"+location.getProvider()+" : Location Accuracy:"+location.getAccuracy());
            Log.e(TAG,"Location Speed:"+location.getSpeed()+" : GPS Status:"+isGPSEnabled);
            Log.e(TAG,"Location Charging Status:"+chargingStatus+" : Remaining Time:"+batteryPct);
            Log.e(TAG,"************************************");
            getContentResolver().insert(Contract.LocationDataEntry.CONTENT_URI,location_cv);
            //sendLocationUpdates();
        }else {
            Log.e(TAG, "stale location update");
        }
    }

    private void sendLocationUpdates() {
        Cursor cursor = getContentResolver().query(Contract.LocationDataEntry.CONTENT_URI,
                LOCATION_DATA_PROJECTION,
                null,
                null,
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            int i = 0;
            int timeStampForFirstLocation = 0;
            try{
                JSONArray locationDataArray = new JSONArray();
                do {
                    JSONObject locationObject = new JSONObject();
                    JSONObject batteryStatusObject = new JSONObject();
                    JSONObject locationRequestObject = new JSONObject();
                    locationObject.put("lat",cursor.getDouble(COL_LOCATION_LATITUDE));
                    locationObject.put("lng",cursor.getDouble(COL_LOCATION_LONGITUDE));
                    locationObject.put("timestamp",cursor.getInt(COL_LOCATION_TIMESTAMP));
                    if(i==0)
                        timeStampForFirstLocation = cursor.getInt(COL_LOCATION_TIMESTAMP);
                    if(cursor.getString(COL_LOCATION_PROVIDER)!=null&&!"null".equals(cursor.getString(COL_LOCATION_PROVIDER)))
                        locationObject.put("provider",cursor.getString(COL_LOCATION_PROVIDER));
                    if(cursor.getString(COL_LOCATION_ACCURACY)!=null&&!"null".equals(cursor.getString(COL_LOCATION_ACCURACY)))
                        locationObject.put("accuracy",cursor.getFloat(COL_LOCATION_ACCURACY));
                    if(cursor.getString(COL_LOCATION_SPEED)!=null&&!"null".equals(cursor.getString(COL_LOCATION_SPEED)))
                        locationObject.put("speed",cursor.getFloat(COL_LOCATION_SPEED));
                    locationObject.put("gpsEnabled",(PrefsHelper.GPS_ENABLED.equals(cursor.getString(COL_LOCATION_GPS_ENABLED))));
                    batteryStatusObject.put("timestamp",cursor.getInt(COL_LOCATION_TIMESTAMP));
                    batteryStatusObject.put("charge",cursor.getInt(COL_LOCATION_BATTERY_STATUS_REMAINING_TIME));
                    batteryStatusObject.put("chargingStatus",cursor.getString(COL_LOCATION_CHARGING_STATUS));
                    locationRequestObject.put("location",locationObject);
                    locationRequestObject.put("batteryStatus",batteryStatusObject);
                    locationDataArray.put(i,locationRequestObject);
                    i++;
                } while (cursor.moveToNext());
                submitLocation(locationDataArray,0,i,timeStampForFirstLocation);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "JSON Exception in building ticker array");
            }
            cursor.close();
        }
    }

    public void submitLocation(JSONArray locationArray,int positionToSend,int totalCount, int timestampOfLocationToBeProcessed){

        String url= "***REMOVED***";
        try {
            JSONObject params = (JSONObject) locationArray.get(positionToSend);
            JsonObjectRequest notificationCheck = new JsonObjectRequest(Request.Method.PUT, url, params,
                    updateLocationSuccessListener(locationArray,positionToSend,totalCount,timestampOfLocationToBeProcessed), updateLocationErrorListener()) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<>();
                    String credentials = "***REMOVED***";
                    String credBase64 = Base64.encodeToString(credentials.getBytes(), Base64.DEFAULT).replace("\n", "");
                    headers.put("Content-Type", "application/json; charset=UTF-8");
                    headers.put("Authorization", "Basic " + credBase64);
                    return headers;
                }
            };
            notificationCheck.setRetryPolicy(new DefaultRetryPolicy(10000, 2, 3.0f));
            VolleyNetworkUtils.getInstance().addToRequestQueue(notificationCheck, SUBMIT_LOCATION);
        }catch (JSONException e){
            e.printStackTrace();
            Log.e(TAG,e.toString());
        }
    }

    private Response.Listener<JSONObject> updateLocationSuccessListener(final JSONArray locationArray, final int positionToSend, final int totalCount, final int timestampOfLocationToBeProcessed) {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    Log.e(TAG, "Location Update Successful for TimeStamp:"+timestampOfLocationToBeProcessed);
                    getContentResolver().delete(Contract.LocationDataEntry.CONTENT_URI,
                            Contract.LocationDataEntry.COLUMN_LOCATION_DATA_TIMESTAMP + " = '" + timestampOfLocationToBeProcessed + "'",
                            null);
                    if (positionToSend + 1 <= totalCount) {
                        JSONObject params = (JSONObject) locationArray.get(positionToSend+1);
                        int updatedTimeStamp = params.getJSONObject("location").getInt("timestamp");
                        submitLocation(locationArray, positionToSend, totalCount, updatedTimeStamp);
                    }else{
                        Log.e(TAG,"Submitted all Locations:"+locationArray.length());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG,e.toString());
                }
            }
        };
    }

    public Response.ErrorListener updateLocationErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof AuthFailureError) {
                    Log.e(TAG,"Not Authorized");
                }
            }
        };
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG,"Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG,"Connection Failed");
        if (mResolvingError) {
            Log.e(TAG,"Resolving Error");
        } else {
            mResolvingError = true;
            Intent i = new Intent(this, ResolverActivity.class);
            i.putExtra(ResolverActivity.CONNECT_RESULT_KEY, connectionResult);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    }
}
