package sreekesh.com.locationtracker.service;

import android.Manifest;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import sreekesh.com.locationtracker.ResolverActivity;
import sreekesh.com.locationtracker.model.Contract;
import sreekesh.com.locationtracker.model.RequestObject;
import sreekesh.com.locationtracker.utils.NetworkUtils;
import sreekesh.com.locationtracker.utils.PrefsHelper;

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
    SharedPreferences preferences;

    GoogleApiClient mGoogleApiClient;
    private static final int TIME_THRESHOLD = 120000;
    private static final int TIME_THRESHOLD_FOR_PRECISION = 40000;
    private static final int LOCATION_UPDATE_TIME = 5000;
    private static final float LOCATION_THRESHOLD = 100;
    boolean firstServerUpdate = true;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private boolean mResolvingError = false;

    public LocationSyncService() {
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(PrefsHelper.MAP_LOCATION_TRACK_STATUS)) {
            checkLocationTrackState(s);
        }
    }

    public void checkLocationTrackState(String s) {
        boolean trackingStatus = preferences.getBoolean(s, false);
        mLocationRequest = new LocationRequest();
        if (trackingStatus) {
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(LOCATION_UPDATE_TIME);
            mLocationRequest.setFastestInterval(1000);
            startLocationUpdates();
        } else {
            stopLocationUpdates();
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        if (preferences.getBoolean(PrefsHelper.MAP_LOCATION_TRACK_STATUS, false)) {
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(LOCATION_UPDATE_TIME);
            mLocationRequest.setFastestInterval(1000);
            startLocationUpdates();
        }
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onCreate() {
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        new PrefsHelper(getApplicationContext()).setUpdatingLocationStatus(false);
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Location location = null;
                    if (ActivityCompat.checkSelfPermission(LocationSyncService.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(LocationSyncService.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    if (location != null) {
                        mLastLocation = location;
                        onLocationChanged(location);
                        createLocationRequest();
                    }
                }
            },2000);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TIME_THRESHOLD;
        boolean isSignificantlyOlder = timeDelta < -TIME_THRESHOLD;
        boolean isNewerForMorePrecision = timeDelta > TIME_THRESHOLD_FOR_PRECISION;

        // If it's been more than two minutes since the current location, location is updated
        //      OR
        // If the new location is older than two minutes, it is discarded
        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            return false;
        }

        //if the new location is more than 100m apart from the previous location, the location is updated
        if (isOutOfDistanceThreshold(location, currentBestLocation)){
            return true;
        }

        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta < -25;
        boolean isMoreAccurate = accuracyDelta < -100;

        //If accuray of new location is more than 100m than previous location, location is updated
        //       OR
        //If accuracy is more that 25 and the location is 40 second newer than than the previous location, location is updated
        if (isMoreAccurate) {
            return true;
        } else if (isNewerForMorePrecision && isLessAccurate) {
            return true;
        }
        return false;
    }

    private boolean isOutOfDistanceThreshold(Location newLocation, Location oldLocation){
        if (oldLocation==null){
            return true;
        }
        float dist = newLocation.distanceTo(mLastLocation);
        return dist >= LOCATION_THRESHOLD;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isBetterLocation(location, mLastLocation)) {
            Toast.makeText(getApplicationContext(),"Location Update from :"+location.getProvider(),Toast.LENGTH_LONG).show();
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

                batteryPct = (int)(((float)level/(float)scale)*100);
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
            getContentResolver().insert(Contract.LocationDataEntry.CONTENT_URI,location_cv);
            if(!(new PrefsHelper(getApplicationContext()).getUpdatingLocationStatus())) {
                sendLocationUpdates();
            }
        }
    }

    private void sendLocationUpdates() {
        Cursor cursor = getContentResolver().query(Contract.LocationDataEntry.CONTENT_URI,
                LOCATION_DATA_PROJECTION,
                null,
                null,
                Contract.LocationDataEntry.COLUMN_LOCATION_DATA_TIMESTAMP+" DESC"
        );
        if (cursor != null && cursor.moveToFirst()) {
            try{
                JSONObject locationObject = new JSONObject();
                JSONObject batteryStatusObject = new JSONObject();
                JSONObject locationRequestObject = new JSONObject();
                locationObject.put("lat",cursor.getDouble(COL_LOCATION_LATITUDE));
                locationObject.put("lng",cursor.getDouble(COL_LOCATION_LONGITUDE));
                locationObject.put("timestamp",cursor.getInt(COL_LOCATION_TIMESTAMP));
                int timeStampForFirstLocation = cursor.getInt(COL_LOCATION_TIMESTAMP);
                if(cursor.getString(COL_LOCATION_PROVIDER)!=null&&!"null".equals(cursor.getString(COL_LOCATION_PROVIDER)))
                    locationObject.put("provider",cursor.getString(COL_LOCATION_PROVIDER));
                if(cursor.getString(COL_LOCATION_ACCURACY)!=null&&!"null".equals(cursor.getString(COL_LOCATION_ACCURACY)))
                    locationObject.put("accuracy",cursor.getFloat(COL_LOCATION_ACCURACY));
                if(cursor.getString(COL_LOCATION_SPEED)!=null&&!"null".equals(cursor.getString(COL_LOCATION_SPEED)))
                    locationObject.put("speed",cursor.getFloat(COL_LOCATION_SPEED));
                locationObject.put("gpsEnabled",(PrefsHelper.GPS_ENABLED.equals(cursor.getString(COL_LOCATION_GPS_ENABLED))));
                batteryStatusObject.put("timestamp",cursor.getInt(COL_LOCATION_BATTERY_STATUS_TIME));
                batteryStatusObject.put("charge",cursor.getInt(COL_LOCATION_BATTERY_STATUS_REMAINING_TIME));
                batteryStatusObject.put("chargingStatus",cursor.getString(COL_LOCATION_CHARGING_STATUS));
                locationRequestObject.put("location",locationObject);
                locationRequestObject.put("batteryStatus",batteryStatusObject);
                RequestObject request= new RequestObject();
                request.setTimestampOfCurrentProcessing(timeStampForFirstLocation);
                request.setLocationObject(locationRequestObject);
                SubmitLocations submitLocations = new SubmitLocations();
                submitLocations.execute(request);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "JSON Exception in building ticker array");
            }
            cursor.close();
        }
    }

    public class SubmitLocations extends AsyncTask<RequestObject, Void, Integer> {

        @Override
        protected void onPreExecute() {
            new PrefsHelper(getApplicationContext()).setUpdatingLocationStatus(true);
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(RequestObject... params) {

            try {
                RequestObject request = params[0];
                HttpResponse response = NetworkUtils.makePostRequest(NetworkUtils.URL, request.getLocationObject().toString());
                if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    if(firstServerUpdate)
                        firstServerUpdate=false;
                    return request.getTimestampOfCurrentProcessing();
                }else if(response!=null){
                    Log.e(TAG,"Error:"+response.getStatusLine().getStatusCode());
                    cancel(true);
                }
            }catch (Exception e){
                e.printStackTrace();
                Log.e(TAG,e.toString());
                cancel(true);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer time) {
            super.onPostExecute(time);
            if(time!=null) {
                try {
                    getContentResolver().delete(Contract.LocationDataEntry.CONTENT_URI,
                            Contract.LocationDataEntry.COLUMN_LOCATION_DATA_TIMESTAMP + " = " + time,
                            null);
                    Cursor cursor = getContentResolver().query(Contract.LocationDataEntry.CONTENT_URI,
                            LOCATION_DATA_PROJECTION,
                            null,
                            null,
                            Contract.LocationDataEntry.COLUMN_LOCATION_DATA_TIMESTAMP+" DESC"
                    );
                    if (cursor != null && cursor.moveToFirst()) {
                        JSONObject locationObject = new JSONObject();
                        JSONObject batteryStatusObject = new JSONObject();
                        JSONObject locationRequestObject = new JSONObject();
                        locationObject.put("lat",cursor.getDouble(COL_LOCATION_LATITUDE));
                        locationObject.put("lng",cursor.getDouble(COL_LOCATION_LONGITUDE));
                        locationObject.put("timestamp",cursor.getInt(COL_LOCATION_TIMESTAMP));
                        int timeStampForFirstLocation = cursor.getInt(COL_LOCATION_TIMESTAMP);
                        if(cursor.getString(COL_LOCATION_PROVIDER)!=null&&!"null".equals(cursor.getString(COL_LOCATION_PROVIDER)))
                            locationObject.put("provider",cursor.getString(COL_LOCATION_PROVIDER));
                        if(cursor.getString(COL_LOCATION_ACCURACY)!=null&&!"null".equals(cursor.getString(COL_LOCATION_ACCURACY)))
                            locationObject.put("accuracy",cursor.getFloat(COL_LOCATION_ACCURACY));
                        if(cursor.getString(COL_LOCATION_SPEED)!=null&&!"null".equals(cursor.getString(COL_LOCATION_SPEED)))
                            locationObject.put("speed",cursor.getFloat(COL_LOCATION_SPEED));
                        locationObject.put("gpsEnabled",(PrefsHelper.GPS_ENABLED.equals(cursor.getString(COL_LOCATION_GPS_ENABLED))));
                        batteryStatusObject.put("timestamp",cursor.getInt(COL_LOCATION_BATTERY_STATUS_TIME));
                        batteryStatusObject.put("charge",cursor.getInt(COL_LOCATION_BATTERY_STATUS_REMAINING_TIME));
                        batteryStatusObject.put("chargingStatus",cursor.getString(COL_LOCATION_CHARGING_STATUS));
                        locationRequestObject.put("location",locationObject);
                        locationRequestObject.put("batteryStatus",batteryStatusObject);
                        RequestObject request= new RequestObject();
                        request.setTimestampOfCurrentProcessing(timeStampForFirstLocation);
                        request.setLocationObject(locationRequestObject);
                        SubmitLocations submitLocations = new SubmitLocations();
                        submitLocations.execute(request);
                        cursor.close();
                    }else{
                        new PrefsHelper(getApplicationContext()).setUpdatingLocationStatus(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e.toString());
                    new PrefsHelper(getApplicationContext()).setUpdatingLocationStatus(false);
                }
            }else{
                new PrefsHelper(getApplicationContext()).setUpdatingLocationStatus(false);
            }
        }

        @Override
        protected void onCancelled(Integer status) {
            super.onCancelled(status);
            new PrefsHelper(getApplicationContext()).setUpdatingLocationStatus(false);
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        new PrefsHelper(getApplicationContext()).setUpdatingLocationStatus(false);}

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
