package com.alexbt.biometric.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.alexbt.biometric.R;
import com.alexbt.biometric.fragment.viewmodel.JotformMemberViewModel;
import com.alexbt.biometric.model.Member;
import com.alexbt.biometric.util.InputValidator;
import com.alexbt.biometric.util.UrlUtils;
import com.android.volley.Response;

import org.json.JSONObject;

public class EditMemberFragment extends Fragment {
    public Member member;
    private EditText phone;
    private EditText email;
    private EditText firstName;
    private CheckBox hasImageCheckbox;
    private EditText lastName;
    private JotformMemberViewModel jotformMemberViewModel;
    private final Response.Listener dummyListener = (Response.Listener<JSONObject>) response -> {
    };

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null || getActivity() == null || getContext() == null) {
            return null;
        }

        try {
            Bundle arguments = getArguments();
            member = (Member) arguments.getSerializable("member");
            if (member == null) {
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                navController.navigate(R.id.navigation_members);
                return null;
            }
        } catch (Exception e) {
            return null;
        }

        jotformMemberViewModel = JotformMemberViewModel.getModel(this,
                UrlUtils.getMembersUrl(getContext()),
                UrlUtils.updateMembersUrl(getContext()),
                UrlUtils.addMembersUrl(getContext()));

        View root = inflater.inflate(R.layout.fragment_member_edit, container, false);
        phone = root.findViewById(R.id.phone);
        phone.setText(member.getPhone());
        phone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        phone.setKeyListener(DigitsKeyListener.getInstance(getString(R.string.PHONE_DIGITS)));

        email = root.findViewById(R.id.email);
        email.setText(member.getEmail());

        firstName = root.findViewById(R.id.firstName);
        firstName.setText(member.getFirstName());

        lastName = root.findViewById(R.id.lastName);
        lastName.setText(member.getLastName());

        TextView save = root.findViewById(R.id.checkin_button);
        save.setEnabled(false);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getContext() == null || getActivity() == null) {
                    return;
                }
                String phoneStr = phone.getText().toString();
                phoneStr = phoneStr.replace(" ", "")
                        .replace("(", "")
                        .replace(")", "")
                        .replace("-", "")
                        .replace(" ", "");
                member.setPhone(phoneStr);
                member.setEmail(email.getText().toString());
                member.setFirstName(firstName.getText().toString());
                member.setLastName(lastName.getText().toString());

                if (!hasImageCheckbox.isChecked() && member.getImage() != null) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Suppression de la photo")
                            .setMessage("Voulez-vous vraiment supprimer la photo?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    member.setImage(null);
                                    jotformMemberViewModel.updateMember(getActivity(), member, dummyListener);
                                    //MemberPersistence.updateMember(getActivity(), member);
                                    member = null;

                                    NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                                    navController.navigate(R.id.navigation_members);
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();

                } else {
                    jotformMemberViewModel.updateMember(getActivity(), member, dummyListener);
                    member = null;

                    NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                    navController.navigate(R.id.navigation_members);
                }
            }
        });

        hasImageCheckbox = root.findViewById(R.id.has_image);
        hasImageCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save.setEnabled(true);
            }
        });

        TextView hasImageLabel = root.findViewById(R.id.has_image_label);
        boolean hasImageFlag = member.getImage() != null;
        if (hasImageFlag) {
            hasImageCheckbox.setChecked(true);
            hasImageLabel.setText(R.string.PHOTO_EXISTS);
        } else {
            hasImageCheckbox.setChecked(false);
            hasImageCheckbox.setVisibility(View.INVISIBLE);
            hasImageLabel.setText(R.string.PHOTO_MISSING);
        }

        TextView cancel = root.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getContext() == null || getActivity() == null) {
                    return;
                }
                member = null;
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                navController.navigate(R.id.navigation_members);
            }
        });

        TextWatcher te = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                boolean is_valid = InputValidator.isMemberValid(
                        firstName.getText().toString(),
                        lastName.getText().toString(),
                        email.getText().toString(),
                        phone.getText().toString()
                );

                save.setEnabled(is_valid);
            }
        };
        email.addTextChangedListener(te);
        lastName.addTextChangedListener(te);
        firstName.addTextChangedListener(te);
        phone.addTextChangedListener(te);
        return root;
    }
}
