package sreekesh.com.locationtracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import sreekesh.com.locationtracker.service.LocationSyncService;

/**
 * Created by sree on 7/1/17.
 */

public class BootCompleteReceiver extends BroadcastReceiver {

    public BootCompleteReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // start your service here
        context.startService(new Intent(context, LocationSyncService.class));
    }
}
