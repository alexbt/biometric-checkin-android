package com.alexbt.biometric.fragment;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.GridLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.alexbt.biometric.R;
import com.alexbt.biometric.fragment.viewmodel.JotformMemberViewModel;
import com.alexbt.biometric.model.Member;
import com.alexbt.biometric.util.UrlUtils;

import java.util.ArrayList;
import java.util.Set;

public class ListMembersFragment extends Fragment implements View.OnClickListener {
    private ArrayAdapter<Member> stringArrayAdapter;
    private JotformMemberViewModel jotformMemberViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (getContext() == null || getActivity() == null) {
            return;
        }
        super.onCreate(savedInstanceState);
        jotformMemberViewModel = JotformMemberViewModel.getModel(this,
                UrlUtils.getMembersUrl(getContext()),
                UrlUtils.updateMembersUrl(getContext()),
                UrlUtils.addMembersUrl(getContext()));
        jotformMemberViewModel.getJotformMembersOrFetch().observe(this, new Observer<Set<Member>>() {
            @Override
            public void onChanged(Set<Member> members) {
                if (members == null || getContext() == null) {
                    return;
                }
                stringArrayAdapter.clear();
                stringArrayAdapter.addAll(members);
                stringArrayAdapter.sort(Member.getSortComparator());
                stringArrayAdapter.notifyDataSetChanged();
            }
        });
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
                navController.navigate(R.id.navigation_member_edit, bundle);
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        if (getActivity() == null || getContext() == null) {
            return;
        }
        super.onResume();
        //Set<Member> members = MemberPersistence.getMembers();
        //stringArrayAdapter.clear();
        //stringArrayAdapter.addAll(members);
        //stringArrayAdapter.sort((m1, m2) -> m1.toString().compareTo(m2.toString()));
        //stringArrayAdapter.notifyDataSetChanged();
        jotformMemberViewModel.getJotformMembersOrFetch(this.getActivity());
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
}
