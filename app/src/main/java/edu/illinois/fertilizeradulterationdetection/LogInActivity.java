package edu.illinois.fertilizeradulterationdetection;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseError;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.widget.Spinner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public class LogInActivity extends AppCompatActivity{
    private FirebaseAuth auth;
    private String phoneNumber;
    private Spinner countrySpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        //country code spinner
        countrySpinner = (Spinner) findViewById(R.id.spinnerCountries);
        ArrayAdapter<String> countrySpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, CountryData.countryAreaCodes);
        countrySpinner.setAdapter(countrySpinnerAdapter );
        countrySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(countrySpinnerAdapter);
        countrySpinnerAdapter.notifyDataSetChanged();
        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapter, View v,
                                       int position, long id) {
                // On selecting a spinner item
                String item = adapter.getItemAtPosition(position).toString();

                // Showing selected spinner item
                Toast.makeText(getApplicationContext(),"Selected Country : " + CountryData.countryNames[countrySpinner.getSelectedItemPosition()], Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });

        //submit button
        Button submit = findViewById(R.id.profile_submit_button);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });
    }

    public void signIn(){

        phoneNumber = ((EditText) findViewById(R.id.user_phone_number_input)).getText().toString().trim();

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_LONG).show();
            return;
        }

        auth.useAppLanguage();
        String code = "+" + countrySpinner.getSelectedItemPosition();
        phoneNumber = code + phoneNumber;


        Intent intent = new Intent(this, VerificationActivity.class);
        intent.putExtra("phoneNumber", phoneNumber);
        startActivity(intent);

//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
//
//        final String username = ((EditText)findViewById(R.id.user_username_input)).getText().toString().trim();
//        final String password = ((EditText)findViewById(R.id.user_password_input)).getText().toString().trim();
//
//        ref.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                for(DataSnapshot child : dataSnapshot.getChildren() ){
//                    // Do magic here
//                    User user = child.getValue(User.class);
//                    Log.i("tag","child " + user.getUsername());
//
//                    if(user != null && user.getUsername().equals(username) && user.getPassword().equals(password)){
//                        Log.i("tag","username" + username);
//                        Log.i("tag","password" + password);
//
//                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                        intent.putExtra("username", user.getUsername());
//                        startActivity(intent);
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//
//        });
//        FirebaseAuth mAuth = FirebaseAuth.getInstance();
//        mAuth.signInWithCredential(credential)
//                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
//                    @Override
//                    public void onComplete(@NonNull Task<AuthResult> task) {
//                        if (task.isSuccessful()) {
//
//                            FirebaseUser user = task.getResult().getUser();
//                            Log.d("Sign in with phone auth", "Success " + user);
//                            showMainActivity();
//                        } else {
//                            notifyUserAndRetry("Your Phone Number Verification is failed.Retry again!");
//                        }
//                    }
//                });
    }
}
