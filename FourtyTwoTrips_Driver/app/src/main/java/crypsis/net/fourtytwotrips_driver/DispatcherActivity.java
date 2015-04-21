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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class DispatcherActivity extends ActionBarActivity {

    Context context;
    String DISPATCHER_RECEIVER_NAME;
    BroadcastReceiver dReceiver;
    LocalStorageData localStorageData;

    TextView connectionStatus;
    ProgressBar loadingIndicator;
    Button tryAgain,signIn;

    String userMobile,userToken;

    Intent homeIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dispatcher_layout);
        context=this;
        DISPATCHER_RECEIVER_NAME = getResources().getString(R.string.DISPATCHER_RECEIVER_NAME);

        //getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.rgb(250, 150, 56)));

        localStorageData = new LocalStorageData(this);

        connectionStatus = (TextView) findViewById(R.id.connectionStatus);
        loadingIndicator = (ProgressBar) findViewById(R.id.loadingIndicator);
        tryAgain =  (Button) findViewById(R.id.tryAgain);
        signIn =  (Button) findViewById(R.id.signIn);

        tryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingIndicator.setVisibility(View.VISIBLE);connectionStatus.setVisibility(View.GONE);
                tryAgain.setVisibility(View.GONE);signIn.setVisibility(View.GONE);
                HttpOperations.httpTask(context, "Get", getResources().getString(R.string.api_address), userMobile, userToken, DISPATCHER_RECEIVER_NAME);    // ReConnecting..
            }
        });

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),LoginActivity.class));
                finish();
            }
        });

        // Checking for Log In ..

        userMobile = localStorageData.getMobile();
        userToken = localStorageData.getToken();

        if( userToken.equals("NA") ) {  // If Not Logged In ..
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        }else {     // Already Logged In..
            HttpOperations.httpTask(context, "Get", getResources().getString(R.string.api_address), userMobile, userToken, DISPATCHER_RECEIVER_NAME);    // Connecting..
        }


        // BroadCast Receiver ..

        IntentFilter intentFilter = new IntentFilter(DISPATCHER_RECEIVER_NAME);
        dReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Log.d("42Trips", DISPATCHER_RECEIVER_NAME+"  :  "+intent);

                try {
                    String message = intent.getStringExtra("message");
                    JSONObject responseData = new JSONObject(intent.getStringExtra("responseData"));

                    if( message.equalsIgnoreCase("Get_Success") ) {

                        connectionStatus.setVisibility(View.GONE);

                        boolean onTrip=false;try{ onTrip = responseData.getBoolean("on_trip"); }catch (Exception e){}

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
                    }
                    else if( message.equalsIgnoreCase("Get_Failure")  || message.equalsIgnoreCase("Get_Error") ) {
                        tryAgain.setVisibility(View.VISIBLE);tryAgain.setEnabled(true);loadingIndicator.setVisibility(View.GONE);
                        signIn.setVisibility(View.VISIBLE);signIn.setEnabled(true);
                        connectionStatus.setVisibility(View.VISIBLE);
                        connectionStatus.setText("Unable to Connect ! , Either Ur previous Token Expired (or) Server Internal Error occured ,  { TryAgain or SignIn again }");
                    }

                }catch (Exception e){e.printStackTrace();}
            }
        };
        this.registerReceiver(dReceiver, intentFilter);//registering Receiver ..

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
        try{  this.unregisterReceiver(dReceiver); }catch (Exception e){e.printStackTrace();}
    }

    @Override
    protected void onStop() {
        super.onStop();
        try{  this.unregisterReceiver(dReceiver); }catch (Exception e){e.printStackTrace();}
    }
}
