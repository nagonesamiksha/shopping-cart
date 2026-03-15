package com.saurabh.CacheEntity;


import java.io.Serializable;

public class User implements Serializable {
    private String name;
    private String email;
    private String contact;
    private String dob;

    // Constructor
    public User(String name, String email, String contact, String dob) {
        this.name = name;
        this.email = email;
        this.contact = contact;
        this.dob = dob;
    }

    // Getters and Setters (Optional, but useful for accessing the fields)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }
}

