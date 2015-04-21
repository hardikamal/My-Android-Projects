package crypsis.net.fourtytwotrips_driver;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;


public class LoginActivity extends ActionBarActivity {

    Context context;
    LocalStorageData localStorageData;
    String LOGIN_RECEIVER_NAME;
    BroadcastReceiver lReceiver;

    Button confirmLogin;
    ProgressBar loginLoadingIndicator;
    EditText userID,userPassword;
    TextView loginHint;

    Intent homeIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);
        context = this;
        LOGIN_RECEIVER_NAME = getResources().getString(R.string.LOGIN_RECEIVER_NAME);

        //getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.rgb(250, 150, 56)));

        localStorageData = new LocalStorageData(context);

        userID = (EditText) findViewById(R.id.userID);
        userPassword = (EditText) findViewById(R.id.userPassword);
        confirmLogin = (Button) findViewById(R.id.confirmLogin);
        loginLoadingIndicator = (ProgressBar) findViewById(R.id.loginLoadingIndicator);
        loginHint = (TextView) findViewById(R.id.loginHint);

        confirmLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(userPassword.getWindowToken(), 0);       // Hiding KeyBoard..
                in.hideSoftInputFromWindow(userID.getWindowToken(), 0);

                String loginMobile = userID.getText().toString();
                String loginPassword = userPassword.getText().toString();

                if( loginMobile.length() > 0 && loginPassword.length() > 0 ) {
                    confirmLogin.setEnabled(false);
                    loginLoadingIndicator.setVisibility(View.VISIBLE);loginHint.setText("");
                    HttpOperations.httpTask(context, "Login", getResources().getString(R.string.api_address), loginMobile, loginPassword, LOGIN_RECEIVER_NAME); //Sending Login Request,.,.
                }else{
                    loginHint.setText("Plz. Enter UserID and Password to Continue..");
                }
            }
        });

        // BroadCast Receiver ..

        IntentFilter intentFilter = new IntentFilter(LOGIN_RECEIVER_NAME);
        lReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Log.d("42Trips", LOGIN_RECEIVER_NAME+"  :  " + intent);

                String message = intent.getStringExtra("message");

                if( message.equalsIgnoreCase("Login_Success") ) {
                    try{
                        JSONObject responseData = new JSONObject(intent.getStringExtra("responseData"));

                        localStorageData.update(responseData.getString("mobile"), responseData.getString("token"), responseData.getString("name"));
                        boolean onTrip = responseData.getBoolean("on_trip");

                        loginHint.setText("Login Success : "+localStorageData.getName());

                        homeIntent = new Intent(getApplicationContext(), HomeActivity.class);
                        homeIntent.putExtra("onTrip", onTrip);

                        if ( onTrip == true ) {
                            JSONArray tripTravelledLocations = null;
                            ArrayList<LatLng> travelledLocations = new ArrayList<LatLng>();

                            try{
                                tripTravelledLocations=responseData.getJSONArray("location_array");
                                if( tripTravelledLocations != null && tripTravelledLocations.length() > 0 ) {
                                    for( int i=0;i<tripTravelledLocations.length();i++){
                                        JSONObject location = tripTravelledLocations.getJSONObject(i);
                                        LatLng loc = new LatLng(location.getDouble("lat"), location.getDouble("lng"));
                                        travelledLocations.add(loc);
                                    }
                                }
                            }catch (Exception e){e.printStackTrace();}

                            homeIntent.putParcelableArrayListExtra("travelledLocations", (ArrayList<LatLng>) travelledLocations);
                        }
                        checkLocationAccessAndProceed();    // Checking LocationAccess..

                    }catch (Exception e){e.printStackTrace();}
                }
                else if( message.equalsIgnoreCase("Login_Failure") ) {
                    confirmLogin.setEnabled(true);
                    loginLoadingIndicator.setVisibility(View.GONE);
                    loginHint.setText("Login Failure : Mobile/Password is/are Wrong, TryAgain");
                }
                else if( message.equalsIgnoreCase("Login_Error") ) {
                    confirmLogin.setEnabled(true);
                    loginLoadingIndicator.setVisibility(View.GONE);
                    loginHint.setText("Login Error : Server Internal Error occured (or) Network Connection Lost , TryAgain");
                }
            }
        };
        this.registerReceiver(lReceiver, intentFilter);//registering Receiver ..

    }

    public void checkLocationAccessAndProceed(){

        boolean status = false;
        LocationManager locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
        if (locationManager != null) {
            status = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if ( status == false ){
                status = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
        }

        if ( status == true ){
            new FusedLocationService(context, false);
            Toast.makeText(getApplicationContext(), "Launching Home, Plz. wait..", Toast.LENGTH_LONG).show();
            startActivity(homeIntent);
            finish();
        }
        else{
            Toast.makeText(getApplicationContext(), "Plz. Enable Location Access From Settings to Continue....", Toast.LENGTH_LONG).show();
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setCancelable(false);

            alertDialog.setTitle("Location Access is Disabled !");
            alertDialog.setMessage("Go to Settings & Enable Location Access to Continue....");

            alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, 1);
                }
            });
            alertDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ( requestCode==1 ) {     // From Location Settings..
            checkLocationAccessAndProceed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{  this.unregisterReceiver(lReceiver); }catch (Exception e){e.printStackTrace();}
    }

    @Override
    protected void onStop() {
        super.onStop();
        try{  this.unregisterReceiver(lReceiver); }catch (Exception e){e.printStackTrace();}
    }
}
