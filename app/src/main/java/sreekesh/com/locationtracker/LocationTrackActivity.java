package sreekesh.com.locationtracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import sreekesh.com.locationtracker.service.LocationSyncService;
import sreekesh.com.locationtracker.utils.PrefsHelper;

public class LocationTrackActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 999;
    Button startTrackingButton;
    PrefsHelper prefsHelper;
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

        startTrackingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!prefsHelper.getLocationTrackingStatus()){
                    if (ContextCompat.checkSelfPermission(LocationTrackActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(LocationTrackActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        // Should we show an explanation?
                        if (ActivityCompat.shouldShowRequestPermissionRationale(LocationTrackActivity.this,
                                Manifest.permission.ACCESS_FINE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(LocationTrackActivity.this,
                                Manifest.permission.ACCESS_COARSE_LOCATION)) {
                            String text = "Please allow the application to access the Location permission.";
                            if (findViewById(android.R.id.content) != null) {
                                Snackbar snackbar = Snackbar
                                        .make(findViewById(android.R.id.content), text, Snackbar.LENGTH_LONG)
                                        .setDuration(Snackbar.LENGTH_LONG)
                                        .setAction("OK", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                            }
                                        });
                                snackbar.setActionTextColor(getResources().getColor(R.color.snackbar_teel));
                                // Changing action button text color
                                View sbView = snackbar.getView();
                                TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                                textView.setTextColor(getResources().getColor(R.color.snackbar_yellow));
                                snackbar.show();
                            } else {
                                Toast.makeText(LocationTrackActivity.this, text, Toast.LENGTH_LONG).show();
                            }
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
                        startTrackingButton.setText(getString(R.string.stop_location_tracking));
                        prefsHelper.setLocationTrackingStatus(true);
                        Intent locationSyncServiceIntent = new Intent(getApplicationContext(), LocationSyncService.class);
                        startService(locationSyncServiceIntent);
                    }
                }else{
                    startTrackingButton.setText(getString(R.string.start_location_tracking));
                    prefsHelper.setLocationTrackingStatus(false);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED) && (grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    startTrackingButton.setText(getString(R.string.stop_location_tracking));
                    prefsHelper.setLocationTrackingStatus(true);
                    Intent locationSyncServiceIntent = new Intent(getApplicationContext(), LocationSyncService.class);
                    startService(locationSyncServiceIntent);
                }
            }
        }
    }
}
