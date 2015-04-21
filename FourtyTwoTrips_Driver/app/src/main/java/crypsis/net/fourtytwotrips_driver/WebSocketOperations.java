package crypsis.net.fourtytwotrips_driver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import br.net.bmobile.websocketrails.WebSocketRailsDataCallback;
import br.net.bmobile.websocketrails.WebSocketRailsDispatcher;

public class WebSocketOperations {

    public static void websocketTask(final Context context,final String operation, final WebSocketRailsDispatcher socketDispatcher,final String socketEventName,final Map<String, Object> requestMap, final  String receiverName){

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    WebSocketRailsDataCallback success = new WebSocketRailsDataCallback() {
                        @Override
                        public void onDataAvailable(Object data) {
                            Log.d("websocket", " ["+operation+"_Success] :  , Msgg : " + data);
                            LinkedHashMap<String,Object> responseMap = null;
                            if ( operation.equalsIgnoreCase("Send_Location")==true ){   responseMap=(LinkedHashMap<String,Object>) data; }
                            Intent i = new Intent(receiverName)
                                    .putExtra("message", operation+"_Success")
                                    .putExtra("responseMap", responseMap);
                            context.sendBroadcast(i);
                        }
                    };

                    WebSocketRailsDataCallback failure = new WebSocketRailsDataCallback() {
                        @Override
                        public void onDataAvailable(Object data) {
                            Log.d("websocket", " ["+operation+"_Failure] :  , Msg : " + data);
                            LinkedHashMap<String,Object> responseMap= (LinkedHashMap<String,Object>) data;
                            Intent i = new Intent(receiverName)
                                    .putExtra("message", operation+"_Failure")
                                    .putExtra("responseMap", responseMap);
                            context.sendBroadcast(i);
                        }
                    };
                    socketDispatcher.trigger(socketEventName, requestMap, success, failure);

                } catch (Exception e) {
                    Log.d("websocket", " ["+operation+"_Error] :");
                    LinkedHashMap<String,Object> responseMap = new LinkedHashMap();
                    responseMap.put("message", "Error");
                    Intent i = new Intent(receiverName)
                            .putExtra("message", operation+"_Error")
                            .putExtra("responseMap", responseMap);
                    context.sendBroadcast(i);
                }
                return null;
            }
        }.execute();
    }

}
