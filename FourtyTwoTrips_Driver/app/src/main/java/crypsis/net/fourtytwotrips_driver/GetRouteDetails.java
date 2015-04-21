package crypsis.net.fourtytwotrips_driver;


import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetRouteDetails {

    public GetRouteDetailsCallback callback;

    public interface GetRouteDetailsCallback {
        void onComplete(boolean status, Map<String, Object> pathDetails);
    }

    public GetRouteDetails(final LatLng from, final LatLng destination, GetRouteDetailsCallback call_back){

        callback = call_back;

        new AsyncTask<Void, Void, Void>() {

            String response_string="";
            JSONObject response_json = new JSONObject();
            int status_code;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Log.d("http", "I'm Called "+from+"   ,   "+destination);
            }

            @Override
            protected Void doInBackground(Void... params) {
                String link = "https://maps.googleapis.com/maps/api/directions/json?origin="+from.latitude+","+from.longitude+"&destination="+destination.latitude+","+destination.longitude;

                DefaultHttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost(link);
                HttpResponse response=null;

                try {
                    post.setHeader("Accept", "application/json");
                    post.setHeader("Content-Type", "application/json");

                    response = client.execute(post);
                    status_code = response.getStatusLine().getStatusCode();
                    response_string = EntityUtils.toString(response.getEntity());
                    response_json = new JSONObject(response_string);

                }catch (Exception e){ e.printStackTrace();}

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                Log.d("http", "Response   :  "+response_string);

                if (status_code == 200){
                    try{
                        JSONArray routes = response_json.getJSONArray("routes");    // Getting Routes Array
                        JSONObject array = routes.getJSONObject(0);
                        JSONArray legs = array.getJSONArray("legs");
                        JSONObject larray = legs.getJSONObject(0);  // Legs Array Containing Steps, Start , End , Total Distance,PolyLines..

                        String start = (String) larray.get("start_address");    // Start Point Name
                        String end = (String) larray.get("end_address");        // Destination Name..

                        JSONObject t_distance = larray.getJSONObject("distance");
                        String total_distance = (String) t_distance.get("text");    // Total Distance..

                        ArrayList<Map<String,Object>> path_steps = new ArrayList<Map<String,Object>>();  // ArrayList To Hold Steps Details..

                        JSONArray steps = larray.getJSONArray("steps");

                        for(int i=0;i<steps.length();i++){
                            JSONObject step = steps.getJSONObject(i);
                            JSONObject distance = step.getJSONObject("distance");
                            String dis = (String) distance.get("text");   // Step Total Distance..

                            JSONObject statLocation = step.getJSONObject("start_location");
                            LatLng startLoc = new LatLng(statLocation.getDouble("lat"), statLocation.getDouble("lng"));

                            JSONObject endLocation = step.getJSONObject("end_location");
                            LatLng endLoc = new LatLng(endLocation.getDouble("lat"), endLocation.getDouble("lng"));

                            /*JSONObject step_polylines = step.getJSONObject("polyline");
                            String step_points = (String) step_polylines.get("points");     // Encoded Step PolyLines..
                            List<LatLng> l_points=decodePoly(step_points);      // Decoded Step Points in (Lat,Lng)..
                            */

                            Map<String, Object> step_details = new HashMap();   // Map To Hold Step Details..
                            step_details.put("distance", dis);
                            step_details.put("statLocation", startLoc);
                            step_details.put("endLocation", endLoc);
                            //step_details.put("step_points", l_points);

                            path_steps.add(step_details);   // Adding to ArrayList of Steps..
                        }

                        JSONObject polylines = array.getJSONObject("overview_polyline");
                        String points = (String) polylines.get("points");     // Encoded Total Path PolyLines..
                        ArrayList<LatLng> travellingPoints = decodePoly(points);          // Decoded Total Path Points in (Lat,Lng)..


                        // PATH DETAILS MAP [ ]..

                        Map<String, Object> path_details = new HashMap();
                        path_details.put("start", start);
                        path_details.put("end", end);
                        path_details.put("total_distance", total_distance);
                        path_details.put("path_steps", path_steps);
                        path_details.put("travellingPoints", travellingPoints);

                        callback.onComplete(true, path_details);        // Sending Path Details to CallBack..

                        Log.d("http", " [PATH_PREDICT_SUCCESS] :  , PATH : "+path_details);

                    }catch (Exception e){
                        Log.d("http", " [PATH_PREDICT_EXCEPTION] :  "+e.toString());
                        callback.onComplete(false, null);
                    }


                }
                else if (status_code == 401){
                    Log.d("http", " [GEOCODE FAILURE] :  Msg : " + response_string);
                    callback.onComplete(false, null);
                }

                else if (status_code == 500){
                    Log.d("http", " [GEOCODE ERROR] :  , Msg : " + response_string);
                    callback.onComplete(false, null);
                }

            }

        }.execute();

    }

    private ArrayList<LatLng> decodePoly(String encoded) {

        ArrayList<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        try {
            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((double) lat / 1E5, (double) lng / 1E5);
                poly.add(p);

                Log.d("pCheck", "[pCheck] " + p);
            }
        }catch (Exception e){}
        return poly;
    }
}