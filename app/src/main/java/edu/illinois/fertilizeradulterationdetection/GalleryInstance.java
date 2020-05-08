package edu.illinois.fertilizeradulterationdetection;

import android.widget.Gallery;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

public class GalleryInstance extends Image {

    private String store, district;

    public GalleryInstance(String store, String district, String note, String prediction, long lon, long lat, String date){
        super(lon, lat, date, prediction, note);
        this.store = store;
        this.district = district;
    }




    public String getStore(){ return store; }
    public String getDistrict(){ return district; }

}
