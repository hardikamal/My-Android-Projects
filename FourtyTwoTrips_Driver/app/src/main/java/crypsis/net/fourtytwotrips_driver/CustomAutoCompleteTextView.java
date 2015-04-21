package crypsis.net.fourtytwotrips_driver;

import android.content.Context;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class CustomAutoCompleteTextView extends AutoCompleteTextView{

    AutoCompleteTextView autoCompleteTextView;
    boolean detectTextChange = true;

    Date lastTypedTime;
    Timer textWatchTimer = new Timer();

    public CustomOnTextChangeCallback callback;

    public interface CustomOnTextChangeCallback {
        void onTextChangeFinish(String text);
    }

    public CustomAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        autoCompleteTextView = this;
    }
    public CustomAutoCompleteTextView(Context context) {
        super(context);
        autoCompleteTextView = this;
    }
    public CustomAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        autoCompleteTextView = this;
    }

    public void setTextChangeListener(CustomOnTextChangeCallback call_back){
        callback = call_back;

        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                lastTypedTime = new Date();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (textWatchTimer != null) {
                    textWatchTimer.cancel();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if ( detectTextChange == true ) {
                    textWatchTimer = new Timer();
                    textWatchTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Date runTime = new Date();
                            if (lastTypedTime.getTime() + 180 <= runTime.getTime()) {   // If Typing Finished..
                                Log.d("customTextListener", "Typing Finished : " + autoCompleteTextView.getText());
                                callback.onTextChangeFinish(autoCompleteTextView.getText().toString());
                            }
                        }
                    }, 1000);
                }
            }
        });
    }

    public void changeText(final String text){
        detectTextChange = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    autoCompleteTextView.setText(text);
                }catch (Exception e){e.printStackTrace();}
                finally {
                    detectTextChange = true;
                }
            }
        }).start();
    }

}
