package crypsis.net.fourtytwotrips_driver;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GetSearchPlaces {

    public GetSearchPlacesCallback callback;

    public interface GetSearchPlacesCallback {
        void onStart();
        void onComplete(boolean status, String requestedSearchName, ArrayList<Map<String, Object>> searchPlacesDetails, ArrayList<String> searchPlacesNames);
    }

    public GetSearchPlaces(final String searchName, GetSearchPlacesCallback call_back){

        callback = call_back;

        new AsyncTask<Void, Void, Void>() {

            String response_string = "";
            int status_code;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                callback.onStart();
                Log.d("searchHelp", "sEARCHING for : " +searchName);
            }

            @Override
            protected Void doInBackground(Void... params) {

                String link="http://maps.googleapis.com/maps/api/geocode/json?address=";
                try { link = link + URLEncoder.encode(searchName, "utf-8");} catch (Exception e) { e.printStackTrace();    }

                DefaultHttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost(link);
                HttpResponse response=null;

                try {
                    post.setHeader("Accept", "application/json");
                    post.setHeader("Content-Type", "application/json");

                    response = client.execute(post);
                    status_code = response.getStatusLine().getStatusCode();
                    response_string = EntityUtils.toString(response.getEntity());

                }catch (Exception e){ e.printStackTrace();}

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                if (status_code == 200) {
                    //Log.d("http", " [SEARCH_PLACE_SUCCESS] :  , Predictions  : " + response_string);

                    ArrayList<Map<String,Object>> searchPlacesDetails = new ArrayList<Map<String,Object>>();  // ArrayList To Hold Search Places Details..
                    ArrayList<String> searchPlacesNames = new ArrayList<String>();  // ArrayList To Hold Search Places Details..

                    try{
                        JSONObject responseJSON = new JSONObject(response_string);
                        Log.d("http", " [SEARCH_PLACE_SUCCESS] :  , Predictions  : " + responseJSON.toString());
                        JSONArray results = responseJSON.getJSONArray("results");    // Getting Routes Array

                        for(int i=0;i<results.length();i++){
                            JSONObject placeDetails = results.getJSONObject(i);                 // Get Individual Place..
                            String placeName = (String) placeDetails.get("formatted_address");
                            JSONObject location = placeDetails.getJSONObject("geometry").getJSONObject("location");
                            LatLng latLng = new LatLng(Double.parseDouble(location.getString("lat")), Double.parseDouble(location.getString("lng")));

                            Map<String, Object> searchPlace = new HashMap<String, Object>();
                            searchPlace.put("placeName", placeName);
                            searchPlace.put("placeCoordinates", latLng);
                            searchPlacesDetails.add(searchPlace);

                            searchPlacesNames.add(placeName);
                        }

                    }catch (Exception e){ e.printStackTrace(); }

                    callback.onComplete(true, searchName, searchPlacesDetails, searchPlacesNames);

                } else if (status_code == 401) {
                    Log.d("http", " [SEARCH_PLACE FAILURE] :  Msg : " + response_string);
                    callback.onComplete(false, searchName, null, null);
                } else if (status_code == 500) {
                    Log.d("http", " [SEARCH_PLACE ERROR] :  , Msg : " + response_string);
                    callback.onComplete(false, searchName, null, null);
                }
            }

        }.execute();

    }
}