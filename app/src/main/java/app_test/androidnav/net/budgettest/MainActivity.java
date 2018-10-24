package app_test.androidnav.net.budgettest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements
        View.OnClickListener, Response.ErrorListener, Response.Listener<JSONObject>,
        OnCompleteListener{
    private static final int RC_SIGN_IN = 0xff;
    private static final int DROPIN_CODE = 0xfd;

    private static final String TAG = API.TAG;
    private static final String CLIENT_ID="83414597563-pge7h9o7q28hfli1ambbqk5mmemuemnp.apps.googleusercontent.com";


    private GoogleSignInClient signInClient;
    private SignInButton sigInButton;
    private Button signOutButton;
    private Button subscribeButton;
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        API.init(this);
        setContentView(R.layout.activity_main);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(CLIENT_ID)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        this.signInClient = GoogleSignIn.getClient(this, gso);
        this.sigInButton = findViewById(R.id.sign_in_button);
        this.signOutButton = findViewById(R.id.btn_signout);
        this.subscribeButton = findViewById(R.id.btn_subscribe);

        this.tv = findViewById(R.id.text);
        this.sigInButton.setOnClickListener(this);
        this.signOutButton.setOnClickListener(this);
        this.subscribeButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.loginStateChanged();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.sign_in_button:
                Intent signInIntent = this.signInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
                break;
            case R.id.btn_signout:
                this.signInClient.signOut().addOnCompleteListener(this, this );
                break;
            case R.id.btn_subscribe:
                this.doSubscribe();
                break;
        }
    }

    private void doSubscribe(){
        final AlertDialog loading = new AlertDialog.Builder(this).create();
        loading.setTitle("Please wait..");
        loading.show();
        API.get("payment/client-token", new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                loading.hide();
                try {
                    Log.d(TAG, "onResponse: " + response.toString(2));
                    if(response.has("content") &&
                            response.getJSONObject("content").has("client_token")){
                        DropInRequest req = new DropInRequest().clientToken(
                                response.getJSONObject("content").getString("client_token")
                        );
                        startActivityForResult(req.getIntent(MainActivity.this), DROPIN_CODE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);

                String authCode = account.getServerAuthCode();
                Log.d(TAG, "AUTHCODE: " + authCode);
                Toast.makeText(this, authCode, Toast.LENGTH_LONG).show();

                // API Login
                JSONObject json = new JSONObject();
                json.put("code", authCode);
                json.put("redirect_uri", "urn:ietf:wg:oauth:2.0:oob");
                API.post("auth/google", json, this);

            } catch (ApiException e) {
                Log.w(TAG, "Sign-in failed", e);
                String errMsg = "Unknown error, make sure you have Google Play Services";
                if( e.getStatusCode() == 12500 ){
                    errMsg = "You Google Play Service is outdated.";
                }
                Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }else if(resultCode == DROPIN_CODE ){
            if (resultCode == Activity.RESULT_OK) {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                Log.d(TAG, "onActivityResult: " + result.getPaymentMethodType() +
                        ", Nonce: " + result.getPaymentMethodNonce());
                // use the result to update your UI and send the payment method nonce to your server
            } else {
                if (resultCode == Activity.RESULT_CANCELED) {
                    // the user canceled
                    Log.d(TAG, "onActivityResult: CANCELED");
                } else {
                    // handle errors here, an exception may be available in
                    Exception error = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
                    error.printStackTrace();
                }
            }
        }
    }
    private void loginStateChanged(){
        Log.d(TAG, "loginStateChanged");
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        if(account != null && API.TOKEN != null && API.TOKEN.length() > 5) {
            // SUdah login
            Log.d(TAG, account.getEmail() + ", AUTHCODE: " + account.getServerAuthCode());
            this.sigInButton.setVisibility(View.GONE);
            this.signOutButton.setVisibility(View.VISIBLE);
            this.subscribeButton.setVisibility(View.VISIBLE);
            API.get("ping", this);
        }else{
            this.sigInButton.setVisibility(View.VISIBLE);
            this.signOutButton.setVisibility(View.INVISIBLE);
            this.subscribeButton.setVisibility(View.GONE);
        }

    }



    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e(TAG, error.toString());
    }
    @Override
    public void onResponse(JSONObject response) {
        if (response.length() > 0) {

            try {
                Log.d(TAG, "onResponse: " + response.toString(2));
                // Check if we've got login token and save it
                if(response.has("content")
                   && response.getJSONObject("content").has("token")){
                    API.saveToken(response.getJSONObject("content").getString("token"));
                    this.loginStateChanged();
                }
                this.tv.append(response.toString(2));

            } catch (JSONException e) {
                // If there is an error then output this to the logs.
                Log.e(TAG, e.getMessage());
            }
        } else {
            // The user didn't have any repos.
            Log.e(TAG, "onResponse: EMpty response" );
        }
    }

    @Override
    public void onComplete(@NonNull Task task) {
        Log.d(TAG, "Signed OUT");
        API.saveToken("");
        this.loginStateChanged();
    }
}
