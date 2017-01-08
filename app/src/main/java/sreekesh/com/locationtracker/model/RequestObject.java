package sreekesh.com.locationtracker.model;

import org.json.JSONArray;

/**
 * Created by sree on 8/1/17.
 */

public class RequestObject {
    private int currentPosition;
    private int totalCount;
    private int timestampOfCurrentProcessing;
    private JSONArray locationArray;

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getTimestampOfCurrentProcessing() {
        return timestampOfCurrentProcessing;
    }

    public void setTimestampOfCurrentProcessing(int timestampOfCurrentProcessing) {
        this.timestampOfCurrentProcessing = timestampOfCurrentProcessing;
    }

    public JSONArray getLocationArray() {
        return locationArray;
    }

    public void setLocationArray(JSONArray locationArray) {
        this.locationArray = locationArray;
    }
}
