package crypsis.net.fourtytwotrips_driver;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import br.net.bmobile.websocketrails.WebSocketRailsDispatcher;


public class HomeActivity extends ActionBarActivity implements View.OnClickListener{

    Context context;
    String HOME_RECEIVER_NAME;
    BroadcastReceiver hReceiver;

    LocalStorageData localStorageData;
    String userMobile="",userToken="", userName="";
    Map<String, Object> authenticationData; //Authentication Map

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerListener;
    String []drawerListItems;

    GoogleMap map;
    Button startTrip,endTrip;
    ProgressBar startTripLoadingIndicator, endTripLoadingIndicator;
    TextView profileName;
    TextView currentPlaceLatLng,currentPlaceName;
    LinearLayout showCurrentLocation, showTripStartPoint;
    TextView tripStartPoint;

    Marker clMarker;
    Location currentLocation = new Location("curLoc");
    Location lastLocation = new Location("lastLoc");

    Intent locationUpdater;
    WebSocketRailsDispatcher dispatcher;    // Socket Dispatcher ..

    boolean drawRoute = false;

    boolean showingAlertDialog; // To Prevent showing multiple dialog pop ups.
    boolean animateCamera=true;

    boolean onDrag=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);
        context = this;

        //getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.rgb(250,150,56)));

        HOME_RECEIVER_NAME = getResources().getString(R.string.HOME_RECEIVER_NAME);

        localStorageData = new LocalStorageData(context);
        userMobile = localStorageData.getMobile();
        userName = localStorageData.getName();
        userToken = localStorageData.getToken();

        // Authentication Map..

        Map<String, Object> auth_data = new HashMap<String, Object>();
        auth_data.put("mobile", userMobile);
        auth_data.put("token", userToken);
        authenticationData = new HashMap<String, Object>();
        authenticationData.put("auth", auth_data);

        // ..

        profileName = (TextView) findViewById(R.id.profileName);
        currentPlaceLatLng = (TextView) findViewById(R.id.currentPlaceLatLng);
        currentPlaceName = (TextView) findViewById(R.id.currentPlaceName);

        showCurrentLocation = (LinearLayout) findViewById(R.id.showCurrentLocation);
        showCurrentLocation.setOnClickListener(this);

        showTripStartPoint = (LinearLayout) findViewById(R.id.showTripStartPoint);
        tripStartPoint = (TextView) findViewById(R.id.tripStartPoint);

        startTrip = (Button) findViewById(R.id.startTrip);
        startTripLoadingIndicator = (ProgressBar) findViewById(R.id.startTripLoadingIndicator);
        startTrip.setOnClickListener(this);

        endTrip = (Button) findViewById(R.id.endTrip);
        endTripLoadingIndicator = (ProgressBar) findViewById(R.id.endTripLoadingIndicator);
        endTrip.setOnClickListener(this);

        CustomMapFragment customMapFragment = ((CustomMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        map = customMapFragment.getMap();
        map.animateCamera(CameraUpdateFactory.zoomTo(16));  // Zooming Map on StartUp..
        map.getUiSettings().setZoomControlsEnabled(true);
        //map.setMyLocationEnabled(true);
        //map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        //map.getUiSettings().setCompassEnabled(true);
        //map.setPadding(0, 140, 10, 0);

        customMapFragment.setOnDragListener(new MapWrapperLayout.OnDragListener() {
            @Override
            public void onDrag(MotionEvent motionEvent) {
                if ( motionEvent.getAction() == MotionEvent.ACTION_MOVE ){  onDrag = true;  }
                else {  onDrag = false; }
            }
        });

        // Navigation Drawer..

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerListItems = getResources().getStringArray(R.array.drawerListItems);
        drawerList = (ListView) findViewById(R.id.drawerList);

        drawerList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, drawerListItems));

        drawerListener = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close){//, R.drawable.ic_drawer
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };

        drawerLayout.setDrawerListener(drawerListener);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                drawerLayout.closeDrawer(drawerList);
                if ( drawerListItems[position].equalsIgnoreCase("Logout") ) {
                    HttpOperations.httpTask(context, "Logout", getResources().getString(R.string.api_address), userMobile, userToken, HOME_RECEIVER_NAME);
                }
                if ( drawerListItems[position].equalsIgnoreCase("Find Route") ) {
                    Intent showRouteAct = new Intent(context, ShowRouteActivity.class);
                    startActivity(showRouteAct);
                }
            }
        });

        // Update UI (profileName , Trip Status) ...

        profileName.setText(userName);

        Intent tripItent = getIntent();    // Intent with TripStatus from DispatcherActivity..

        if( tripItent.getBooleanExtra("onTrip", false) == true ){   // If ON_TRIP..showCurrentLocation

            drawRoute = true;

            startTrip.setVisibility(View.GONE);startTripLoadingIndicator.setVisibility(View.GONE);
            endTrip.setVisibility(View.VISIBLE);endTripLoadingIndicator.setVisibility(View.GONE);
            showTripStartPoint.setVisibility(View.VISIBLE);//tripStartPoint.setText("Fetching Place Name..");

            ArrayList<LatLng> travelledLocations = new ArrayList<LatLng>();
            LatLng startPoint = new LatLng(0,0);

            try{
                travelledLocations = tripItent.getParcelableArrayListExtra("travelledLocations");
                if ( travelledLocations != null && travelledLocations.size() > 0 ){
                    startPoint = travelledLocations.get(0);
                    lastLocation.setLatitude(travelledLocations.get(travelledLocations.size()-1).latitude);
                    lastLocation.setLongitude(travelledLocations.get(travelledLocations.size()-1).longitude);

                    for(int i=0;i<travelledLocations.size()-1;i++){
                        map.addPolyline(new PolylineOptions().add(travelledLocations.get(i), travelledLocations.get(i+1)).width(6).color(Color.MAGENTA).geodesic(true));
                    }
                }
            }catch (Exception e){ e.printStackTrace(); }

            new GetPlaceName(startPoint, new GetPlaceName.GetPlaceNameCallback() {
                @Override
                public void onStart() { tripStartPoint.setText("Fetching Place Name..");  }
                @Override
                public void onComplete(boolean status, LatLng location, String placeName) {
                    tripStartPoint.setText(placeName);
                }
            });

            showGPSAlertDialog("onTripRoute");

        }else{                                                      // If Not_On_Trip ..
            startTrip.setVisibility(View.VISIBLE);
            endTrip.setVisibility(View.GONE);
        }

        // Initializing WebSocket Dispatcher ..

        try {
            dispatcher = new WebSocketRailsDispatcher(new URL(getResources().getString(R.string.socket_address)));
            dispatcher.connect();
        }catch (Exception e){   e.printStackTrace(); }


        // Starting Location Updater ...

        lastLocation.setLatitude(0.0);lastLocation.setLongitude(0.0);

        locationUpdater = new Intent(context, LocationUpdater.class);
        context.startService(locationUpdater);//Starting Location Service


        // BroadCast Receiver ...

        IntentFilter intentFilter = new IntentFilter(HOME_RECEIVER_NAME);
        hReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.d("hrcvr", intent.getStringExtra("message"));
                try {
                    String message = intent.getStringExtra("message");

                    if( message.equalsIgnoreCase("Show_Current_Location") ) {
                        currentLocation = intent.getParcelableExtra("currentLocation");
                        showCurrentLocation(animateCamera);
                    }

                    if( message.equalsIgnoreCase("Show_GPS_Alert") ) {
                        showGPSAlertDialog("GPS");
                    }

                    if( message.equalsIgnoreCase("Token_Expired") ) {
                        Toast.makeText(context,"Your Token Expired , Plz. Login to Continue..",Toast.LENGTH_LONG).show();
                        continueLogout();
                    }

                    if( message.equalsIgnoreCase("StartTrip_Success") ) {

                        Toast.makeText(context, "Trip Started Successfully..",Toast.LENGTH_SHORT).show();

                        startTrip.setVisibility(View.GONE);startTripLoadingIndicator.setVisibility(View.GONE);
                        endTrip.setVisibility(View.VISIBLE);endTrip.setEnabled(true);endTripLoadingIndicator.setVisibility(View.GONE);  // Showing EndTrip Button..
                        showTripStartPoint.setVisibility(View.VISIBLE);tripStartPoint.setText(currentPlaceName.getText().toString());
                        drawRoute = true;map.clear();showCurrentLocation(animateCamera);

                        showGPSAlertDialog("TripRoute");
                    }

                    if( message.equalsIgnoreCase("StartTrip_Failure") || message.equalsIgnoreCase("StartTrip_Error") ) {
                        Map<String, Object> responseMap = (Map<String, Object>) intent.getSerializableExtra("responseMap");

                        if ( responseMap.get("message").toString().equalsIgnoreCase("Trip is already started") ){

                            Toast.makeText(context, "WARNING : Trip is Started Already , Continue with it (or) End It to start the New Trip..", Toast.LENGTH_SHORT).show();

                            startTrip.setVisibility(View.GONE);startTripLoadingIndicator.setVisibility(View.GONE); // Hiding StartTrip Button..
                            endTrip.setVisibility(View.VISIBLE);endTrip.setEnabled(true);endTripLoadingIndicator.setVisibility(View.GONE);  // Showing EndTrip Button..
                            showTripStartPoint.setVisibility(View.VISIBLE);tripStartPoint.setText("NA");
                            drawRoute = true;map.clear();showCurrentLocation(animateCamera);

                            showGPSAlertDialog("TripRoute");
                        }

                        else if ( responseMap.get("message").toString().equalsIgnoreCase("Authentication Failed") ){
                            Toast.makeText(context,"Your Token Expired , Plz. Login to Continue...",Toast.LENGTH_LONG).show();
                            continueLogout();
                        }
                        else{
                            startTrip.setEnabled(true);startTripLoadingIndicator.setVisibility(View.GONE);
                            Toast.makeText(context, "Error in Starting Trip , TryAgain", Toast.LENGTH_SHORT).show();
                        }
                    }

                    if( message.equalsIgnoreCase("EndTrip_Success") ) {

                        Toast.makeText(context, "Ended Current Trip Successfully..",Toast.LENGTH_SHORT).show();

                        endTrip.setVisibility(View.GONE);endTripLoadingIndicator.setVisibility(View.GONE);  // Hiding EndTrip Button ..
                        startTrip.setVisibility(View.VISIBLE);startTrip.setEnabled(true);startTripLoadingIndicator.setVisibility(View.GONE);  // Showing StartTrip Button..
                        showTripStartPoint.setVisibility(View.GONE);tripStartPoint.setText("NA");
                        drawRoute = false;map.clear();showCurrentLocation(animateCamera);
                    }

                    if( message.equalsIgnoreCase("EndTrip_Failure") || message.equalsIgnoreCase("EndTrip_Error") ) {
                        Map<String, Object> responseMap = (Map<String, Object>) intent.getSerializableExtra("responseMap");

                        if ( responseMap.get("message").toString().equalsIgnoreCase("Trip is not yet started") ){

                            Toast.makeText(context, "WARNING : Trip is Not Yet Started..", Toast.LENGTH_SHORT).show();

                            endTrip.setVisibility(View.GONE); endTripLoadingIndicator.setVisibility(View.GONE);  // Hiding EndTrip Button ..
                            startTrip.setVisibility(View.VISIBLE);startTrip.setEnabled(true);startTripLoadingIndicator.setVisibility(View.GONE);  // Showing StartTrip Button..
                            showTripStartPoint.setVisibility(View.GONE);
                            drawRoute = false;map.clear();showCurrentLocation(animateCamera);
                        }

                        else if ( responseMap.get("message").toString().equalsIgnoreCase("Authentication Failed") ){
                            Toast.makeText(context,"Your Token Expired , Plz. Login to Continue..",Toast.LENGTH_LONG).show();
                            continueLogout();
                        }

                        else{
                            endTrip.setEnabled(true);endTripLoadingIndicator.setVisibility(View.GONE);
                            Toast.makeText(context, "Error in Ending Current Trip , TryAgain", Toast.LENGTH_SHORT).show();
                        }
                    }

                    if( message.equalsIgnoreCase("Logout_Success") ) {
                        Toast.makeText(context, "Logged out Successfully..",Toast.LENGTH_SHORT).show();
                        continueLogout();
                    }

                    if( message.equalsIgnoreCase("Logout_Failure")  || message.equalsIgnoreCase("Logout_Error") ) {
                        continueLogout();
                    }

                    if( message.equalsIgnoreCase("ShowRoute") ) {
                        try {
                            ArrayList<LatLng> travellingPoints = (ArrayList<LatLng>) intent.getSerializableExtra("travellingPoints");
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            for (int j = 0; j < travellingPoints.size() - 1; j++) {
                                builder.include(travellingPoints.get(j));
                                map.addPolyline(new PolylineOptions().add(travellingPoints.get(j), travellingPoints.get(j + 1)).width(7).color(Color.rgb(150, 220, 120)).geodesic(true));
                            }
                            if (builder != null) {
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 27));
                            }
                        }catch (Exception e){e.printStackTrace();}
                    }



                }catch (Exception e){}
            }
        };
        this.registerReceiver(hReceiver, intentFilter);//registering our receiver


    }

    @Override
    public void onClick(View view) {

        if ( view == showCurrentLocation ){
            map.animateCamera(CameraUpdateFactory.zoomTo(16));  // Zooming Map on StartUp..
            showCurrentLocation(true);
        }

        if ( view == startTrip ){       // START_TRIP..

            if ( currentLocation != null ) {
                startTrip.setEnabled(false);  // Disabling Click..
                startTripLoadingIndicator.setVisibility(View.VISIBLE);

                checkSocketConnection();
                WebSocketOperations.websocketTask(context, "StartTrip", dispatcher, getResources().getString(R.string.startTripEvent), authenticationData, HOME_RECEIVER_NAME);

                new CountDownTimer(40000, 1000) {
                    public void onTick(long millisUntilFinished) {
                    }

                    public void onFinish() {
                        if (startTrip.getVisibility() == View.VISIBLE) {
                            startTrip.setEnabled(true);    // Enabling Click..
                            startTripLoadingIndicator.setVisibility(View.GONE);
                            Toast.makeText(context, "Error in Starting Trip , TryAgain", Toast.LENGTH_SHORT).show();
                        }
                    }
                }.start();
            }else{
                Toast.makeText(context, "Failure: Your Current Location Not Fetched , Plz. TryAgain after some time..", Toast.LENGTH_SHORT).show();
            }
        }//st

        if ( view == endTrip ){    // END_TRIP..

            if ( currentLocation != null ){
                endTrip.setEnabled(false);  // Disabling Click..
                endTripLoadingIndicator.setVisibility(View.VISIBLE);

                checkSocketConnection();
                WebSocketOperations.websocketTask(context, "EndTrip", dispatcher, getResources().getString(R.string.endTripEvent), authenticationData, HOME_RECEIVER_NAME);

                new CountDownTimer(40000, 1000) {
                    public void onTick(long millisUntilFinished) {}
                    public void onFinish() {
                        if ( endTrip.getVisibility() == View.VISIBLE ) {
                            endTrip.setEnabled(true);    // Enabling Click..
                            endTripLoadingIndicator.setVisibility(View.GONE);
                            Toast.makeText(context, "Error in Ending Trip , TryAgain", Toast.LENGTH_SHORT).show();
                        }
                    }
                }.start();
            }else{
                Toast.makeText(context, "Failure: Your Current Location Not Fetched , Plz. TryAgain after some time..", Toast.LENGTH_SHORT).show();
            }
        }//et

    }

    // Show Current Location on Map .....

    public void showCurrentLocation(boolean animateCameraToCurrentLocation){

        if ( currentLocation != null ){
            double latitude = currentLocation.getLatitude(), longitude = currentLocation.getLongitude();

            if ( lastLocation != null && currentLocation != null ) {
                if ( lastLocation.distanceTo(currentLocation) > 15 ) {  // If Distance > 10m between successive points
                    new GetPlaceName(new LatLng(latitude, longitude), new GetPlaceName.GetPlaceNameCallback() {
                        @Override
                        public void onStart() { currentPlaceName.setText("Fetching Place Name..");  }

                        @Override
                        public void onComplete(boolean status, LatLng location, String placeName) {
                            currentPlaceName.setText(placeName);
                        }
                    });
                }
            }

            currentPlaceLatLng.setText("(Lat: " + latitude + ", Lng: " + longitude + ")");
            LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            if ( clMarker != null ){    clMarker.remove();  }
            clMarker = map.addMarker(new MarkerOptions().position(latLng).title(currentPlaceName.getText().toString()).snippet("(Lat: " + latitude + ", Lng: " + longitude + ")").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)).draggable(true));

            if( drawRoute == true ){
                if ( lastLocation.getLongitude() != 0 && lastLocation.getLatitude() != 0 && currentLocation != lastLocation ) {
                    map.addPolyline(new PolylineOptions().add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude())).width(6).color(Color.BLUE).geodesic(true));
                }
            }

            if ( onDrag == false ){ map.moveCamera(CameraUpdateFactory.newLatLng(latLng));  }
            if ( animateCameraToCurrentLocation== true ){   map.animateCamera(CameraUpdateFactory.zoomTo(16));animateCamera=false;  }
            lastLocation = currentLocation;
        }
        else{
            currentPlaceName.setText("Fetching Current Place..");
        }
    }

    // Check Socket Connection & ReConnection ....

    public void checkSocketConnection(){

        if( dispatcher.getState().equals("connected")==false ){
            Log.d("websocket", "[.Reconnecting to WebSocket..]");
            try {
                dispatcher = new WebSocketRailsDispatcher(new URL(getResources().getString(R.string.socket_address)));
                dispatcher.connect();
            }catch (Exception e){   e.printStackTrace();    }
        }
    }

    // Logout ....

    public void continueLogout(){
        localStorageData.clear();
        context.stopService(locationUpdater);  // Storpping Location Updater ..
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));// Going to LoginActivity
        finish();
    }

    // Create Navigation Drawer ..

    public void createNavigationDrawer(){

    }

    // GPS Alert Dialog..

    public void showGPSAlertDialog(final String type){

        if ( showingAlertDialog == false ) {

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setCancelable(false);

            String dialogTitle ="", dialogMessage="";
            String yesTitle = "";

            if ( type.equalsIgnoreCase("GPS") ) {
                dialogTitle = "GPS is Not Enabled !";yesTitle = "GPS Settings";
                dialogMessage = "Plz. Enable GPS for Accurate Location Tracking.. \n\nDo you want to go to settings ?";
            }
            else if ( type.equalsIgnoreCase("TripRoute") ){
                dialogTitle = "Trip Started !";yesTitle = "Yes";
                dialogMessage = "Do u want to Enter the Destination Place to Find Route / Track Travelling Path ?";
            }
            else if ( type.equalsIgnoreCase("onTripRoute") ){
                dialogTitle = "You are on Trip !";yesTitle = "Yes";
                dialogMessage = "Do u want to Enter the Destination Place to Find Route / Track Travelling Path ?";
            }

            alertDialog.setTitle(dialogTitle);
            alertDialog.setMessage(dialogMessage);

            alertDialog.setPositiveButton(yesTitle, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (type.equalsIgnoreCase("GPS") ){
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                    else if ( type.equalsIgnoreCase("TripRoute")  || type.equalsIgnoreCase("onTripRoute") ){
                        if ( currentLocation != null ) {
                            Intent showRouteAct = new Intent(context, ShowRouteActivity.class);
                            showRouteAct.putExtra("hasStartPoint", true);
                            showRouteAct.putExtra("StartPoint", new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
                            startActivity(showRouteAct);
                        }
                    }

                    showingAlertDialog = false;
                }
            });
            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    showingAlertDialog=false;
                }
            });

            alertDialog.show();
            showingAlertDialog = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{    this.unregisterReceiver(hReceiver);     }catch (Exception e){}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ( drawerListener.onOptionsItemSelected(item) ) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerListener.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        drawerListener.onConfigurationChanged(newConfig);
    }

}
