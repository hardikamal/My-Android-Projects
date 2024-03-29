package crypsis.net.fourtytwotrips_customer;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.paytm.pgsdk.PaytmClientCertificate;
import com.paytm.pgsdk.PaytmMerchant;
import com.paytm.pgsdk.PaytmOrder;
import com.paytm.pgsdk.PaytmPGService;
import com.paytm.pgsdk.PaytmPaymentTransactionCallback;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class PaytmWalletActivity extends ActionBarActivity implements View.OnClickListener {

    Context context;

    Button payMoney;
    EditText moneyAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.paytm_wallet_layout);
        context =  this;

        moneyAmount = (EditText) findViewById(R.id.moneyAmount);
        payMoney = (Button) findViewById(R.id.payMoney);
        payMoney.setOnClickListener(this);


    }

    @Override
    public void onClick(View selected) {

        if ( selected == payMoney ){
            int amount=0;
            try {
                amount = Integer.parseInt(moneyAmount.getText().toString());
            }catch (Exception e){  amount =0;  }

            if ( amount > 0 ){
                proceedToPayment(amount);
            }
            else{
                Toast.makeText(this,"Please Enter Amount above 0",Toast.LENGTH_LONG).show();;
            }

        }

    }

    public void proceedToPayment (int amount){

        Random randomGenerator = new Random();
        int randomInt = randomGenerator.nextInt(1000);

        PaytmPGService paytmPGService =  PaytmPGService.getStagingService();

        Map<String, String> paramMap = new HashMap<String, String>();

       /* paramMap.put("REQUEST_TYPE", "RENEW_SUBSCRIPTION");
        paramMap.put("ORDER_ID", "42TRIPS000" + randomInt);
        paramMap.put("MID", getResources().getString(R.string.merchant_id));
        //paramMap.put("CUST_ID", getResources().getString(R.string.user_name));
        paramMap.put("SUBS_ID", "619");
        paramMap.put("TXN_AMOUNT", ""+amount);*/

        paramMap.put("REQUEST_TYPE", "SUBSCRIBE");
        paramMap.put("ORDER_ID", "42TRIPS000" + randomInt);
        paramMap.put("MID", getResources().getString(R.string.merchant_id));
        paramMap.put("CUST_ID", getResources().getString(R.string.user_name));
        paramMap.put("CHANNEL_ID", "WAP");
        paramMap.put("INDUSTRY_TYPE_ID", "Retail");
        paramMap.put("WEBSITE", "Crypsisapp");
        paramMap.put("TXN_AMOUNT", ""+amount);
        paramMap.put("SUBS_SERVICE_ID", "XYZ");
        paramMap.put("SUBS_AMOUNT_TYPE", "VARIABLE");
        paramMap.put("SUBS_MAX_AMOUNT", "100");
        paramMap.put("SUBS_FREQUENCY", "1");
        paramMap.put("SUBS_FREQUENCY_UNIT", "MONTH");
        paramMap.put("THEME", "merchant");
        paramMap.put("SUBS_ENABLE_RETRY", "0"); //this value will always be 0

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currentDate = dateFormat.format(calendar.getTime());
        String endDate = "";

        try {
            calendar.setTime(dateFormat.parse(currentDate));
            calendar.add(Calendar.MONTH, 5);
            endDate = dateFormat.format(calendar.getTime());
        }catch (Exception e){ e.printStackTrace(); }

        //Toast.makeText(getApplicationContext(), "CD "+currentDate+" , ED "+endDate, Toast.LENGTH_LONG).show();

        paramMap.put("SUBS_START_DATE", currentDate);
        paramMap.put("SUBS_GRACE_DAYS", "1");
        paramMap.put("SUBS_EXPIRY_DATE", endDate);

        /*paramMap.put("REQUEST_TYPE", "DEFAULT");
        paramMap.put("ORDER_ID", "42TRIPS000" + randomInt);
        paramMap.put("MID", getResources().getString(R.string.merchant_id));
        paramMap.put("CUST_ID", getResources().getString(R.string.user_name));
        paramMap.put("MOBILE_NO", getResources().getString(R.string.user_name));
        paramMap.put("EMAIL_ID", "test@test.com");
        paramMap.put("CHANNEL_ID", "WAP");
        paramMap.put("INDUSTRY_TYPE_ID", "Retail");
        paramMap.put("WEBSITE", "Crypsisapp");
        paramMap.put("TXN_AMOUNT", ""+amount);
        paramMap.put("THEME", "merchant");*/



        PaytmOrder paytmOrder = new PaytmOrder(paramMap);
        PaytmMerchant merchant = new PaytmMerchant( "http://dev.42trips.com/paytm/checksum", "http://dev.42trips.com/paytm/checksum_verification");
        PaytmClientCertificate certificate = null;

        paytmPGService.initialize(paytmOrder, merchant, certificate);

        paytmPGService.startPaymentTransaction(this, true, true, new PaytmPaymentTransactionCallback() {
            @Override
            public void onTransactionSuccess(Bundle bundle) {
                Log.d("success", "sussess callback");
                Toast.makeText(context, "Transaction Success", Toast.LENGTH_LONG).show();

                Intent homeActivity=new Intent(context, HomeActivity.class);
                homeActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);   // Clearing All Previous Activities..
                startActivity(homeActivity);
            }

            @Override
            public void onTransactionFailure(String s, Bundle bundle) {
                Toast.makeText(context,"Transaction Failure , Msg : "+s, Toast.LENGTH_LONG).show();
            }

            @Override
            public void networkNotAvailable() {
                Toast.makeText(context,"No Network", Toast.LENGTH_LONG).show();
            }

            @Override
            public void clientAuthenticationFailed(String s) {
                Toast.makeText(context,"Authentication Failed , Msg : "+s, Toast.LENGTH_LONG).show();
                Log.d("paytm", "Authentication Failed , Msg: "+s);

            }

            @Override
            public void someUIErrorOccurred(String s) {
                Toast.makeText(context,"URL ERROR", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onErrorLoadingWebPage(int i, String s, String s2) {
                Toast.makeText(context,"Error Loading Web Page", Toast.LENGTH_LONG).show();
            }

        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_paytm_wallet, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }


}
