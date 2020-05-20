package edu.illinois.fertilizeradulterationdetection.entity;

import java.util.ArrayList;

public class User {
    private String phoneNumber, occupation;

    public User(String number, String occ){
        phoneNumber = number;
        occupation = occ;
    }

    public String getPhoneNumber(){return phoneNumber;}

    public String getOccupation() {return occupation;}

    public void setPhoneNumber(String number) {phoneNumber = number;}

    public void setOccupation(String occ) {occupation = occ;}
}
