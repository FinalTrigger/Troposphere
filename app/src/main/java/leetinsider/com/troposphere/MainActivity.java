package leetinsider.com.troposphere;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.ConnectionResult;

import android.content.Intent;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by FinalTrigger on 1/24/15.
 */
public class MainActivity extends ActionBarActivity implements
        ConnectionCallbacks, OnConnectionFailedListener  {
    //Create TAG variable for logging on MainActivity
    public static final String TAG = MainActivity.class.getSimpleName();

    private CurrentWeather mCurrentWeather;
    //Butterknife variables for Model-View-Controller
    @InjectView(R.id.timeLabel) TextView mTimeLabel;
    @InjectView(R.id.temperatureLabel) TextView mTemperatureLabel;
    @InjectView(R.id.humidityValue) TextView mHumidityValue;
    @InjectView(R.id.precipValue) TextView mPrecipValue;
    @InjectView(R.id.summaryLabel) TextView mSummaryLabel;
    @InjectView(R.id.iconImageView) ImageView mIconImageView;
    @InjectView(R.id.refreshImageView) ImageView mRefreshImageView;
    @InjectView(R.id.progressBar) ProgressBar mProgressBar;
    @InjectView(R.id.locationLabel) TextView mLocationLabel;

    private Location mLastLocation;

    private GoogleApiClient mGoogleApiClient;

    private AddressResultReceiver mResultReceiver;

    private String mAddressFromGeoCoder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Inject butterknife view variables on startup
        ButterKnife.inject(this);

        //Hide progress spinner on load
        mProgressBar.setVisibility(View.INVISIBLE);
        buildGoogleApiClient();

        mGoogleApiClient.connect();

        mResultReceiver = new AddressResultReceiver(new Handler());

        //Set click listener for refresh - call getForecast function
        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastLocation != null) {
                    getForecast(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    getLocationName();
                }
            }
        });
        //Start app by getting forecast.io data
        //Log letting dev know activity is running
        Log.d(TAG, "Main UI code is running!");
    }
    @Override
    protected void onStop(){
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    //Main function that parse forecast.io api for data
    private void getForecast(double latitude, double longitude) {
        //Api dev key to inject to the forecastUrl
        String apiKey = "27974c4bc33201748eaf542a6769c3b7";
        String forecastUrl = "https://api.forecast.io/forecast/" + apiKey +
                "/" + latitude + "," + longitude;

        //Check if network is available prior to getting data
        if (isNetworkAvailable()) {
            //Switch progress<->refresh icon
            toggleRefresh();

            //Create new OkHttpClient for GET request
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    /*If Fail:
                    -stop the progress spinner
                    -Alert the user of network issue
                     */
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError();
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        /*If Response is made
                        -Stop the progress spinner
                        -Set string jsonData to the text in JSON GET request
                        -Call getCurrentDetails from jsonData Object and
                        set the data to mCurrentWeather member variable
                         */
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mCurrentWeather = getCurrentDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //Update views with new data
                                    updateDisplay();
                                }
                            });
                        } else {
                            alertUserAboutError();
                        }
                    }
                    catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                    catch (JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
        }
        else {
            Toast.makeText(this, getString(R.string.no_network_available),
                    Toast.LENGTH_LONG).show();
        }
    }
    protected synchronized void buildGoogleApiClient() {
        Log.d(TAG,"Building Google API");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        Log.d(TAG,"Completed Build Google API");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG,"Connected to Google");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation == null) {
            Log.e(TAG,"Null Last location");
        }
        else{
            Log.d(TAG,String.format("Retrieved location latitude: [%f], Longitude: [%f]",mLastLocation.getLatitude(), mLastLocation.getLongitude()));
            getForecast(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            getLocationName();
        }
    }
    @Override
    public void onConnectionSuspended(int cause){
        Log.e(TAG,"Connection to Google suspended.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result){
         if (result.hasResolution()) {
             Log.e(TAG,"Connection to Google failed.  Attempting reconnect.");
             mGoogleApiClient.connect();
        }
         else {
             Log.e(TAG,"Connection to Google failed.  No reconnect attempt.");
         }
    }
    protected void getLocationName() {
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(this, FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        startService(intent);
    }

    private void toggleRefresh() {
        /*
        If progress bar is not active, set refresh icon off and progress on
        Else progress bar off, refresh icon on
         */
        if (mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        }
        else{
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    //Update the activity main views to the variables from the data in CurrentWeather
    private void updateDisplay() {
        if (mCurrentWeather != null){
            mTemperatureLabel.setText(mCurrentWeather.getTemperature() + "");
            mTimeLabel.setText("At " + mCurrentWeather.getFormattedTime() + " it is currently");
            mHumidityValue.setText(mCurrentWeather.getHumidity() + "");
            mPrecipValue.setText(mCurrentWeather.getPrecipChance() + "%");
            mSummaryLabel.setText(mCurrentWeather.getSummary());

            //Update the weather icon
            Drawable drawable = getResources().getDrawable(mCurrentWeather.getIconId());
            mIconImageView.setImageDrawable(drawable);
        }

        if (mAddressFromGeoCoder != null){
            mLocationLabel.setText(mAddressFromGeoCoder);
        }
    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException {
        //New JSON object forecast to contain all JSON data
        JSONObject forecast = new JSONObject(jsonData);
        //Log the current timezone depending on the location
        String timezone = forecast.getString("timezone");
        Log.i(TAG, "From JSON: " + timezone);

        //Create new JSON object that collects data from the currently section of the JSON response
        JSONObject currently = forecast.getJSONObject("currently");

        /*Parse each part of JSON data and get the data we want
        Done by going into the CurrentWeather.java class and call each of the
        set functions that take the data and set each variable we will use to be returned
        to the main activity by calling example: ".getTemperature() which returns a member variable
         */
        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTimeZone(timezone);

        Log.d(TAG, currentWeather.getFormattedTime());

        return currentWeather;
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }
        /*
         *  Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                mAddressFromGeoCoder = resultData.getString(Constants.RESULT_DATA_KEY);
                Log.d(TAG,String.format("Address [%s] was found", mAddressFromGeoCoder));
            }
            else
            {
                Log.e(TAG,"No Address returned geocoder!");
                mAddressFromGeoCoder = "No Address Found";
            }
            updateDisplay();
        }
    }

}

