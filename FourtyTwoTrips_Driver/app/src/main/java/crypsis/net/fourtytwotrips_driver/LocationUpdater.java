package crypsis.net.fourtytwotrips_driver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONObject;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import br.net.bmobile.websocketrails.WebSocketRailsDispatcher;

public class LocationUpdater extends Service{

    Context context;
    LocalStorageData localStorageData;
    String LOCATION_UPDATER_RECEIVER_NAME;
    BroadcastReceiver lReceiver;

    String userMobile="",userToken="";

    double latitude,longitude,accuracy;
    Location currentLocation;
    long timestamp;
    boolean isGpsEnabled;

    LocationQueue locationQueue;

    FusedLocationService fusedLocationService;
    LocationManager locationManager;

    WebSocketRailsDispatcher dispatcher;
    Handler uHandler = new Handler();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context=getApplicationContext();

        LOCATION_UPDATER_RECEIVER_NAME = getResources().getString(R.string.LOCATION_UPDATER_RECEIVER_NAME);

        localStorageData=new LocalStorageData(context);
        userMobile=localStorageData.getMobile();
        userToken=localStorageData.getToken();

        fusedLocationService = new FusedLocationService(context, true);
        locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);

        locationQueue=new LocationQueue(userMobile,userToken);  // Initializing Location Queue

        try {
            dispatcher = new WebSocketRailsDispatcher(new URL(getResources().getString(R.string.socket_address)));
            dispatcher.connect();
        }catch (Exception e){ e.printStackTrace(); }

        // BroadCast Receiver ..

        IntentFilter intentFilter = new IntentFilter(LOCATION_UPDATER_RECEIVER_NAME);
        lReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                try {
                    String message = intent.getStringExtra("message");
                    Map<String, Object> responseMap = (Map<String, Object>) intent.getSerializableExtra("responseMap");

                    if( message.equalsIgnoreCase("Send_Location_Success") ) {   // On Location Update Success..
                        int stored_locations = Integer.parseInt(responseMap.get("count").toString());
                        locationQueue.removeLocations(stored_locations);//Removing Updated Locations
                    }
                    else if( message.equalsIgnoreCase("Send_Location_Failure") ) {
                        if ( responseMap.get("message").toString().equalsIgnoreCase("Authentication Failed") == true ){
                            Intent i = new Intent(getResources().getString(R.string.HOME_RECEIVER_NAME)).putExtra("message", "Token_Expired");
                            context.sendBroadcast(i);
                            stopSelf();
                        }
                    }

                }catch (Exception e){ e.printStackTrace(); }

            }
        };
        this.registerReceiver(lReceiver, intentFilter);//registering our receiver

        // Handler for Finding & Updating Location every 5sec ..

        uHandler.postDelayed(updateLocation, 100);  // Handler to Find & Update Location every 5sec..

    }

    private Runnable updateLocation = new Runnable () {
        public void run() {

            Location location=fusedLocationService.getLocation();

            if( location != null ){
                currentLocation = location;timestamp=System.currentTimeMillis();
                latitude=location.getLatitude();longitude=location.getLongitude();accuracy=location.getAccuracy();
            }

            locationQueue.addLocaton(latitude,longitude,accuracy,timestamp);

            checkSocketConnection();
            if( dispatcher.getState().equals("connected")==true ) {
                WebSocketOperations.websocketTask(context, "Send_Location", dispatcher, getResources().getString(R.string.setLocationEvent), locationQueue.toMap(), LOCATION_UPDATER_RECEIVER_NAME);
            }
            else{
                Log.d("42Trips","[CONNECTION LOST]   Locations Saved = "+locationQueue.locations.size());
            }

            if (locationManager != null) {
                isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            }else{  locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE); }

            if ( isGpsEnabled==false ){
                Intent i = new Intent(getResources().getString(R.string.HOME_RECEIVER_NAME)).putExtra("message","Show_GPS_Alert");
                context.sendBroadcast(i);
            }

            Intent i = new Intent(getResources().getString(R.string.HOME_RECEIVER_NAME))
                    .putExtra("message", "Show_Current_Location")
                    .putExtra("currentLocation", currentLocation);
            context.sendBroadcast(i);

            uHandler.postDelayed(updateLocation, 5000);
        }
    };

    public void checkSocketConnection(){

        if( dispatcher.getState().equals("connected")==false ){
            Log.d("42Trips","[.Reconnecting to WebSocket.]");
            try {
                dispatcher = new WebSocketRailsDispatcher(new URL(getResources().getString(R.string.socket_address)));
                dispatcher.connect();
            }catch (Exception e){   e.printStackTrace();    }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("42Trips", "[Location Updater] > Started");
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("42Trips", "[Location Updater] > Stoped");

        uHandler.removeCallbacksAndMessages(null);// Stoping Location Service
        try {    this.unregisterReceiver(lReceiver); }catch (Exception e){e.printStackTrace();}
        fusedLocationService.stop();
    }
}
