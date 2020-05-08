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

    public static ArrayList<Store> getInstances() {
        final ArrayList<Store> storeInstances = new ArrayList<Store>();

        //add instances to contacts ArrayList
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        ref = ref.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("images");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot store : dataSnapshot.getChildren()) {
                    Store str = store.getValue(Store.class);
                    storeInstances.add(str);
//                    for (DataSnapshot instance : store.getChildren()){
//                        Image img = instance.getValue(Image.class);
//                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return storeInstances;
    }

    public String getName() { return name; }
}
