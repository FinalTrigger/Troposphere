package leetinsider.com.troposphere;

import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.NetworkInterface;


public class MainActivity extends ActionBarActivity {
    //TAG variable for logging activity in MainActivity
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Forecast.io developer url= apiKey+latatude+longitude
        String apiKey = "4d0e45683b7534f37310e7e716272643";
        double latatude = 40.4262,
                longitude = -74.4182;
        String forecastIoUrl = "https://api.forecast.io/forecast/" + apiKey + "/" + latatude + "," + longitude;

        //Object - Conditional statement to verify that Network is available since app network based.
        //Return Boolean value true/false of network availability
        if(isNetworkAvailable()) {
            //OkHTTP is a web server client. Create new client below and Asynchronous GET to forecast.io
            //Required Android Permission INTERNET
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastIoUrl)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override //If GET request fails
                public void onFailure(Request request, IOException e) {

                }

                @Override //Completed network GET
                public void onResponse(Response response) throws IOException {
                    try {
                        Log.v(TAG, response.body().string());
                        if (response.isSuccessful()) {

                        } else {
                            //Object - Dialog box to alert that response failed
                            alertUserAboutError();
                        }
                    } catch (IOException e) {
                        //Log the exception
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
        }
        //Ending conditional statement if network is not available
        else{
            //Temp message to alert user to verify network
            Toast.makeText(this, getString(R.string.no_network_available), Toast.LENGTH_LONG).show();
        }
    }
    //Object that checks if network is available. Required Android permission ACCESS NETWORK STATE
    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        //AND statement verify network has a response and connects, set boolean to true and return
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }
        return isAvailable;

    }
    //Object related to Alert Dialog Fragment activity
    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "dialog error");
    }
}
