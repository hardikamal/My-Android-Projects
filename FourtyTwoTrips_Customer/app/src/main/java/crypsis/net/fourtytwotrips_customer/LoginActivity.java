package crypsis.net.fourtytwotrips_customer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.android.Facebook;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class LoginActivity extends ActionBarActivity {

    Context context;

    LoginButton loginButton;
    TextView connectionStatus;

    SharedPreferencesData sharedPreferencesData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.login_layout);
        context = this;

        sharedPreferencesData = new SharedPreferencesData(this);

        connectionStatus = (TextView) findViewById(R.id.connectionStatus);

        loginButton = (LoginButton) findViewById(R.id.fbLogin);
        List<String> permissions = new ArrayList<>();
        permissions.add("public_profile");
        permissions.add("email");
        permissions.add("user_birthday");
        loginButton.setReadPermissions(permissions);
        //loginButton.performClick();  // Perform Login

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(this,"Launching Home , wait..", Toast.LENGTH_SHORT).show();
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);

        Session session = Session.getActiveSession();
        if (session.isOpened()) {
            Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user, Response response) {
                    if (user != null) {
                        String name = user.getFirstName() + user.getLastName();
                        String email = user.getProperty("email").toString();

                        sharedPreferencesData.update(email, name); // Stroing User Data..

                        Toast.makeText(context, "welcome " + name + " (" + email + ")", Toast.LENGTH_LONG).show();

                        Intent homeActivity = new Intent(context, HomeActivity.class);
                        homeActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);   // Clearing All Previous Activities..
                        startActivity(homeActivity);
                        finish();;

                    } else {
                        Toast.makeText(context, "Login Failed, TryAgain", Toast.LENGTH_LONG).show();
                        Intent loginActivity = new Intent(context, LoginActivity.class);
                        loginActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);   // Clearing All Previous Activities..
                        startActivity(loginActivity);
                        finish();
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
