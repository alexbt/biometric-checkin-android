package com.alexbt.biometric.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.alexbt.biometric.model.JotformMembers;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MembersViewModel extends ViewModel implements SharedPreferences.OnSharedPreferenceChangeListener {
    public MutableLiveData<JotformMembers> members = new MutableLiveData<>();
    private final String URL;

    public LiveData<JotformMembers> getMembers() {
        return members;
    }

    public LiveData<JotformMembers> getMembers(final Activity activity) {
        if (members.getValue() != null) {
            return members;
        }
        RequestQueue requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                TypeToken<JotformMembers> token = new TypeToken<JotformMembers>() {
                };
                members.setValue(new Gson().fromJson(response, token.getType()));
            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                //This code is executed if there is an error.
                System.out.println(error);
            }
        }) {
        };
        requestQueue.add(stringRequest);
        return members;
    }

    public static MembersViewModel getModel(Fragment fragment, String url) {
        if(fragment==null || fragment.getActivity()==null){
            return null;
        }
        FragmentActivity activity = fragment.getActivity();
        return new ViewModelProvider(activity, new MembersViewModelFactory(activity, url)).get(MembersViewModel.class);
    }

    public MembersViewModel(Activity activity, String url) {
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        this.URL = url;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals("lastCheckinProp")) {
            members.setValue(null);
        }
    }
}


class MembersViewModelFactory implements ViewModelProvider.Factory {
    private final String url;
    private final Activity activity;

    public MembersViewModelFactory(Activity activity, String url) {
        this.url = url;
        this.activity = activity;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new MembersViewModel(activity, url);
    }
}