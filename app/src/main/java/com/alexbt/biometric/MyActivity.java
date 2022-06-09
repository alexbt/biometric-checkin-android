package com.alexbt.biometric;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.alexbt.biometric.persistence.MemberPersistence;
import com.alexbt.biometric.util.EmailUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class MyActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA}, 1);
        }

        try {
            URL url = new URL("https://github.com/alexbt/biometric-checkin-android/blob/main/version?raw=true");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String remoteVersion = in.readLine();
            in.close();

            SharedPreferences sharedPref = getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE);
            String lastError = sharedPref.getString("lastErrorProp", null);
            int lastErrorIgnoreCount = sharedPref.getInt("lastErrorIgnoreCountProp", 0);

            if (lastError != null && lastErrorIgnoreCount < 2) {
                Activity activity = this;
                new AlertDialog.Builder(this)
                        .setTitle("Rapport d'erreur")
                        .setMessage("Une erreur est survenue à la dernière exécution.\n Voulez-vous envoyer le rapport d'erreur ?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                EmailUtils.sendErrorEmail(activity, lastError);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                EmailUtils.markIgnoreError(activity);
                            }
                        }).show();
            }

            if (BuildConfig.VERSION.compareTo(remoteVersion) < 0) {
                new AlertDialog.Builder(this)
                        .setTitle(String.format("Nouvelle version '%s'", remoteVersion))
                        .setMessage("Voulez-vous la télécharger?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse("https://github.com/alexbt/biometric-checkin-android/blob/main/biometric-checkin.apk?raw=true"));
                                startActivity(i);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BottomNavigationView navView = findViewById(R.id.nav_view);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        //toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_baseline_add_24));
        //getActionBar().setIcon(R.drawable.ic_launcher)

        MemberPersistence.init(this);

        View viewById = findViewById(R.id.nav_host_fragment);
        viewById.setOnTouchListener(new OnSwipeTouchListener(this, MyActivity.this) {
            public void onSwipeTop() {
            }

            public void onSwipeRight() {
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                NavDestination current = navController.getCurrentDestination();

                if (current == null || current.getLabel() == null) {
                    return;
                }

                switch (current.getId()){
                    case R.id.navigation_info:
                        navController.navigate(R.id.navigation_scan);
                        break;
                    case R.id.navigation_scan:
                    case R.id.navigation_member_edit:
                    case R.id.navigation_member_add:
                        navController.navigate(R.id.navigation_members);
                        break;
                }
            }

            public void onSwipeLeft() {
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                NavDestination current = navController.getCurrentDestination();

                if (current == null || current.getLabel() == null) {
                    return;
                }

                switch (current.getId()) {
                    case R.id.navigation_member_edit:
                    case R.id.navigation_member_add:
                        navController.navigate(R.id.navigation_members);
                        break;
                    case R.id.navigation_members:
                        navController.navigate(R.id.navigation_scan);
                        break;
                    case R.id.navigation_scan:
                        navController.navigate(R.id.info);
                        break;
                }
            }

            public void onSwipeBottom() {
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
