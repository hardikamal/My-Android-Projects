package crypsis.net.fourtytwotrips_customer;

import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Context;
import android.content.Intent;

import com.facebook.Session;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;


public class HomeActivity extends ActionBarActivity{

    Context context;
    SharedPreferencesData sharedPreferencesData;

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerListener;
    String []listItems;

    GoogleMap map;
    LinearLayout searchLocation;
    TextView pickupLocation;

    LatLng currentLocation;

    LatLng selectedLocation;
    String selectedLocation_Name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);
        context=this;

        sharedPreferencesData = new SharedPreferencesData(context);

        searchLocation = (LinearLayout) findViewById(R.id.searchLocation);
        pickupLocation = (TextView) findViewById(R.id.pickupLocation);

        searchLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent searchActivity=new Intent(context, SearchActivity.class);
                searchActivity.putExtra("nearLocation", currentLocation);
                startActivityForResult(searchActivity, 1);
            }
        });

        // Navigation Drawer..

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        listItems = getResources().getStringArray(R.array.listItems);
        drawerList = (ListView) findViewById(R.id.drawerList);

        drawerList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems));

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

                if ( listItems[position].equalsIgnoreCase("PAYMENT") ){
                    Intent selectPaymentActivity=new Intent(context, SelectPaymentTypeActivity.class);
                    startActivity(selectPaymentActivity);
                }
                if ( listItems[position].equalsIgnoreCase("LogOut") ){
                    sharedPreferencesData.clear();

                    Session session = Session.getActiveSession();
                    if (session != null) {
                        if (!session.isClosed()) {
                            session.closeAndClearTokenInformation();
                        }
                    }else {
                        session = new Session(context);
                        Session.setActiveSession(session);
                        session.closeAndClearTokenInformation();
                    }

                    Intent dispatcherActivity=new Intent(context, DispatcherActivity.class);
                    dispatcherActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    dispatcherActivity.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(dispatcherActivity);
                    finish();;
                }
            }
        });


        // Google Map ..

        CustomMapFragment customMapFragment = ((CustomMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        map = customMapFragment.getMap();
        map.setMyLocationEnabled(true);

        customMapFragment.setOnDragListener(new MapWrapperLayout.OnDragListener() {
            @Override
            public void onDrag(MotionEvent motionEvent) {
                if ( motionEvent.getAction() == MotionEvent.ACTION_MOVE ){
                    pickupLocation.setText("waiting for completion...");
                }
                else {
                    selectedLocation = map.getCameraPosition().target;
                    selectedLocation_Name = "Lat: " + selectedLocation.latitude + ", Lng: " + selectedLocation.longitude;
                    getPlaceName(selectedLocation);
                }
            }
        });

        // Current Location ..

        new GetCurrentLocation(context,new GetCurrentLocation.CurrentLocationCallback() {
            @Override
            public void onComplete(Location location) {
                if (location != null ) {
                    currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    map.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
                    map.animateCamera(CameraUpdateFactory.zoomTo(15));

                    getPlaceName(currentLocation);
                }
            }
        });

        //....

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ( requestCode == 1 ){
            try {
                LatLng placeLatLng = data.getParcelableExtra("placeLatLng");
                String placeName = data.getStringExtra("placeName");
                if ( placeLatLng != null && placeName != null) {
                    selectedLocation = placeLatLng;
                    selectedLocation_Name = placeName;

                    pickupLocation.setText(selectedLocation_Name);

                    map.moveCamera(CameraUpdateFactory.newLatLng(selectedLocation));
                    map.animateCamera(CameraUpdateFactory.zoomTo(15));

                }
            }catch (Exception e){e.printStackTrace();}
        }
    }

    public void getPlaceName(LatLng location){
        new GetPlaceName(location, new GetPlaceName.GetPlaceNameCallback() {
            @Override
            public void onStart() {
                pickupLocation.setText("Fetching Pickup place, wait..");
            }

            @Override
            public void onComplete(boolean result, LatLng location, String placeName) {
                if ( result == true ) {
                    pickupLocation.setText(placeName);
                }else{
                    pickupLocation.setText("Sorry, Tryagain..");
                }
            }
        });
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