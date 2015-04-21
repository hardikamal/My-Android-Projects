package crypsis.net.fourtytwotrips_driver;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class LocalStorageData {

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    public LocalStorageData(Context context) {
        prefs=PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
    }

    public void update(String mobile,String token,String name){

        editor.putString("userMobile", mobile);
        editor.putString("userToken", token);
        editor.putString("userName", name);
        editor.commit();
    }

    public String getMobile(){
        return prefs.getString("userMobile", "NA");
    }
    public String getToken(){
        return prefs.getString("userToken", "NA");
    }
    public String getName(){
        return prefs.getString("userName", "NA");
    }

    public void clear() {

        editor.putString("userToken","NA");
        editor.putString("userMobile","NA");
        editor.putString("userName","NA");
        editor.commit();
    }
}
