package crypsis.net.fourtytwotrips_driver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import crypsis.net.fourtytwotrips_driver.HttpDeleteWithBody;
import crypsis.net.fourtytwotrips_driver.HttpGetWithBody;

public class HttpOperations {

    public static void httpTask(final Context context, final String operation, final String apiAddress, final String userMobile, final String userPassword, final  String receiverName){

        new AsyncTask<Void, Void, Void>() {

            String response_string="";
            int status_code;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {

                DefaultHttpClient client = new DefaultHttpClient();
                JSONObject userObj = new JSONObject();
                HttpResponse response = null;

                try {
                    userObj.put("mobile", userMobile);

                    if ( operation.equalsIgnoreCase("Login") ) {
                        userObj.put("password", userPassword);

                        HttpPost post = new HttpPost(apiAddress);
                        StringEntity se = new StringEntity(userObj.toString());
                        post.setEntity(se);

                        post.setHeader("Accept", "application/json");
                        post.setHeader("Content-Type", "application/json");

                        response = client.execute(post);
                        status_code = response.getStatusLine().getStatusCode();
                        response_string = EntityUtils.toString(response.getEntity());
                    }

                    else if ( operation.equalsIgnoreCase("Logout") ) {
                        userObj.put("token", userPassword);

                        HttpDeleteWithBody delete=new HttpDeleteWithBody(apiAddress);
                        StringEntity se = new StringEntity(userObj.toString());
                        delete.setEntity(se);

                        delete.setHeader("delete", "application/json");
                        delete.setHeader("Content-Type","application/json");

                        response=client.execute(delete);
                        status_code=response.getStatusLine().getStatusCode();
                        response_string= EntityUtils.toString(response.getEntity());
                    }

                    else if ( operation.equalsIgnoreCase("Get") ) {
                        userObj.put("token", userPassword);

                        HttpGetWithBody get=new HttpGetWithBody(apiAddress);
                        StringEntity se = new StringEntity(userObj.toString());
                        get.setEntity(se);

                        get.setHeader("get", "application/json");
                        get.setHeader("Content-Type","application/json");

                        response=client.execute(get);
                        status_code=response.getStatusLine().getStatusCode();
                        response_string= EntityUtils.toString(response.getEntity());
                    }

                }catch (Exception e){
                    Log.d("http", " ["+operation+" EXCEPTION] :  , Msg : " + e);
                    Intent i = new Intent(receiverName)
                            .putExtra("message", operation+"_Error")
                            .putExtra("responseData", ""+new JSONObject());
                    context.sendBroadcast(i);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                if (status_code == 200){
                    Log.d("http", " ["+operation+" SUCCESS] :  , Msg : " + response_string);
                    Intent i = new Intent(receiverName)
                            .putExtra("message", operation+"_Success")
                            .putExtra("responseData", "" + response_string);
                    context.sendBroadcast(i);
                }

                else if (status_code == 401){
                    Log.d("http", " ["+operation+" FAILURE] : Msg" + response_string);
                    Intent i = new Intent(receiverName)
                            .putExtra("message", operation+"_Failure")
                            .putExtra("responseData", "" + response_string);
                    context.sendBroadcast(i);
                }

                else if (status_code == 500){
                    Log.d("http", " ["+operation+" ERROR] :  , Msg : " + response_string);
                    Intent i = new Intent(receiverName)
                            .putExtra("message", operation+"_Error")
                            .putExtra("responseData", "" + new JSONObject());
                    context.sendBroadcast(i);
                }
            }
        }.execute();
    }

}