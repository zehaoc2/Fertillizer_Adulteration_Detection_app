package edu.illinois.fertilizeradulterationdetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InfoActivity extends AppCompatActivity {

    private Spinner existingStoreView;
    private LinearLayout newStoreView;
    private Button toPrediction;

    private String id;
    private DatabaseReference databaseRef;
    private String storeName;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;

        newStoreView = findViewById(R.id.newStore);

        final List<String> labels = new ArrayList<>(Arrays.asList("District", "Village", "Store"));
        final List<Integer> ids = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            @SuppressLint("InflateParams") View custom = inflater.inflate(R.layout.template_label_edittext, null);


            TextView tv = custom.findViewById(R.id.label);
            tv.setText(labels.get(i) + ": ");

            // todo: id added to ids list is 1, 2, 3, which are wrong
            int id = View.generateViewId();
            Log.e("idd", "id: " + ids.get(i).toString());
            ids.add(id);
            tv.setId(id);

            newStoreView.addView(custom);
        }

        for (int i = 0; i < 3; i++) {
            Log.e("idd", ids.get(i).toString());
        }

        // background image
        Uri uri = Uri.parse(getIntent().getStringExtra("uri"));
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            ImageView imageView = findViewById(R.id.info_image);
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Fail to load image",Toast.LENGTH_LONG).show();
        }

        // store selection
        existingStoreView = findViewById(R.id.existingStore);

        id = getIntent().getStringExtra("username");
        id = "q";
        databaseRef = FirebaseDatabase.getInstance().getReference();

        RetrieveSavedStores();

        // toPrediction button
        toPrediction = findViewById(R.id.to_prediction);
        toPrediction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText noteView = findViewById(R.id.note_text);
                String note = noteView.getText().toString();

                Intent intent = new Intent(getApplicationContext(), PredictActivity.class);
                intent.putExtra("uri", getIntent().getStringExtra("uri"));
                intent.putExtra("username", getIntent().getStringExtra("username"));
                intent.putExtra("isFromStorage", getIntent().getBooleanExtra("isFromStorage", false));

                intent.putExtra("note", note);

                if (storeName == null) {
                    //TODO: check valid store name & district name
                    for (int i = 0; i < 3; i++) {
                        EditText v = findViewById(ids.get(i));
                        intent.putExtra(labels.get(i), v.getText().toString());
                        Toast.makeText(getApplicationContext(), v.getText().toString(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    intent.putExtra("Store", storeName);
                }

                startActivity(intent);
            }
        });

    }

    /** ===================================== Store Selection ================================================== **/

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
        }
    }

    private void RetrieveSavedStores() {
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
