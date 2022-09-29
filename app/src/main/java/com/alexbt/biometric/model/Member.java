package com.alexbt.biometric.model;

import androidx.annotation.NonNull;

import com.alexbt.biometric.util.DateUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

public class Member implements Comparable<Member>, Serializable {
    private String submissionId;
    private String memberId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String updatedAt;
    private String lastCheckin;
    private float[][] image;

    public Member(String submissionId, String memberId, @NonNull String firstName, @NonNull String lastName, @NonNull String email, @NonNull String phone, float[][] image) {
        this.submissionId = submissionId;
        this.memberId = memberId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.image = null;
        this.lastCheckin = null;
        this.image = image;
        this.updatedAt = null;
        touchUpdatedAt();
    }


    public String getFirstName() {
        return firstName;
    }

    public float[][] getImage() {
        return image;
    }

    public String getLastCheckin() {
        return lastCheckin;
    }

    public void setLastCheckin(String lastCheckin) {
        this.lastCheckin = lastCheckin;
    }

    public void setImage(float[][] image) {
        touchUpdatedAt();
        this.image = image;
    }


    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        touchUpdatedAt();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        touchUpdatedAt();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        touchUpdatedAt();
    }

    public void setPhone(String phone) {
        this.phone = phone;
        touchUpdatedAt();
    }

    public String getPhone() {
        return phone;
    }

    public void touchUpdatedAt() {
        this.updatedAt = DateUtils.getCurrentTime();
    }

    public String getMemberId() {
        return memberId;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    @Override
    public String toString() {
        return firstName + " " + lastName + "\n" + email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        return firstName.equals(member.firstName) && lastName.equals(member.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName);
    }

    public static String toUnique(String firstName, String lastName) {
        return firstName + " " + lastName;
    }

    @Override
    public int compareTo(Member member) {
        return toUnique(this.firstName, this.lastName).compareTo(toUnique(member.getFirstName(), member.getLastName()));
    }

    @NonNull
    public static Comparator<Member> getSortComparator() {
        return (m1, m2) -> StringUtils.stripAccents(m1.toString().toLowerCase()).compareTo(StringUtils.stripAccents(m2.toString().toLowerCase()));
    }
}