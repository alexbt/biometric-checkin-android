package com.alexbt.biometric.util;

import android.content.Context;

import com.alexbt.biometric.R;

public class UrlUtils {
    private static String apiKey = null;
    private static String membersFormId = null;
    private static String presenceFormId = null;
    private static String getCheckinsUrl = null;
    private static String getMembersUrl = null;
    private static String updateMembersUrl = null;
    private static String addMembersUrl = null;
    private static String addPresenceUrl = null;


    private static void init(Context context){
        if(apiKey==null){
            apiKey = context.getResources().getString(R.string.JOTFORM_API_KEY);
        }
        if(presenceFormId==null){
            presenceFormId = context.getResources().getString(R.string.JOTFORM_CHECKINS_FORM_ID);
        }
        if(membersFormId==null){
            membersFormId = context.getResources().getString(R.string.JOTFORM_MEMBERS_FORM_ID);
        }
    }
    public static String getCheckinsUrl(Context context){
        init(context);
        if(getCheckinsUrl!=null){
            return getCheckinsUrl;
        }
        getCheckinsUrl = String.format(context.getResources().getString(R.string.GET_ALL_CHECKINS_URL), presenceFormId, apiKey);
        return getCheckinsUrl;
    }

    public static String getMembersUrl(Context context){
        init(context);
        if(getMembersUrl!=null){
            return getMembersUrl;
        }
        getMembersUrl = String.format(context.getResources().getString(R.string.GET_ALL_MEMBERS_URL), membersFormId, apiKey);
        return getMembersUrl;
    }

    public static String updateMembersUrl(Context context){
        init(context);
        if(updateMembersUrl!=null){
            return updateMembersUrl;
        }
        updateMembersUrl = String.format(context.getResources().getString(R.string.UPDATE_MEMBER_URL), "%s", apiKey);
        return updateMembersUrl;
    }

    public static String addMembersUrl(Context context){
        init(context);
        if(addMembersUrl!=null){
            return addMembersUrl;
        }
        addMembersUrl = String.format(context.getResources().getString(R.string.ADD_MEMBER_URL), membersFormId, apiKey);
        return addMembersUrl;
    }

    public static String addPresenceUrl(Context context){
        init(context);
        if(addPresenceUrl!=null){
            return addPresenceUrl;
        }
        addPresenceUrl = String.format(context.getResources().getString(R.string.ADD_CHECKIN_URL), presenceFormId, apiKey);
        return addPresenceUrl;
    }
}
