package com.alexbt.biometric.util;

import android.app.Activity;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.alexbt.biometric.MyApplication;
import com.alexbt.biometric.R;
import com.alexbt.biometric.events.NewCheckinRecordedEvent;
import com.alexbt.biometric.model.Member;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RequestUtil {
    private static String URL = null;
    private static String SOURCE_APP = null;
    private static TextToSpeech tts = null;

    public static void sendCheckin(Member member, Activity activity) {
        Date dateTime = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = df.format(dateTime);
        sendCheckin(member, dateTime, formattedDate, activity, null, null);
    }

    public static void sendCheckin(Member member, Date dateTime, String formattedDate, Activity activity, Response.Listener<JSONObject> responseListener, Response.ErrorListener errorListener) {
        if (URL == null) {
            URL = UrlUtils.addPresenceUrl(activity.getApplicationContext());
        }
        if (SOURCE_APP == null) {
            SOURCE_APP = activity.getApplicationContext().getResources().getString(R.string.SOURCE_APP);
        }
        if (tts == null) {
            tts = new TextToSpeech(activity.getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                }
            });
        }

        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(dateTime);
        String month = new SimpleDateFormat("MM", Locale.getDefault()).format(dateTime);
        String day = new SimpleDateFormat("dd", Locale.getDefault()).format(dateTime);
        String hourMin = new SimpleDateFormat("h:mm", Locale.getDefault()).format(dateTime);
        String hour = new SimpleDateFormat("h", Locale.getDefault()).format(dateTime);
        String min = new SimpleDateFormat("mm", Locale.getDefault()).format(dateTime);
        String pm_am = new SimpleDateFormat("a", Locale.getDefault()).format(dateTime);
        pm_am = pm_am.replace(".", "").toUpperCase();

        RequestQueue requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
        try {
            JSONObject top = new JSONObject();
            top.put("4", member.getMemberId());

            JSONObject name = new JSONObject();
            name.put("first", member.getFirstName());
            name.put("last", member.getLastName());
            top.put("8", name);

            JSONObject time = new JSONObject();
            time.put("timeInput", hourMin);
            time.put("ampm", pm_am);
            time.put("hourSelect", hour);
            time.put("minuteSelect", min);

            top.put("6", time);
            JSONObject date = new JSONObject();
            date.put("year", year);
            date.put("month", month);
            date.put("day", day);
            top.put("5", date);
            top.put("7", SOURCE_APP);
            JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.POST, URL, top, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    if (responseListener != null) {
                        responseListener.onResponse(response);
                    }
                    member.setLastCheckin(formattedDate);
                    EventBus.getDefault().post(new NewCheckinRecordedEvent());

                    LayoutInflater inflater = LayoutInflater.from(activity.getApplicationContext());
                    View layout = inflater.inflate(R.layout.custom_toast, null);
                    TextView text = (TextView) layout.findViewById(R.id.message);
                    text.setText(String.format("Présence enregistrée pour %s %s", member.getFirstName(), member.getLastName()));
                    Toast toast = new Toast(activity.getApplicationContext());
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.setDuration(Toast.LENGTH_LONG);
                    layout.setBackgroundColor(Color.DKGRAY);
                    toast.setView(layout);
                    toast.show();

                    tts.setLanguage(Locale.FRENCH);
                    int result = tts.speak(member.getFirstName() + " est là", TextToSpeech.QUEUE_FLUSH, null, null);
                    if (result == -1) {
                        MediaPlayer.create(activity.getApplicationContext(), Settings.System.DEFAULT_NOTIFICATION_URI).start();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (errorListener != null) {
                        errorListener.onErrorResponse(error);
                    }
                    MyApplication.saveError(activity.getApplicationContext(), error);
                    Toast.makeText(activity, String.format("Erreur d'enregistrement pour %s %s", member.getFirstName(), member.getLastName()), Toast.LENGTH_SHORT).show();
                }
            }) {
            };
            requestQueue.add(stringRequest);
        } catch (Exception e) {
            MyApplication.saveError(activity.getApplicationContext(), e);
        }
    }

    public static void init(FragmentActivity activity) {
        tts = new TextToSpeech(activity.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            }
        });
    }
}
