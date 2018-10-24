package app_test.androidnav.net.budgettest;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class API {
    private static RequestQueue requestQueue;

    private static final String API_SERVER_DEV = "http://10.0.2.2/";
    private static final String API_SERVER = "http://api.androidnav.net/";
    static final String TAG = "BDGT";
    public static Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e(TAG, error.getMessage());
            error.printStackTrace();
        }
    };
    public static String TOKEN = "";
    public static Context CONTEXT;
    private static SharedPreferences PREFS;

    /**
     * Call this only once on first activity
     * @param context
     */
    static void init(Context context){
        CONTEXT = context;
        if( API.requestQueue == null ){
            API.requestQueue = Volley.newRequestQueue(context);
            PREFS = context.getSharedPreferences("creds", Context.MODE_PRIVATE);
            TOKEN = PREFS.getString("token", "");
            Log.d(TAG, "init: " + TOKEN);
        }
    }

    static boolean saveToken(String token){
        SharedPreferences.Editor editor = PREFS.edit();
        editor.putString("token", token);
        TOKEN = token;
        return editor.commit();
    }
    public static void get(String action,
                           Response.Listener<JSONObject> listener
    ){
        API.get(action, listener, null);
    }

    public static void get(String action,
                           Response.Listener<JSONObject> listener,
                           @Nullable Map<String, String> params
                           ){
        request(Request.Method.GET, action, null, listener, params, null);
    }

    public static void post(String action,
                            JSONObject body,
                            Response.Listener<JSONObject> listener
                            ){
        request(Request.Method.POST, action, body, listener, null, null);
    }
    static void request(int method,
                        String action,
                        @Nullable JSONObject body,
                        Response.Listener<JSONObject> listener,
                        @Nullable final Map<String, String> params,
                        @Nullable Response.ErrorListener errorListener){
        JsonObjectRequest req = new JsonObjectRequest(
                method,
                API_SERVER_DEV + action,
                body,
                listener,
                errorListener == null ? API.errorListener : errorListener
        ){
            @Override
            protected Map<String, String> getParams() {
                return  params;// == null ? super.getParams() : params;
            }

            @Override
            public Map<String, String> getHeaders(){
                if( TOKEN != null && TOKEN.length() > 5){
                    Map<String, String> headers = new HashMap<>();
                    headers.put("X-Token", TOKEN);
                    return headers;
                }else{
                    return Collections.emptyMap();
                }
            }
        };
        API.addQueue(req);
    }

    public static void addQueue(JsonObjectRequest req){
        Log.d(TAG, "REQUEST: " + req.getUrl());

        API.requestQueue.add(req);

    }

}

