package edu.illinois.fertilizeradulterationdetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InfoActivity extends AppCompatActivity {

    private Spinner existingStoreView;
    private LinearLayout newStoreView;
    private Button toPrediction;

    private String id;
    private DatabaseReference databaseRef;
    private String storeName;

    private Intent intent;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        initialization();
        retrieveSavedStores();
    }

    private void initialization() {
        newStoreView = findViewById(R.id.newStore);
        existingStoreView = findViewById(R.id.existingStore);

        id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // set up background image
        Uri uri = Uri.parse(getIntent().getStringExtra("uri"));
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            ImageView imageView = findViewById(R.id.info_image);
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Fail to load image",Toast.LENGTH_LONG).show();
        }

        // inflater is used to inflate district, village and store text field into its parent view. Please refer to template_label_edittext.xml for details about "custom" view
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final List<String> labels = new ArrayList<>(Arrays.asList("District", "Village", "Store"));
        final List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            assert inflater != null;
            @SuppressLint("InflateParams") View custom = inflater.inflate(R.layout.template_label_edittext, null);

            TextView tv = custom.findViewById(R.id.label);
            tv.setText(labels.get(i) + ": ");

            EditText et = custom.findViewById(R.id.text);
            int id = View.generateViewId();
            ids.add(id);
            et.setId(id);

            newStoreView.addView(custom);
        }

        //pass info to prediction
        intent = new Intent(getApplicationContext(), PredictActivity.class);
        toPrediction = findViewById(R.id.to_prediction);
        toPrediction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // relay extras from previous activity
                intent.putExtra("uri", getIntent().getStringExtra("uri"));
                intent.putExtra("isFromStorage", getIntent().getBooleanExtra("isFromStorage", false));

                // pass new extras obtained in this activity
                EditText noteView = findViewById(R.id.note_text);
                intent.putExtra("note", noteView.getText().toString());

                if (storeName == null) {
                    for (int i = 0; i < 3; i++) {
                        EditText v = findViewById(ids.get(i)); //TODO: check valid store, district, village name, i.e. field value should not be empty
                        intent.putExtra(labels.get(i), v.getText().toString());
                    }
                } else {
                    intent.putExtra("Store", storeName);
                }

                startActivity(intent);
            }
        });
    }

    public void checkButton(View V) {
        RadioGroup radioGroup = findViewById(R.id.radio);;
        int radioId = radioGroup.getCheckedRadioButtonId();
        // save to existing store
        if (radioId == R.id.radio1) {
            existingStoreView.setVisibility(View.VISIBLE);
            newStoreView.setVisibility(View.GONE);
            toPrediction.setVisibility(View.VISIBLE);
        } else { // save to new store
            newStoreView.setVisibility(View.VISIBLE);
            existingStoreView.setVisibility(View.GONE);
            toPrediction.setVisibility(View.VISIBLE);
            intent.putExtra("newStore", true);
        }
    }

    private void retrieveSavedStores() {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<String> stores = new ArrayList<>();
                for(DataSnapshot child : dataSnapshot.child(id).child("images").getChildren()){
                    stores.add(child.getKey());
                }

                if (stores.size() == 0) {
                    findViewById(R.id.radio1).setVisibility(View.GONE);
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_item, stores);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                existingStoreView.setAdapter(adapter);
                existingStoreView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        storeName = (String) adapterView.getItemAtPosition(i);
                        intent.putExtra("newStore", false);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                throw databaseError.toException();
            }
        });
    }
}
