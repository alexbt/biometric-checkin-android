package com.alexbt.biometric.model;

import androidx.annotation.NonNull;

import com.alexbt.biometric.util.DateUtils;
import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
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

    public String getUpdatedAt() {
        return updatedAt;
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

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
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

    public String toShortString() {
        return firstName + ", " + lastName + ", " + email;
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

    public String toKey() {
        return toUnique(firstName, lastName);
    }

    @Override
    public int compareTo(Member member) {
        return toUnique(this.firstName, this.lastName).compareTo(toUnique(member.getFirstName(), member.getLastName()));
    }
}