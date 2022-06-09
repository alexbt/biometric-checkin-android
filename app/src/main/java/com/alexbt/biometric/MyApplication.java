package com.alexbt.biometric;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;
import android.widget.Toast;

import com.alexbt.biometric.util.DateUtils;

import java.util.Arrays;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                MyApplication.saveError(getApplicationContext(), e);
            }
        });
    }

    public static void saveError(Context context, Throwable e) {
        String error = e == null ?
                "Unexpected exception null" :
                String.format("Version: %s\n" +
                                "Current Time: %s\n" +
                                "Error:%s\n" +
                                "StackTrace:%s",
                        BuildConfig.VERSION,
                        DateUtils.getCurrentTime(),
                        e.toString(),
                        Arrays.toString(e.getStackTrace()));

        context
                .getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE)
                .edit()
                .putString("lastErrorProp", error)
                .apply();
        Toast.makeText(context, "Error recorded: " + (e != null ? e.getMessage() : "null"), Toast.LENGTH_SHORT).show();
    }
}