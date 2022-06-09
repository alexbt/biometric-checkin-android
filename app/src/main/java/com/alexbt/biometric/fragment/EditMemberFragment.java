package com.alexbt.biometric.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.alexbt.biometric.R;
import com.alexbt.biometric.fragment.viewmodel.JotformMemberViewModel;
import com.alexbt.biometric.model.CheckDetails;
import com.alexbt.biometric.model.CheckinMonth;
import com.alexbt.biometric.model.CheckinYear;
import com.alexbt.biometric.model.Member;
import com.alexbt.biometric.persistence.MemberPersistence;
import com.alexbt.biometric.util.InputValidator;
import com.alexbt.biometric.util.UrlUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class EditMemberFragment extends Fragment {
    public static Member member;
    public static Map<String, CheckDetails> checkinDetails;
    private EditText phone;
    private EditText email;
    private EditText firstName;
    private CheckBox hasImageCheckbox;
    private EditText lastName;
    private final SimpleDateFormat monthParse = new SimpleDateFormat("MM");
    private final SimpleDateFormat monthDisplay = new SimpleDateFormat("MMMM");
    private  JotformMemberViewModel jotformMemberViewModel;

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        if (member == null || checkinDetails == null) {
            NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.navigation_members);
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

        TextView lastCheckin = root.findViewById(R.id.last_checkin);

        CheckDetails checkDetails = checkinDetails.get(member.getMemberId());
        if (checkDetails != null) {
            String lastCheckinStr;
            if (checkDetails.getLastCheckinDate() != null && member.getLastCheckin() != null) {
                lastCheckinStr = checkDetails.getLastCheckinDate().compareTo(member.getLastCheckin()) > 1 ? checkDetails.getLastCheckinDate() : member.getLastCheckin();
            } else if (checkDetails.getLastCheckinDate() != null) {
                lastCheckinStr = checkDetails.getLastCheckinDate();
            } else if (member.getLastCheckin() != null) {
                lastCheckinStr = member.getLastCheckin();
            } else {
                lastCheckinStr = "Jamais";
            }

            lastCheckin.setText(lastCheckinStr);
            lastCheckin.setEnabled(false);
        }

        email = root.findViewById(R.id.email);
        email.setText(member.getEmail());

        TextView dates = root.findViewById(R.id.dates);
        StringBuilder value = new StringBuilder();
        if (checkDetails != null) {
            for (String year : checkDetails.getCheckinYears().keySet()) {
                value.append(String.format("%s\n", year));
                CheckinYear checkinYear = checkDetails.getCheckinYears().get(year);
                for (String month : checkinYear.getMonths().keySet()) {
                    CheckinMonth checkinMonth = checkinYear.getMonths().get(month);
                    try {
                        String monthName = monthDisplay.format(monthParse.parse(month));

                        value.append(String.format(Locale.getDefault(), "\t\u2022 %s: %d\n", monthName, checkinMonth.getDates().size()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                }
            }
            dates.setText(value.toString());
        } else {
            root.findViewById(R.id.checkinsLabel).setVisibility(TextView.INVISIBLE);
        }
        firstName = root.findViewById(R.id.firstName);
        firstName.setText(member.getFirstName());

        lastName = root.findViewById(R.id.lastName);
        lastName.setText(member.getLastName());

        hasImageCheckbox = root.findViewById(R.id.has_image);

        TextView hasImageLabel = root.findViewById(R.id.has_image_label);
        boolean hasImageFlag = EditMemberFragment.member.getImage() != null;
        if (hasImageFlag) {
            hasImageCheckbox.setChecked(true);
            hasImageLabel.setText("Photo présente");
        } else {
            hasImageCheckbox.setChecked(false);
            hasImageCheckbox.setVisibility(View.INVISIBLE);
            hasImageLabel.setText("Photo non présente");
        }

        TextView save = root.findViewById(R.id.checkin_button);
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
                EditMemberFragment.member.setPhone(phoneStr);
                EditMemberFragment.member.setEmail(email.getText().toString());
                EditMemberFragment.member.setFirstName(firstName.getText().toString());
                EditMemberFragment.member.setLastName(lastName.getText().toString());

                if (!hasImageCheckbox.isChecked() && EditMemberFragment.member.getImage() != null) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Suppression de la photo")
                            .setMessage("Voulez-vous vraiment supprimer la photo?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    EditMemberFragment.member.setImage(null);
                                    jotformMemberViewModel.updateMember(getActivity(), member);
                                    //MemberPersistence.updateMember(getActivity(), member);
                                    EditMemberFragment.member = null;

                                    NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                                    navController.navigate(R.id.navigation_members);
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();

                } else {
                    //MemberPersistence.updateMember(getActivity(), member);
                    jotformMemberViewModel.updateMember(getActivity(), member);
                    EditMemberFragment.member = null;

                    NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                    navController.navigate(R.id.navigation_members);
                }
            }
        });

        TextView cancel = root.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getContext() == null || getActivity() == null) {
                    return;
                }
                EditMemberFragment.member = null;
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                navController.navigate(R.id.navigation_members);
            }
        });

        TextView delete = root.findViewById(R.id.delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getContext() == null || getActivity() == null) {
                    return;
                }
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.REMOVE_MEMBER_TITLE)
                        .setMessage(Html.fromHtml(String.format(getString(R.string.CONFIRM_REMOVAL_MEMBER),
                                String.format("<br/><b>&nbsp;&nbsp;%s %s</b><br/>&nbsp;&nbsp;%s<br/>&nbsp;&nbsp;%s<br/>&nbsp;&nbsp;%s",
                                        member.getFirstName(),
                                        member.getLastName(),
                                        member.getEmail(),
                                        member.getPhone(),
                                        "(" + (member.getLastCheckin() != null ? member.getLastCheckin() : "Jamais") + ")"
                                ))))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                Toast.makeText(getActivity(), getString(R.string.MEMBER_REMOVED_CONFIRMATION), Toast.LENGTH_SHORT).show();
                                //MemberPersistence.removeMember(getActivity(), member);
                                EditMemberFragment.member = null;
                                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                                navController.navigate(R.id.navigation_members);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
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
