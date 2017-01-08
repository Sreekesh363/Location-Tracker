package sreekesh.com.locationtracker.model;

import org.json.JSONObject;

/**
 * Created by sree on 8/1/17.
 */

public class RequestObject {
    private int timestampOfCurrentProcessing;
    private JSONObject locationObject;

    public int getTimestampOfCurrentProcessing() {
        return timestampOfCurrentProcessing;
    }

    public void setTimestampOfCurrentProcessing(int timestampOfCurrentProcessing) {
        this.timestampOfCurrentProcessing = timestampOfCurrentProcessing;
    }

    public JSONObject getLocationObject() {
        return locationObject;
    }

    public void setLocationObject(JSONObject locationObject) {
        this.locationObject = locationObject;
    }
}
