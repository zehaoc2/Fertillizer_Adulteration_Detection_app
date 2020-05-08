package edu.illinois.fertilizeradulterationdetection;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Store {

    private String name, district;
    private ArrayList<Image> images;

    public Store(String name, String district, ArrayList<Image> images){
        this.name = name;
        this.district = district;
        this.images = images;
    }

    public void addImage(Image img){
        images.add(img);
    }
    public ArrayList<Image> getImages(){ return images; }

    public String getName() { return name; }
    public String getDistrict() { return district; }
}
