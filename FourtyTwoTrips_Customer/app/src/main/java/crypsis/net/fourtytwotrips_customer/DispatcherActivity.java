package crypsis.net.fourtytwotrips_customer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import android.app.Activity;

import java.security.MessageDigest;


public class DispatcherActivity extends Activity {

    Context context;
    String userId, userName;

    SharedPreferencesData sharedPreferencesData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dispatcher_layout);
        context = this;

        sharedPreferencesData = new SharedPreferencesData(this);

        userId = sharedPreferencesData.getId();
        userName = sharedPreferencesData.getName();

        if ( userId != null && userId.equalsIgnoreCase("NA") == false ) {
            Toast.makeText(context, "welcome "+userName+" ("+userId+")", Toast.LENGTH_LONG).show();

            Intent homeActivity = new Intent(context, HomeActivity.class);
            //homeActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);   // Clearing All Previous Activities..
            startActivity(homeActivity);
            finish();
        }
        else{
            Intent loginActivity = new Intent(this, LoginActivity.class);
            startActivity(loginActivity);
            finish();
        }

    }

    public void getAppKeyHash() {
        /*try {
            PackageInfo info = getPackageManager().getPackageInfo("crypsis.net.fourtytwotrips_customer", PackageManager.GET_SIGNATURES);
            for (android.content.pm.Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        }catch (Exception e){}*/
    }
}


