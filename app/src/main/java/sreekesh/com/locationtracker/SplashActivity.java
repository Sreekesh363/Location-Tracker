package sreekesh.com.locationtracker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import sreekesh.com.locationtracker.service.LocationSyncService;
import sreekesh.com.locationtracker.utils.PrefsHelper;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = SplashActivity.class.getSimpleName();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView splashImage = (ImageView) findViewById(R.id.splash_image);
        GlideDrawableImageViewTarget imageViewTarget = new GlideDrawableImageViewTarget(splashImage);
        Glide.with(this).load(R.drawable.infinity).into(imageViewTarget);


        if(new PrefsHelper(this).getLocationTrackingStatus()) {
            Log.e(TAG, "Location Tracking enabled");
            Intent locationSyncServiceIntent = new Intent(getApplicationContext(), LocationSyncService.class);
            startService(locationSyncServiceIntent);
        }else{
            Log.e(TAG, "Location Tracking disabled");
        }

        try {
            ScheduledFuture<?> countdown = scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    Intent mapIntent = new Intent(SplashActivity.this,LocationTrackActivity.class);
                    mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(mapIntent);
                }}, 4000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(TAG, "onMapReady Exception:" + e.toString());
        }
    }
}
