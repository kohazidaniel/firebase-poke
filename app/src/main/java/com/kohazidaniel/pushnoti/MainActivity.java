package com.kohazidaniel.pushnoti;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private String url = "https://fcm.googleapis.com/fcm/send";
    private String server_key = "";
    private TextView pushToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final View layout_main = findViewById(R.id.layout_main);

        ImageButton copyToClipboardButton = findViewById(R.id.copy_button);
        final Button sendButton = findViewById(R.id.send_notification);

        copyToClipboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("token_on_clipboard", pushToken.getText().toString());
                clipboard.setPrimaryClip(clip);

                Snackbar.make(layout_main, R.string.snackbar_copy, Snackbar.LENGTH_SHORT).show();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                try {
                    RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

                    final EditText notificationToToken = findViewById(R.id.notification_to_token);
                    final EditText notificationTitle = findViewById(R.id.notification_title);
                    final EditText notificationBody = findViewById(R.id.notification_body);

                    if(notificationBody.getText().toString().matches("") ||
                            notificationBody.getText().toString().matches("") ||
                            notificationBody.getText().toString().matches("")){
                        Snackbar snack = Snackbar.make(layout_main, "Hiányos mezők", Snackbar.LENGTH_SHORT);
                        snack.setBackgroundTint(Color.parseColor("#D9514E"));
                        snack.show();
                        return;
                    }


                    JSONObject jsonBody = new JSONObject();
                    JSONObject jsonNotification = new JSONObject();

                    jsonNotification.put("title", notificationTitle.getText().toString().trim());
                    jsonNotification.put("text", notificationBody.getText().toString().trim());

                    jsonBody.put("notification", jsonNotification);
                    jsonBody.put("to", notificationToToken.getText().toString());
                    jsonBody.put("project_id", "mobilalkfejl-push-noti");

                    final String mRequestBody = jsonBody.toString();

                    StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if(response.contains("\"error\":\"InvalidRegistration\"")){
                                Snackbar snack = Snackbar.make(layout_main, R.string.snackbar_invalid_token, Snackbar.LENGTH_SHORT);
                                snack.setBackgroundTint(Color.parseColor("#D9514E"));
                                snack.show();
                                return;
                            }
                            if(response.contains("\"success\":1")){
                                Snackbar snack = Snackbar.make(layout_main, R.string.snackbar_succes, Snackbar.LENGTH_SHORT);
                                snack.setBackgroundTint(Color.parseColor("#2DA8D8"));
                                snack.show();

                                notificationBody.setText("");
                                notificationTitle.setText("");
                                notificationToToken.setText("");
                            } else {
                                Snackbar snack = Snackbar.make(layout_main, R.string.snackbar_error, Snackbar.LENGTH_SHORT);
                                snack.setBackgroundTint(Color.parseColor("#D9514E"));
                                snack.show();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Snackbar snack = Snackbar.make(layout_main, R.string.snackbar_error, Snackbar.LENGTH_SHORT);
                            snack.setBackgroundTint(Color.parseColor("#D9514E"));
                            snack.show();
                        }
                    }) {
                        @Override
                        public String getBodyContentType() {
                            return "application/json; charset=utf-8";
                        }

                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            final Map<String, String> headers = new HashMap<>();
                            headers.put("Authorization", "key=" + server_key);
                            return headers;
                        }

                        @Override
                        public byte[] getBody() throws AuthFailureError {
                            try {
                                return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                            } catch (UnsupportedEncodingException uee) {
                                VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8");
                                return null;
                            }
                        }
                    };

                    queue.add(stringRequest);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_HIGH));
        }

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        pushToken = findViewById(R.id.push_token);

                        if (!task.isSuccessful()) {
                            pushToken.setText(getString(R.string.error_getting_push_token));
                            return;
                        }

                        String token = task.getResult().getToken();
                        pushToken.setTextIsSelectable(true);
                        pushToken.setText(token);
                    }
                });

    }
}
