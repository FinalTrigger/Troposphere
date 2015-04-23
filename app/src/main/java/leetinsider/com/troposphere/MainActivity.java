package leetinsider.com.troposphere;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        updateDisplay();
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
        //Create a Geocoder and use it to resolve address from lat and long coords
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String errorMessage = "";
        // Address found using the Geocoder.
        List<Address> addresses = null;

        try {
            // Using getFromLocation() returns an array of Addresses for the area immediately
            // surrounding the given latitude and longitude. The results are a best guess and are
            // not guaranteed to be accurate.
            addresses = geocoder.getFromLocation(
                    mLastLocation.getLatitude(),
                    mLastLocation.getLongitude(),
                    // In this sample, we get just a single address.
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.no_location_data_provided);
            Log.e(TAG, errorMessage, ioException);
            mAddressFromGeoCoder = "No Address Found";
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + mLastLocation.getLatitude() +
                    ", Longitude = " + mLastLocation.getLongitude(), illegalArgumentException);
            mAddressFromGeoCoder = "No Address Found";
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size() == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Log.e(TAG, errorMessage);
                mAddressFromGeoCoder = "No Address Found";
            }
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();
            // Fetch the address lines using {@code getAddressLine},
            // join them, and send them to the thread. The {@link android.location.address}
            // class provides other options for fetching address details that you may prefer
            // to use. Here are some examples:
            // getLocality() ("Mountain View", for example)
            // getAdminArea() ("CA", for example)
            // getPostalCode() ("94043", for example)
            // getCountryCode() ("US", for example)
            // getCountryName() ("United States", for example)
//            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
            //              addressFragments.add(address.getAddressLine(i));
            //        }
            if (address.getLocality() != null) addressFragments.add(address.getLocality());
            if (address.getAdminArea() != null) addressFragments.add(address.getAdminArea());
            if (address.getPostalCode() != null)addressFragments.add(address.getPostalCode());
            Log.i(TAG, getString(R.string.address_found));
            mAddressFromGeoCoder = TextUtils.join(System.getProperty("line.separator"), addressFragments);
        }
        updateDisplay();
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

}

