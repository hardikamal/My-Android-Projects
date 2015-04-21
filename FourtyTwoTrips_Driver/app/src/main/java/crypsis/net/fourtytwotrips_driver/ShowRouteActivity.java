package crypsis.net.fourtytwotrips_driver;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class ShowRouteActivity extends ActionBarActivity {

    Context context;

    GoogleMap map;
    ProgressBar searchingRouteIndicator;

    CustomAutoCompleteTextView fromPlaceSearch;
    CustomListAdapter fromPlaceAdapter;
    ProgressBar fromLoadingIndicator;

    CustomAutoCompleteTextView destinationPlaceSearch;
    CustomListAdapter destinationPlaceAdapter;
    ProgressBar destinationLoadingIndicator;

    Map<String, Object> fromPlace = new HashMap<>();
    Map<String, Object> destinationPlace = new HashMap<>();

    String searchName;

    boolean goToHome = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_route_layout);
        context = this;

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.rgb(250,150,56)));

        map=((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
        //map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setMyLocationEnabled(true);

        searchingRouteIndicator = (ProgressBar) findViewById(R.id.searchingRouteIndicator);

        fromPlaceSearch = (CustomAutoCompleteTextView) findViewById(R.id.fromPlaceSearch);
        fromLoadingIndicator = (ProgressBar) findViewById(R.id.fromLoadingIndicator);
        fromPlaceSearch.setThreshold(1);
        fromPlaceAdapter = new CustomListAdapter(context,R.layout.custom_list_item, new ArrayList<Map<String, Object>>());
        fromPlaceSearch.setAdapter(fromPlaceAdapter);

        fromPlaceSearch.setTextChangeListener(new CustomAutoCompleteTextView.CustomOnTextChangeCallback() {
            @Override
            public void onTextChangeFinish(String text) {
                if ( text != null && text.length() >1 ){
                    searchName = text;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new GetSearchPlaces(searchName, new GetSearchPlaces.GetSearchPlacesCallback() {
                                public void onStart() { fromLoadingIndicator.setVisibility(View.VISIBLE);    }

                                public void onComplete(boolean status, String requestedSearchName, ArrayList<Map<String, Object>> searchPlacesDetails, ArrayList<String> searchPlacesNames) {
                                    if (searchName != null && requestedSearchName != null && searchName.equalsIgnoreCase(requestedSearchName)) {
                                        fromLoadingIndicator.setVisibility(View.GONE);
                                        if (fromPlaceAdapter != null) {  fromPlaceAdapter.clear();    }
                                        fromPlaceAdapter = new CustomListAdapter(context, R.layout.custom_list_item, searchPlacesDetails);
                                        fromPlaceSearch.setAdapter(fromPlaceAdapter);
                                        fromPlaceAdapter.notifyDataSetChanged();
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

        fromPlaceSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(fromPlaceSearch.getWindowToken(), 0);    // Hides Key Board After Item Select..

                fromPlace = fromPlaceAdapter.getItem(position);
                fromPlaceSearch.changeText(fromPlace.get("placeName").toString());
                if ( destinationPlaceSearch.getText().toString().length() > 0){
                    findRoute(fromPlace, destinationPlace);
                }
            }
        });


        Intent startPointIntent = getIntent();    // Intent with StartPoint...

        if( startPointIntent.getBooleanExtra("hasStartPoint", false) == true ){
            try {
                LatLng startPoint = startPointIntent.getParcelableExtra("StartPoint");
                fromPlace.put("placeCoordinates", startPoint);
                fromPlaceSearch.setEnabled(false);
                fromPlaceSearch.changeText("Your Current Place");
                goToHome = true;
            }catch (Exception e){e.printStackTrace();}
        }

        destinationPlaceSearch = (CustomAutoCompleteTextView) findViewById(R.id.destinationPlaceSearch);
        destinationLoadingIndicator = (ProgressBar) findViewById(R.id.destinationLoadingIndicator);
        destinationPlaceSearch.setThreshold(1);//will start working from first character
        destinationPlaceAdapter = new CustomListAdapter(context,R.layout.custom_list_item, new ArrayList<Map<String, Object>>());
        destinationPlaceSearch.setAdapter(destinationPlaceAdapter);

        destinationPlaceSearch.setTextChangeListener(new CustomAutoCompleteTextView.CustomOnTextChangeCallback() {
            @Override
            public void onTextChangeFinish(String text) {
                if ( text != null && text.length() >1 ){
                    searchName = text;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new GetSearchPlaces(searchName, new GetSearchPlaces.GetSearchPlacesCallback() {
                                public void onStart() { destinationLoadingIndicator.setVisibility(View.VISIBLE);    }

                                public void onComplete(boolean status, String requestedSearchName, ArrayList<Map<String, Object>> searchPlacesDetails, ArrayList<String> searchPlacesNames) {
                                    if (searchName != null && requestedSearchName != null && searchName.equalsIgnoreCase(requestedSearchName)) {
                                        destinationLoadingIndicator.setVisibility(View.GONE);
                                        if (destinationPlaceAdapter != null) {  destinationPlaceAdapter.clear();    }
                                        destinationPlaceAdapter = new CustomListAdapter(context, R.layout.custom_list_item, searchPlacesDetails);
                                        destinationPlaceSearch.setAdapter(destinationPlaceAdapter);
                                        destinationPlaceAdapter.notifyDataSetChanged();
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

        destinationPlaceSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(destinationPlaceSearch.getWindowToken(), 0);    // Hides Key Board After Item Select..

                destinationPlace = destinationPlaceAdapter.getItem(position);
                destinationPlaceSearch.changeText(destinationPlace.get("placeName").toString());

                if (fromPlaceSearch.getText().toString().length() > 0) {
                    findRoute(fromPlace, destinationPlace);
                }
            }
        });

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                if ( marker.getTitle().toString().equalsIgnoreCase("NA") ){
                    new GetPlaceName(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude), new GetPlaceName.GetPlaceNameCallback() {
                        @Override
                        public void onStart() { marker.setTitle("Fetching Place Name, wait..");  }

                        @Override
                        public void onComplete(boolean status, LatLng location, String placeName) {
                            marker.setTitle(placeName);
                            marker.setVisible(false);marker.setVisible(true);
                            marker.showInfoWindow();
                        }
                    });

                }
                return false;
            }
        });

    }

    // Method : Find Route..

    public void  findRoute(Map<String, Object> fromPlace, Map<String, Object> destinationPlace){

        if ( fromPlace != null && destinationPlace != null ){
            LatLng fromLatLng = null, destinationLatLng = null;
            try {
                fromLatLng = (LatLng) fromPlace.get("placeCoordinates");
                destinationLatLng = (LatLng) destinationPlace.get("placeCoordinates");
            } catch (Exception e) { e.printStackTrace(); }

            if ( fromLatLng != null && destinationLatLng != null && fromLatLng.latitude != destinationLatLng.latitude && fromLatLng.longitude != destinationLatLng.longitude ){
                searchingRouteIndicator.setVisibility(View.VISIBLE);

                new GetRouteDetails(fromLatLng, destinationLatLng, new GetRouteDetails.GetRouteDetailsCallback() {
                    @Override
                    public void onComplete(boolean status, Map<String, Object> pathDetails) {
                        searchingRouteIndicator.setVisibility(View.GONE);
                        map.clear();        // Clearing the Map..

                        try {
                            ArrayList<Map<String, Object>> pathSteps = (ArrayList<Map<String, Object>>) pathDetails.get("path_steps");
                            ArrayList<LatLng> travellingPoints = (ArrayList<LatLng>) pathDetails.get("travellingPoints");

                            if ( goToHome == false ) {
                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                LatLng endLocation = null;
                                for (int i = 0; i < pathSteps.size(); i++) {
                                    Map<String, Object> stepDetails = pathSteps.get(i);
                                    String stepDistance = (String) stepDetails.get("distance");
                                    LatLng statLocation = (LatLng) stepDetails.get("statLocation");
                                    endLocation = (LatLng) stepDetails.get("endLocation");
                                    builder.include(statLocation);

                                    float marker = BitmapDescriptorFactory.HUE_VIOLET;
                                    if (i == 0) {
                                        marker = BitmapDescriptorFactory.HUE_GREEN;
                                    }
                                    map.addMarker(new MarkerOptions().position(statLocation).title("NA").snippet("(Lat: " + statLocation.latitude + "  , Lng: " + statLocation.longitude + ")").icon(BitmapDescriptorFactory.defaultMarker(marker)));
                                }

                                if (endLocation != null) {
                                    map.addMarker(new MarkerOptions().position(endLocation).title("NA").snippet("(Lat: " + endLocation.latitude + "  , Lng: " + endLocation.longitude + ")").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                                    builder.include(endLocation);
                                }

                                for (int j = 0; j < travellingPoints.size() - 1; j++) {
                                    map.addPolyline(new PolylineOptions().add(travellingPoints.get(j), travellingPoints.get(j + 1)).width(7).color(Color.BLUE).geodesic(true));
                                }

                                if (builder != null) {
                                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 27));    // Moveing Camera to Path..
                                }
                            }
                            else{   // If Show Home..
                                Intent i = new Intent(getResources().getString(R.string.HOME_RECEIVER_NAME))
                                        .putExtra("message", "ShowRoute")
                                        .putExtra("travellingPoints", travellingPoints);
                                context.sendBroadcast(i);
                                finish();
                            }

                        }catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(context, "Unable to Find Path, TryAgain..", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
            else{
                Toast.makeText(context, "From & Destination should be Different", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        onBackPressed();
        return true;
    }


}
