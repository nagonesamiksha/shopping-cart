package com.saurabh.user.entity;


import lombok.*;


import javax.persistence.Id;
import java.time.LocalDate;


public class User1 {

    private String name;
    private String email;
    private String contact; //make it contact
    private String DOB;// make it DOB

    public User1(String name, String email, String contact, String DOB) {
        this.name = name;
        this.email = email;
        this.contact = contact;
        this.DOB = DOB;
    }
    public User1() {

    }

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

    public String getDOB() {
        return DOB;
    }

    public void setDOB(String DOB) {
        this.DOB = DOB;
    }
}
