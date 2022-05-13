package com.alexbt.biometric.util;

import com.alexbt.biometric.model.Member;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Set;

public class JsonUtil {

    public static Set<Member> toMembers(String json) {
        TypeToken<Set<Member>> token = new TypeToken<Set<Member>>() {
        };
        return new Gson().fromJson(json, token.getType());
    }

    public static String toJson(Set<Member> members){
        return new Gson().toJson(members);
    }
}
