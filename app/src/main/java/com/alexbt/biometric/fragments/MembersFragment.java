package com.alexbt.biometric.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.GridLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.alexbt.biometric.MyApplication;
import com.alexbt.biometric.R;
import com.alexbt.biometric.model.CheckDetails;
import com.alexbt.biometric.model.Content;
import com.alexbt.biometric.model.JotformMembers;
import com.alexbt.biometric.model.Member;
import com.alexbt.biometric.persistence.MemberPersistence;
import com.alexbt.biometric.util.JsonUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MembersFragment extends Fragment implements View.OnClickListener, Observer<JotformMembers> {
    private ArrayAdapter<Member> stringArrayAdapter;
    private ActivityResultLauncher<Intent> someActivityResultLauncher;
    private MembersViewModel vm;
    private final Map<String, CheckDetails> checkinDetails = new TreeMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sharedPreferences = getActivity().getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE);
        String jotformId = sharedPreferences.getString("jotformIdProp", getContext().getResources().getString(R.string.JOTFORM_ID));
        String jotformApiKey = sharedPreferences.getString("jotformApiKeyProp", getContext().getResources().getString(R.string.JOTFORM_API_KEY));
        String urlStr = getContext().getResources().getString(R.string.BASE_URL);
        String URL = String.format(urlStr, jotformId, jotformApiKey);
        vm = MembersViewModel.getModel(this, URL);
        vm.getMembers().observe(this, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null || getActivity() == null || getContext() == null) {
            return null;
        }

        View root = inflater.inflate(R.layout.fragment_members, container, false);

        stringArrayAdapter = new ArrayAdapter<Member>(getContext(),
                R.layout.abt_two_line_list_item_check,
                new ArrayList<>()) {

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                ConstraintLayout twoLineListItem;
                GridLayout gridLayout;

                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    twoLineListItem = (ConstraintLayout) inflater.inflate(R.layout.abt_two_line_list_item_check, null);
                } else {
                    twoLineListItem = (ConstraintLayout) convertView;
                }
                gridLayout = (GridLayout) twoLineListItem.getChildAt(0);

                TextView text1 = (TextView) gridLayout.getChildAt(0);
                CheckedTextView v = (CheckedTextView) gridLayout.getChildAt(1);
                if (position % 2 == 0) {
                    gridLayout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.chrome));
                } else {
                    gridLayout.setBackgroundColor(Color.WHITE);
                }

                Member member = getItem(position);
                v.setChecked(member.getImage() != null);
                text1.setText(String.format("%s %s", member.getFirstName(), member.getLastName()));

                TextView textView2 = (TextView) gridLayout.getChildAt(2);
                textView2.setText(String.format("%s", member.getEmail()));
                textView2.setTypeface(null, Typeface.ITALIC);

                return twoLineListItem;
            }

        };

        ListView viewById = root.findViewById(R.id.list_view);
        viewById.setAdapter(stringArrayAdapter);

        someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            try (InputStream is = getActivity().getContentResolver().openInputStream(data.getData())) {
                                String json_content = convertStreamToString(is);
                                Set<Member> members = JsonUtil.toMembers(json_content);
                                MemberPersistence.importMembers(getActivity(), members);
                            } catch (RuntimeException e) {
                                throw e;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    }
                });

        root.findViewById(R.id.go_to_add_member_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity() == null || getContext() == null) {
                    return;
                }
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                navController.navigate(R.id.navigation_member_add);
            }
        });


        viewById.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (getActivity() == null || getContext() == null) {
                    return;
                }
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                Member member = (Member) adapterView.getItemAtPosition(i);
                Bundle bundle = new Bundle();
                bundle.putSerializable("member", member);
                //bundle.putSerializable("checkins", checkinDetails);
                EditMemberFragment.member = member;
                EditMemberFragment.checkinDetails = checkinDetails;
                navController.navigate(R.id.navigation_member_edit, bundle);
            }
        });
        viewById.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (getActivity() == null || getContext() == null) {
                    return false;
                }
                Member member = (Member) adapterView.getItemAtPosition(position);

                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.REMOVE_MEMBER_TITLE)
                        .setMessage(Html.fromHtml(String.format(getString(R.string.CONFIRM_REMOVAL_MEMBER),
                                String.format("<br/><b>&nbsp;&nbsp;%s %s</b><br/>&nbsp;&nbsp;%s<br/>&nbsp;&nbsp;%s<br/>&nbsp;&nbsp;%s",
                                        member.getFirstName(),
                                        member.getLastName(),
                                        member.getEmail(),
                                        member.getPhone(),
                                        "(Dernier checkin: " + (member.getLastCheckin() != null ? member.getLastCheckin() : "Jamais") + ")"
                                ))))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                Toast.makeText(getActivity(), getString(R.string.MEMBER_REMOVED_CONFIRMATION), Toast.LENGTH_SHORT).show();
                                MemberPersistence.removeMember(getActivity(), member);
                                stringArrayAdapter.remove(member);
                                stringArrayAdapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();

                return true;
            }
        });

        root.findViewById(R.id.import_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                someActivityResultLauncher.launch(intent);
            }
        });

        root.findViewById(R.id.export_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MemberPersistence.exportMembers(getActivity());
            }
        });

        return root;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    @Override
    public void onResume() {
        if (getActivity() == null || getContext() == null) {
            return;
        }
        super.onResume();
        Set<Member> members = MemberPersistence.getMembers();
        stringArrayAdapter.clear();
        stringArrayAdapter.addAll(members);
        stringArrayAdapter.sort((m1, m2) -> m1.toString().compareTo(m2.toString()));
        stringArrayAdapter.notifyDataSetChanged();
        vm.getMembers(this.getActivity());
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onClick(View view) {
        if (getActivity() == null || getContext() == null) {
            return;
        }
        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.navigation_members);
    }

    @Override
    public void onChanged(JotformMembers jotformMembers) {
        if (jotformMembers == null) {
            return;
        }
        checkinDetails.clear();
        try {
            for (Content content : jotformMembers.getContent()) {
                Map<String, Object> names = content.getAnswers().get("4");
                Map<String, Object> date = content.getAnswers().get("9");
                Map<String, Object> dateAnswer = (Map<String, Object>) date.get("answer");
                String year = (String) dateAnswer.get("year");
                String month = String.format("%02d", Integer.parseInt(dateAnswer.get("month").toString()));
                String day = String.format("%02d", Integer.parseInt(dateAnswer.get("day").toString()));

                String name = names.get("prettyFormat").toString();

                CheckDetails checkDetails = checkinDetails.get(name);
                if (checkDetails == null) {
                    checkDetails = new CheckDetails();
                    checkinDetails.put(name, checkDetails);
                }
                checkDetails.addCheckin(year, month, day);
            }
        } catch (Exception e) {
            MyApplication.saveError(getContext(), e);
        }
    }
}
