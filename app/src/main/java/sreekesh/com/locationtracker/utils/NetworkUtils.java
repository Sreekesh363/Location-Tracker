package sreekesh.com.locationtracker.utils;

import android.app.Application;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;

/**
 * Created by sree on 7/1/17.
 */

public class NetworkUtils {

    public static final String TAG = NetworkUtils.class.getSimpleName();

    public static String URL= "https://api.locus.sh/v1//client/test/user/candidate/location";

    public static synchronized HttpResponse makePostRequest(String uri, String json) {
        HttpResponse httpResponse = null;
        try {
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(new StringEntity(json));
            String credentials = "test/candidate:c00e-4764";
            String credBase64 = Base64.encodeToString(credentials.getBytes(), Base64.DEFAULT).replace("\n", "");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.addHeader("Authorization", "Basic " + credBase64);
            HttpParams httpParams = new BasicHttpParams();
            int timeoutConnection = 3000;
            HttpConnectionParams.setConnectionTimeout(httpParams, timeoutConnection);
            httpResponse = new DefaultHttpClient(httpParams).execute(httpPost);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
        return httpResponse;
    }
}
