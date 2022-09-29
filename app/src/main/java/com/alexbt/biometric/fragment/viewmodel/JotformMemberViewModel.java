package com.alexbt.biometric.fragment.viewmodel;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.alexbt.biometric.MyApplication;
import com.alexbt.biometric.events.MemberChangedEvent;
import com.alexbt.biometric.model.Content;
import com.alexbt.biometric.model.JotformMember;
import com.alexbt.biometric.model.Member;
import com.alexbt.biometric.util.FormatUtil;
import com.alexbt.biometric.util.RequestUtil;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class JotformMemberViewModel extends ViewModel {
    public MutableLiveData<Set<Member>> jotformMembers = new MutableLiveData<>();
    public MutableLiveData<Integer> jotformNextMemberId = new MutableLiveData<>();
    private final String URL;
    private final String updateMemberUrl;
    private final String addMemberUrl;

    public LiveData<Set<Member>> getJotformMembersOrFetch() {
        return jotformMembers;
    }

    public LiveData<Set<Member>> getJotformMembersOrFetch(final Activity activity) {
        if (jotformMembers.getValue() != null) {
            return jotformMembers;
        }
        RequestQueue requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                TypeToken<JotformMember> token = new TypeToken<JotformMember>() {
                };
                JotformMember jotformMember = new Gson().fromJson(response, token.getType());
                Set<Member> members = new TreeSet<>();

                int currentHighestMemberId = 0;
                for (Content content : jotformMember.getContent()) {
                    String memberId = (String) content.getAnswers().get("25").get("answer");
                    int current = 0;
                    try {
                        current = Integer.parseInt(memberId.replace("GBG-", ""));
                    } catch (Exception e) {
                        //ignore.
                    }
                    currentHighestMemberId = Math.max(currentHighestMemberId, current);

                    if (!content.getStatus().equals("ACTIVE")) {
                        continue;
                    }
                    Map<String, String> names = (Map<String, String>) content.getAnswers().get("3").get("answer");
                    String submissionId = content.getId();
                    String firstName = names.get("first");
                    String lastName = names.get("last");
                    //String belt = (String) content.getAnswers().get("14").get("answer");
                    String email = (String) content.getAnswers().get("4").get("answer");
                    if (email == null) {
                        email = "";
                    }
                    Object ph = content.getAnswers().get("5").get("answer");
                    String phone;
                    if (ph instanceof LinkedTreeMap){
                        phone = (String) ((LinkedTreeMap<?, ?>) ph).get("full");
                    }else {
                        phone = (String) ph;
                    }
                    if (phone == null) {
                        phone = "";
                    }
                    phone = phone
                            .replace("(", "")
                            .replace(")", "")
                            .replace("-", "")
                            .replace(" ", "");

                    String picture = (String) content.getAnswers().get("17").get("answer");
                    float[][] pc = null;
                    if (picture != null && !picture.trim().isEmpty()) {
                        try {
                            pc = new Gson().fromJson(picture, float[][].class);
                        } catch (Exception e) {

                        }
                    }
                    //String stripe = "";
                    //try {
                    //    stripe = (String) content.getAnswers().get("15").get("answer");
                    ////} catch (Exception e) {
                    //}
                    //Map<String, String> dateOfBirth = (Map<String, String>) content.getAnswers().get("6").get("answer");

                    //String dateOfBirthYear = (String) dateOfBirth.get("year");
                    //String dateOfBirthMonth = String.format("%02d", Integer.parseInt(dateOfBirth.get("month").toString()));
                    //String dateOfBirthDay = String.format("%02d", Integer.parseInt(dateOfBirth.get("day").toString()));

                    //String name = (String) names.get("prettyFormat");

                    members.add(new Member(submissionId, memberId, firstName, lastName, email, phone, pc));
                }
                jotformMembers.setValue(members);
                if ( currentHighestMemberId < 5000 ){
                    currentHighestMemberId = 5000;
                }
                jotformNextMemberId.setValue(currentHighestMemberId + 1);
            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }) {
        };
        requestQueue.add(stringRequest);
        return jotformMembers;
    }

    public static JotformMemberViewModel getModel(Fragment fragment, String url, String updateMemberUrl, String addMemberUrl) {
        if (fragment == null || fragment.getActivity() == null) {
            return null;
        }
        FragmentActivity activity = fragment.getActivity();
        return new ViewModelProvider(activity, new JotformMemberViewModelFactory(url, updateMemberUrl, addMemberUrl)).get(JotformMemberViewModel.class);
    }

    public JotformMemberViewModel(String url, String updateMemberUrl, String addMemberUrl) {
        this.URL = url;
        this.updateMemberUrl = updateMemberUrl;
        this.addMemberUrl = addMemberUrl;
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MemberChangedEvent event) {
        jotformMembers.setValue(null);
    }

    public void addMember(Activity activity, Member member) {

        if (member.getSubmissionId() != null && member.getMemberId() != null) {
            updateMember(activity, member, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        RequestUtil.sendCheckin(member, activity);
                    } catch (Exception e) {
                        //TODO ABT tmp
                    }
                }
            });
        } else {
            addNewMember(activity, member);
        }
    }

    private void addNewMember(Activity activity, Member member) {
        try {
            RequestQueue requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
            String picJson = new Gson().toJson(member.getImage());
            if (picJson == null || picJson.equals("null") || picJson.isEmpty()) {
                picJson = " ";
            }
            String memberId = "GBG-" + String.format("%04d", jotformNextMemberId.getValue());
            JSONObject top = new JSONObject();
            top.put("17", picJson);
            top.put("14", "White");
            top.put("15", "0");
            top.put("16", "Non");
            top.put("8", "");//Notes
            //top.put("19", memberId);
            top.put("25", memberId);
            top.put("24", jotformNextMemberId.getValue());
            JSONObject name = new JSONObject();
            name.put("first", member.getFirstName());
            name.put("last", member.getLastName());
            top.put("3", name);
            top.put("4", member.getEmail());
            top.put("5", FormatUtil.formatPhone(member.getPhone()));
            JSONObject date = new JSONObject();
            date.put("year", 1900);
            date.put("month", 1);
            date.put("day", 1);
            top.put("6", date);

            JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.POST, addMemberUrl, top, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    jotformNextMemberId.setValue(jotformNextMemberId.getValue() + 1);
                    //jotformMembers.setValue(null);
                    try {
                        String submissionId = (String) ((JSONObject) response.get("content")).get("submissionID");
                        member.setSubmissionId(submissionId);
                        member.setMemberId(memberId);
                        jotformMembers.getValue().add(member);
                        jotformMembers.setValue(jotformMembers.getValue());
                        RequestUtil.sendCheckin(member, activity);
                    } catch (Exception e) {
                        //TODO ABT tmp
                    }
                }
            }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {
            };
            requestQueue.add(stringRequest);
        } catch (Exception e) {

        }
    }

    public void updateMember(Activity activity, Member member, Response.Listener<JSONObject> listener) {
        try {
            String updateUrl = String.format(updateMemberUrl, member.getSubmissionId());
            RequestQueue requestQueue = Volley.newRequestQueue(activity.getApplicationContext());
            String picJson = " ";
            if (member.getImage() != null) {
                picJson = new Gson().toJson(member.getImage());
                if (picJson == null || picJson.equals("null") || picJson.isEmpty()) {
                    picJson = " ";
                }
            }
            JSONObject top = new JSONObject();
            JSONObject name = new JSONObject();
            name.put("first", member.getFirstName());
            name.put("last", member.getLastName());
            top.put("3", name);
            top.put("4", member.getEmail());
            top.put("5", FormatUtil.formatPhone(member.getPhone()));
            top.put("17", picJson);

            JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.POST, updateUrl, top, listener,
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            MyApplication.saveError(activity.getApplicationContext(), error);
                        }
                    }) {
            };
            requestQueue.add(stringRequest);
        } catch (Exception e) {
            MyApplication.saveError(activity.getApplicationContext(), e);
        }
    }
}


class JotformMemberViewModelFactory implements ViewModelProvider.Factory {
    private final String url;
    private final String updateMemberUrl;
    private final String addMemberUrl;

    public JotformMemberViewModelFactory(String url, String updateMemberUrl, String addMemberUrl) {
        this.url = url;
        this.updateMemberUrl = updateMemberUrl;
        this.addMemberUrl = addMemberUrl;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new JotformMemberViewModel(url, updateMemberUrl, addMemberUrl);
    }
}