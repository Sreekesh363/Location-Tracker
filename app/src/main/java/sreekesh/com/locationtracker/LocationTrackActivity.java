package sreekesh.com.locationtracker;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import sreekesh.com.locationtracker.service.LocationSyncService;
import sreekesh.com.locationtracker.utils.PrefsHelper;

public class LocationTrackActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener{

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 999;
    private static final String TAG = "Maps Activity";
    Button startTrackingButton;
    PrefsHelper prefsHelper;

    private static final int REQUEST_CHECK_SETTINGS = 1;
    private static final int REQUEST_RESOLVE_ERROR = 2;
    private static final String DIALOG_ERROR = "dialog_error_play_services";
    private static final String DIALOG_TAG = "dialog_tag_error_play_services";

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    private ErrorDialogFragment mDialogFragment;
    private LocationRequest mLocationRequestBalancedPowerAccuracy;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_track);
        startTrackingButton = (Button) findViewById(R.id.start_location_tracking);

        prefsHelper = new PrefsHelper(this);

        if(prefsHelper.getLocationTrackingStatus()){
            startTrackingButton.setText(getString(R.string.stop_location_tracking));
        }else{
            startTrackingButton.setText(getString(R.string.start_location_tracking));
        }

        if (ContextCompat.checkSelfPermission(LocationTrackActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(LocationTrackActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(LocationTrackActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(LocationTrackActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                startTrackingButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (ContextCompat.checkSelfPermission(LocationTrackActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(LocationTrackActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            android.support.v7.app.AlertDialog.Builder details_builder = new android.support.v7.app.AlertDialog.Builder(LocationTrackActivity.this);
                            LayoutInflater details_inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            View select_place_prompt = details_inflater.inflate(R.layout.alert_dialog_location, null);
                            details_builder.setView(select_place_prompt);
                            Button ok = (Button) select_place_prompt.findViewById(R.id.ok_button);
                            ok.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
                                    cancelDialog();
                                }
                            });
                            dialog = details_builder.show();
                        }else{
                            if(!prefsHelper.getLocationTrackingStatus()){
                                startTrackingButton.setText(getString(R.string.stop_location_tracking));
                                prefsHelper.setLocationTrackingStatus(true);
                                Intent locationSyncServiceIntent = new Intent(getApplicationContext(), LocationSyncService.class);
                                startService(locationSyncServiceIntent);
                            }else{
                                startTrackingButton.setText(getString(R.string.start_location_tracking));
                                prefsHelper.setLocationTrackingStatus(false);
                            }
                        }
                    }
                });
            } else {
                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(LocationTrackActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            if(mGoogleApiClient==null)
                enableLocationServices();
            startTrackingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!prefsHelper.getLocationTrackingStatus()){
                        startTrackingButton.setText(getString(R.string.stop_location_tracking));
                        prefsHelper.setLocationTrackingStatus(true);
                        Intent locationSyncServiceIntent = new Intent(getApplicationContext(), LocationSyncService.class);
                        startService(locationSyncServiceIntent);
                    }else{
                        startTrackingButton.setText(getString(R.string.start_location_tracking));
                        prefsHelper.setLocationTrackingStatus(false);
                    }
                }
            });
        }
    }

    private void cancelDialog() {
        dialog.dismiss();
        dialog.cancel();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED) && (grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    if(mGoogleApiClient==null)
                        enableLocationServices();
                    startTrackingButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(!prefsHelper.getLocationTrackingStatus()){
                                startTrackingButton.setText(getString(R.string.stop_location_tracking));
                                prefsHelper.setLocationTrackingStatus(true);
                                Intent locationSyncServiceIntent = new Intent(getApplicationContext(), LocationSyncService.class);
                                startService(locationSyncServiceIntent);
                            }else{
                                startTrackingButton.setText(getString(R.string.start_location_tracking));
                                prefsHelper.setLocationTrackingStatus(false);
                            }
                        }
                    });
                    if(prefsHelper.getLocationTrackingStatus()) {
                        Intent locationSyncServiceIntent = new Intent(getApplicationContext(), LocationSyncService.class);
                        startService(locationSyncServiceIntent);
                    }
                }else{
                    startTrackingButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            android.support.v7.app.AlertDialog.Builder details_builder = new android.support.v7.app.AlertDialog.Builder(LocationTrackActivity.this);
                            LayoutInflater details_inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            View select_place_prompt = details_inflater.inflate(R.layout.alert_dialog_location, null);
                            details_builder.setView(select_place_prompt);
                            Button ok = (Button) select_place_prompt.findViewById(R.id.ok_button);
                            ok.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
                                    cancelDialog();
                                }
                            });
                            dialog = details_builder.show();
                        }
                    });
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.e(TAG,"Result is true");
                        Intent locationSyncServiceIntent = new Intent(getApplicationContext(), LocationSyncService.class);
                        startService(locationSyncServiceIntent);
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getApplicationContext(), "Enable location services for seamless functioning", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(getApplicationContext(), "Location services unavailable on this device, functionality may be affected", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;

            case REQUEST_RESOLVE_ERROR:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        mResolvingError = false;
                        if (mDialogFragment != null) {
                            mDialogFragment.dismiss();
                            mDialogFragment = null;
                        }
                        if (!mGoogleApiClient.isConnecting() &&
                                !mGoogleApiClient.isConnected()) {
                            mGoogleApiClient.connect();
                            Log.e(TAG,"Result is true after resolving error");
                            Intent locationSyncServiceIntent = new Intent(getApplicationContext(), LocationSyncService.class);
                            startService(locationSyncServiceIntent);
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        if (mDialogFragment != null) {
                            mDialogFragment.dismiss();
                            mDialogFragment = null;
                        }
                        Toast.makeText(getApplicationContext(), "Please install google play services and try again", Toast.LENGTH_SHORT).show();
                        finish();
                        break;
                    default:
                        if (mDialogFragment != null) {
                            mDialogFragment.dismiss();
                            mDialogFragment = null;
                        }
                        Toast.makeText(getApplicationContext(), "Unsupported device", Toast.LENGTH_SHORT).show();
                        finish();
                        break;
                }
                break;
            default:
                break;
        }
    }

    private void enableLocationServices() {
        buildGoogleApiClient();
        setUpLocationRequests();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    private void setUpLocationRequests() {
        mLocationRequestBalancedPowerAccuracy = new LocationRequest();
        mLocationRequestBalancedPowerAccuracy.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequestBalancedPowerAccuracy.setInterval(3000);
        mLocationRequestBalancedPowerAccuracy.setFastestInterval(1000);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                mGoogleApiClient.connect();
            }
        } else {
            mResolvingError = true;
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    private void showErrorDialog(int errorCode) {
        if (mDialogFragment == null) {
            mDialogFragment = new ErrorDialogFragment();
            Bundle args = new Bundle();
            args.putInt(DIALOG_ERROR, errorCode);
            mDialogFragment.setArguments(args);
            mDialogFragment.setCancelable(false);
            mDialogFragment.show(getSupportFragmentManager(), DIALOG_TAG);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequestBalancedPowerAccuracy)
                .setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Intent locationSyncServiceIntent = new Intent(getApplicationContext(), LocationSyncService.class);
                        startService(locationSyncServiceIntent);

                        return;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    LocationTrackActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        Toast.makeText(getApplicationContext(), "Location services not available. Please enable them for proceeding further.", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    public static class ErrorDialogFragment extends DialogFragment {

        public ErrorDialogFragment() {
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }
    }
}
