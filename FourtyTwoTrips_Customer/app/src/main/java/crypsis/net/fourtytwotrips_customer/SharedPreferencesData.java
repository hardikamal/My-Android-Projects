package crypsis.net.fourtytwotrips_customer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPreferencesData {

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    public SharedPreferencesData(Context context) {
        prefs=PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
    }

    public void update(String id, String name){
        editor.putString("userId", id);
        editor.putString("userName", name);
        editor.commit();
    }

    public void clear() {
        editor.putString("userId","NA");
        editor.putString("userName","NA");
        editor.commit();
    }

    public String getId(){
        return prefs.getString("userId", "NA");
    }

    public String getName(){
        return prefs.getString("userName", "NA");
    }


}
