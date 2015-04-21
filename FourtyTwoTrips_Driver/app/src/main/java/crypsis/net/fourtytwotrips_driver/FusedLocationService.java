package crypsis.net.fourtytwotrips_driver;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class FusedLocationService implements LocationListener,GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener {

    Context context;

    boolean listenLocationUpdates = false;

    private static final long INTERVAL = 1000 * 5;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    private static final long ONE_MIN = 1000 * 60;
    private static final float MINIMUM_ACCURACY = 60.0f;

    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;

    private Location location;

    public FusedLocationService(Context context, boolean listenLocationUpdates) {

        this.context = context;
        this.listenLocationUpdates = listenLocationUpdates;

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("fusedlcoation", "====> onConnected : FusedLocationService");

        fusedLocationProviderApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        location = fusedLocationProviderApi.getLastLocation(googleApiClient);
    }

    @Override
    public void onLocationChanged(Location location) {

        if ( location != null && location.getAccuracy() > 0 ) {
            if( this.location == null || location.getTime() - this.location.getTime() > ONE_MIN || location.getAccuracy() < MINIMUM_ACCURACY ){
                Log.d("fusedlcoation", "**** Location Changed with required Accuracy (or) Most Recent Location ****");
                this.location = location;

                if ( listenLocationUpdates == false ) {
                    stop();
                }
            }
        }
    }

    public Location getLocation() {
        Log.d("fusedlcoation", "====> Tracked Location : "+location);
        return this.location;
    }

    public void stop(){
        fusedLocationProviderApi.removeLocationUpdates(googleApiClient, this);
        Log.d("fusedlcoation", "====> Closed Location Listener");
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}
}