package com.alexbt.biometric.persistence;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.alexbt.biometric.BuildConfig;
import com.alexbt.biometric.R;
import com.alexbt.biometric.model.Member;
import com.alexbt.biometric.util.DateUtils;
import com.alexbt.biometric.util.InputValidator;
import com.alexbt.biometric.util.JsonUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MemberPersistence {

    private static final Set<Member> members = new TreeSet<>((m1, m2) -> m1.toString().compareTo(m2.toString()));

    public static void init(Activity activity) {
        MemberPersistence.members.addAll(load(activity));
    }

    public static Set<Member> getMembers() {
        return members;
    }

    public static Set<Member> load(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE);
        String json = sharedPreferences.getString("membersProp", "[]");
        // System.out.println("Output json"+json.toString());
        Set<Member> local_members = JsonUtil.toMembers(json);
        // System.out.println("Output map"+retrievedMap.toString());

        //During type conversion and save/load procedure,format changes(eg float converted to double).
        //So embeddings need to be extracted from it in required format(eg.double to float).
        int OUTPUT_SIZE = 192;

        for (Member member : local_members) {
            float[][] output = new float[1][OUTPUT_SIZE];
            float[][] arrayList = member.getImage();
            if (arrayList == null) {
                continue;
            }
            float[] other = arrayList[0];
            System.arraycopy(other, 0, output[0], 0, other.length);
            member.setImage(output);
        }

        return local_members;
    }

    public static void removeMember(Activity activity, Member member) {
        members.remove(member);
        saveMembers(activity);
    }

    public static void addMember(Activity activity, Member member) {
        if(activity==null){
            return;
        }
        members.add(member);
        saveMembers(activity);
        markMemberChanged(activity);
    }

    public static void saveMembers(Activity activity){
        String membersJson = JsonUtil.toJson(members);
        activity.getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE).edit().putString("membersProp", membersJson).apply();
    }

    public static void markMemberChanged(Activity activity){
        if(activity==null){
            return;
        }
        activity.getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE).edit().putBoolean("membersChangedSinceLastExportProp", true).apply();
    }

    public static void importMembers(Activity activity, Set<Member> importedMembers) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Map<String, Member> membersByName = new HashMap<>();
        for (Member member : members) {
            if (member.getUpdatedAt() == null) {
                member.setUpdatedAt(DateUtils.getCurrentTime());
            }
            membersByName.put(member.toShortString(), member);
        }

        int nbUpdated = 0;
        for (Member importedMember : importedMembers) {
            if (!InputValidator.isMemberValid(
                    importedMember.getFirstName(),
                    importedMember.getLastName(),
                    importedMember.getEmail(),
                    importedMember.getPhone())) {
                continue;
            }
            if (importedMember.getUpdatedAt() == null) {
                importedMember.setUpdatedAt(DateUtils.getCurrentTime());
            }

            Member existingMember = membersByName.get(importedMember.toShortString());
            if (existingMember == null) {
                nbUpdated++;
                membersByName.put(importedMember.toShortString(), importedMember);
                continue;
            }

            membersByName.put(existingMember.toShortString(), existingMember);

            if (importedMember.getUpdatedAt().compareTo(existingMember.getUpdatedAt()) < 0) {
                nbUpdated++;
                existingMember.setFirstName(importedMember.getFirstName());
                existingMember.setLastName(importedMember.getLastName());
                existingMember.setEmail(importedMember.getEmail());
                existingMember.setPhone(importedMember.getPhone());
                if (importedMember.getImage() != null) {
                    existingMember.setImage(importedMember.getImage());
                }
            }

            if (existingMember.getImage() == null && importedMember.getImage() != null) {
                nbUpdated++;
                existingMember.setImage(importedMember.getImage());
            }
        }

        members.clear();
        members.addAll(membersByName.values());

        String jsonContent = JsonUtil.toJson(members);
        editor.putString("membersProp", jsonContent);
        editor.apply();

        Toast.makeText(activity.getApplicationContext(), nbUpdated + " Membre(s) importé(s)/mis-à-jour", Toast.LENGTH_SHORT).show();
    }

    public static void exportMembers(Activity activity) {
        Toast.makeText(activity, "export", Toast.LENGTH_SHORT).show();
        SharedPreferences sharedPreferences = activity.getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE);
        String json = sharedPreferences.getString("membersProp", "[]");
        try {
            File memberExport = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + "members-export.json");
            if(!memberExport.exists()){
                memberExport.createNewFile();
            }
            OutputStream fo = new FileOutputStream(memberExport, false);
            fo.write(json.getBytes(StandardCharsets.UTF_8));
            fo.close();
            System.out.println("file created: " + memberExport);
            Toast.makeText(activity.getBaseContext(), "Membres exportés vers: " + memberExport.toString(), Toast.LENGTH_LONG).show();

            int nbMembers = MemberPersistence.getMembers().size();
            int nbMembersWithImage = 0;
            for (Member member : MemberPersistence.getMembers()) {
                if (member.getImage() != null) {
                    nbMembersWithImage++;
                }
            }
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"alex.belisleturcot+biometric@gmail.com"});
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_SUBJECT, String.format("Biometric - Export de %s membres", nbMembers));
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(memberExport));
            intent.putExtra(Intent.EXTRA_TEXT, String.format("Bonjour Alex,\n\n"
                            + "Veuillez trouver mon export des members!\n\n\n"
                            + "Timestamp: %s\n"
                            + "Version: %s\n"
                            + "Nombre de membres: %s\n"
                            + "Nombre de membres avec photo: %s\n\n\n"
                            + "Cordialement,",
                    DateUtils.getCurrentTime(),
                    BuildConfig.VERSION,
                    nbMembers,
                    nbMembersWithImage
            ));
            activity.startActivityForResult(intent, 101);
            activity.getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE)
                    .edit()
                    .putString("lastDayExportedProp", DateUtils.getCurrentDate())
                    .putBoolean("membersChangedSinceLastExportProp", false)
                    .apply();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static void updateMember(Activity activity, Member member) {
        if(activity==null){
            return;
        }

        Member found = null;
        for (Member m: members){
            if (m == member){
                found=m;
                break;
            }
        }
        if (found != null){
            members.remove(found);
        }
        members.add(member);
        saveMembers(activity);
        markMemberChanged(activity);
    }

    public static boolean isExportRequired(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("biometricCheckinSharedPref", Context.MODE_PRIVATE);
        boolean membersChanged = sharedPreferences.getBoolean("membersChangedSinceLastExportProp", false);
        if(!membersChanged){
            return false;
        }

        String lastExportDateStr = sharedPreferences.getString("lastDayExportedProp", DateUtils.getCurrentDate());
        int nbDaysSince = DateUtils.daysBeforeToday(lastExportDateStr);

        return nbDaysSince >= activity.getApplicationContext().getResources().getInteger(R.integer.NB_DAYS_ASK_FOR_EXPORT);
    }
}
