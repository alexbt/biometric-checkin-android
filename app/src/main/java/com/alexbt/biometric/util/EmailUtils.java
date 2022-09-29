package com.alexbt.biometric.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.alexbt.biometric.BuildConfig;

public class EmailUtils {

    public static void markIgnoreError(Activity activity) {
        SharedPreferences sharedPref = activity
                .getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE);
        int lastErrorIgnoreCount = sharedPref
                .getInt("lastErrorIgnoreCountProp", 0);

        if (lastErrorIgnoreCount < 2) {
            sharedPref
                    .edit()
                    .putInt("lastErrorIgnoreCountProp", lastErrorIgnoreCount + 1)
                    .apply();
        }
    }

    public static void sendErrorEmail(Activity activity, String lastError) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"alex.belisleturcot+biometric@gmail.com"});
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Biometric - Rapport d'erreurs");
        intent.putExtra(Intent.EXTRA_TEXT, String.format("Bonjour Alex,\n\n"
                        + "Veuillez trouver les logs de Biometric Checkin!\n\n\n"
                        + "Timestamp: %s\n"
                        + "Version: %s\n"
                        + "Erreur: %s\n\n\n"
                        + "Cordialement,",
                DateUtils.getCurrentTime(),
                BuildConfig.VERSION,
                lastError
        ));
        activity.startActivityForResult(intent, 1);
        activity
                .getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE)
                .edit()
                .remove("lastErrorProp")
                .remove("lastErrorIgnoreCountProp")
                .apply();
    }
}
