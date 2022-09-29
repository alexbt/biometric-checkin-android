package com.alexbt.biometric.util;

import android.content.Context;

import com.alexbt.biometric.R;

public class UrlUtils {
    public static String getMembersUrl(Context context) {
        String apiKey = context.getResources().getString(R.string.JOTFORM_API_KEY);
        String membersFormId = context.getResources().getString(R.string.JOTFORM_MEMBERS_FORM_ID);
        return String.format(context.getResources().getString(R.string.GET_ALL_MEMBERS_URL), membersFormId, apiKey);
    }

    public static String updateMembersUrl(Context context) {
        String apiKey = context.getResources().getString(R.string.JOTFORM_API_KEY);
        return String.format(context.getResources().getString(R.string.UPDATE_MEMBER_URL), "%s", apiKey);
    }

    public static String addMembersUrl(Context context) {
        String apiKey = context.getResources().getString(R.string.JOTFORM_API_KEY);
        String membersFormId = context.getResources().getString(R.string.JOTFORM_MEMBERS_FORM_ID);
        return String.format(context.getResources().getString(R.string.ADD_MEMBER_URL), membersFormId, apiKey);
    }

    public static String addCheckinUrl(Context context) {
        String apiKey = context.getResources().getString(R.string.JOTFORM_API_KEY);
        String presenceFormId = context.getResources().getString(R.string.JOTFORM_CHECKINS_FORM_ID);
        return String.format(context.getResources().getString(R.string.ADD_CHECKIN_URL), presenceFormId, apiKey);
    }
}
