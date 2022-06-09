package com.alexbt.biometric.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.alexbt.biometric.BuildConfig;
import com.alexbt.biometric.R;
import com.alexbt.biometric.util.EmailUtils;

public class InfoFragment extends Fragment {
    private EditText distance;
    private EditText jotformMembersFormId;
    private EditText jotformPresencesFormId;
    private EditText jotformApiKey;
    private View sendErrorButton;

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null || getActivity() == null || getContext() == null) {
            return null;
        }

        View root = inflater.inflate(R.layout.fragment_info, container, false);
        TextView buildVersion = root.findViewById(R.id.buildVersion);
        buildVersion.setText(BuildConfig.VERSION);

        TextView lastDayExported = root.findViewById(R.id.lastDayExported);
        lastDayExported.setText(getActivity()
                .getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE)
                .getString("lastDayExportedProp", "Jamais"));

        CheckBox memberChangedSinceLastExport = root.findViewById(R.id.memberChangedSinceLastExport);
        memberChangedSinceLastExport.setChecked(!getActivity()
                .getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE)
                .getBoolean("membersChangedSinceLastExportProp", false));

        sendErrorButton = root.findViewById(R.id.send_error_report);
        setSendReportStatus();

        final SharedPreferences sharedPreferences = getActivity().getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE);
        distance = root.findViewById(R.id.distance);
        jotformMembersFormId = root.findViewById(R.id.jotformMembersFormId);
        jotformPresencesFormId = root.findViewById(R.id.jotformPresencesFormId);
        jotformApiKey = root.findViewById(R.id.jotformApikey);
        distance.setText(String.format("%s", sharedPreferences.getFloat("distanceProp", 0.75f)));
        TextWatcher te = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String valueStr = distance.getText().toString();
                try {
                    float value = Float.parseFloat(valueStr);
                    sharedPreferences.edit().putFloat("distanceProp", value).apply();
                } catch (NumberFormatException e) {
                    sharedPreferences.edit().putFloat("distanceProp", 0.75f).apply();
                }
            }
        };
        distance.addTextChangedListener(te);

        jotformApiKey.setText(sharedPreferences.getString("jotformApiKeyProp", getContext().getResources().getString(R.string.JOTFORM_API_KEY)));
        te = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                sharedPreferences.edit().putString("jotformApiKeyProp", jotformApiKey.getText().toString()).apply();
            }
        };


        jotformMembersFormId.setText(sharedPreferences.getString("jotformMembersFormIdProp", getContext().getResources().getString(R.string.JOTFORM_MEMBERS_FORM_ID)));
        te = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                sharedPreferences.edit().putString("jotformMembersFormIdProp", jotformMembersFormId.getText().toString()).apply();
            }
        };
        jotformMembersFormId.addTextChangedListener(te);

        jotformPresencesFormId.setText(sharedPreferences.getString("jotformPresencesFormIdProp", getContext().getResources().getString(R.string.JOTFORM_PRESENCES_FORM_ID)));
        te = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                sharedPreferences.edit().putString("jotformPresencesFormIdProp", jotformPresencesFormId.getText().toString()).apply();
            }
        };
        jotformMembersFormId.addTextChangedListener(te);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        setSendReportStatus();
    }

    private void setSendReportStatus() {
        if (getActivity() == null) {
            return;
        }
        String lastError = getActivity()
                .getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE)
                .getString("lastErrorProp", null);
        if (lastError != null) {
            sendErrorButton.setVisibility(View.VISIBLE);
            sendErrorButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    if (getContext() == null || getActivity() == null) {
                        return;
                    }
                    EmailUtils.sendErrorEmail(getActivity(), lastError);
                }
            });
        } else {
            sendErrorButton.setVisibility(View.INVISIBLE);
        }
    }
}
