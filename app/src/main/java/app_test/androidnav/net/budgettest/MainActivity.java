package app_test.androidnav.net.budgettest;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
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
    private static final String TAG = "TESTAUTH";
    private static final String CLIENT_ID="83414597563-pge7h9o7q28hfli1ambbqk5mmemuemnp.apps.googleusercontent.com";
    private static final String API_SERVER_DEV = "http://10.0.2.2";
    private static final String API_SERVER = "http://api.androidnav.net";

    private GoogleSignInClient signInClient;
    private SignInButton siginButton;
    private Button signoutButton;
    private TextView tv;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(CLIENT_ID)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        this.signInClient = GoogleSignIn.getClient(this, gso);
        this.siginButton = findViewById(R.id.sign_in_button);
        //this.siginButton.setSize(SignInButton.SIZE_STANDARD);
        requestQueue = Volley.newRequestQueue(this);
        this.signoutButton = findViewById(R.id.btn_signout);
        this.tv = findViewById(R.id.text);
        this.siginButton.setOnClickListener(this);
        this.signoutButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.loginStateChanged();
    }

    @Override
    public void onClick(View v) {
        if( v.getId() == R.id.sign_in_button ){
            // SignIn
            Intent signInIntent = this.signInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }else{
            // SignOut
            this.signInClient.signOut().addOnCompleteListener(this, this );
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "requestCode: " + requestCode + ", resultCode: " + resultCode);
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
                Log.d(TAG, "POST JSON: " + json.toString(2));

                JsonObjectRequest arrReq = new JsonObjectRequest(
                        Request.Method.POST,
                        API_SERVER + "/auth/google",
                        json,
                        this,
                        this);

                requestQueue.add(arrReq);

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


        }
    }
    private void loginStateChanged(){
        Log.d(TAG, "loginStateChanged");
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        if(account != null) {
            // SUdah login
            Log.d(TAG, account.getEmail() + ", AUTHCODE: " + account.getServerAuthCode());
            this.siginButton.setVisibility(View.INVISIBLE);
            this.signoutButton.setVisibility(View.VISIBLE);
        }else{
            this.siginButton.setVisibility(View.VISIBLE);
            this.signoutButton.setVisibility(View.INVISIBLE);
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
                this.tv.setText(response.toString(2));
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
        this.loginStateChanged();
    }
}
