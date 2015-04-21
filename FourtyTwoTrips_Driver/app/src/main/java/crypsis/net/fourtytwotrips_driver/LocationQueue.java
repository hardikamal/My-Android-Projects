package crypsis.net.fourtytwotrips_driver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class LocationQueue {
    String mobile;
    String token;

    List<Map<String,Object>> locations = new ArrayList<Map<String,Object>>();

    public LocationQueue(String mobile, String token){
        this.mobile = mobile;
        this.token = token;
    }

    public void addLocaton(double lattitude, double longitude, double accuracy,long timestamp){

        if ( lattitude !=0 && longitude !=0 && accuracy !=0 && timestamp !=0 ) {
            Map<String, Object> loc_data = new HashMap<String, Object>();
            loc_data.put("lat", lattitude);
            loc_data.put("lng", longitude);
            loc_data.put("accuracy", accuracy);
            loc_data.put("timestamp", timestamp);

            locations.add(loc_data);
        }
    }

    public void removeLocations(int n){
        //Log.d("websocket","-----> Before LocLen="+locations.size()+" , Remove="+n);
        if ( locations.size() >= n ){
            for(int i=0;i<n;i++){
                locations.remove(0);
            }
        }
       //Log.d("websocket","-----> After CurLen="+locations.size());
    }

    public void resetLocations(){
        locations.clear();
    }

    public Map<String, Object> toMap(){

        Map<String, Object> auth_data = new HashMap<String, Object>();
        auth_data.put("mobile", mobile);
        auth_data.put("token", token);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("auth", auth_data);
        data.put("location", locations);
        return data;
    }
}