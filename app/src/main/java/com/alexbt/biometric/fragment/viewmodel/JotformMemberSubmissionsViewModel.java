package com.alexbt.biometric.fragment.viewmodel;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.alexbt.biometric.events.NewCheckinRecordedEvent;
import com.alexbt.biometric.model.JotformMemberSubmissions;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class JotformMemberSubmissionsViewModel extends ViewModel {
    public MutableLiveData<JotformMemberSubmissions> jotformMemberSubmissions = new MutableLiveData<>();
    private final String URL;

    public LiveData<JotformMemberSubmissions> getJotformMemberSubmissionsOrFetch() {
        return jotformMemberSubmissions;
    }

    public LiveData<JotformMemberSubmissions> getJotformMemberSubmissionsOrFetch(final Activity activity) {
        if (jotformMemberSubmissions.getValue() != null) {
            return jotformMemberSubmissions;
        }
        RequestQueue requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                TypeToken<JotformMemberSubmissions> token = new TypeToken<JotformMemberSubmissions>() {
                };
                jotformMemberSubmissions.setValue(new Gson().fromJson(response, token.getType()));
            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }) {
        };
        requestQueue.add(stringRequest);
        return jotformMemberSubmissions;
    }

    public static JotformMemberSubmissionsViewModel getModel(Fragment fragment, String url) {
        if (fragment == null || fragment.getActivity() == null) {
            return null;
        }
        FragmentActivity activity = fragment.getActivity();
        return new ViewModelProvider(activity, new JotformMemberSubmissionsViewModelFactory(url)).get(JotformMemberSubmissionsViewModel.class);
    }

    public JotformMemberSubmissionsViewModel(String url) {
        this.URL = url;
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(NewCheckinRecordedEvent event) {
        jotformMemberSubmissions.setValue(null);
    }
}


class JotformMemberSubmissionsViewModelFactory implements ViewModelProvider.Factory {
    private final String url;

    public JotformMemberSubmissionsViewModelFactory(String url) {
        this.url = url;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new JotformMemberSubmissionsViewModel(url);
    }
}