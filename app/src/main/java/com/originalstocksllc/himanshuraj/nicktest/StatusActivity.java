package com.originalstocksllc.himanshuraj.nicktest;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class StatusActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private TextInputLayout statusInputLayout;
    private Button setStatusButton;

    private DatabaseReference mStatusDatabase;
    private FirebaseUser mCurrentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        mToolbar = findViewById(R.id.status_app_bar_layout);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Account Status");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setStatusButton = findViewById(R.id.set_status_button);
        statusInputLayout = findViewById(R.id.status_edit_layout);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        final String currentUserId = mCurrentUser.getUid();

        mStatusDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);

        setStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(StatusActivity.this, "Hang on, We're updating your status.", Toast.LENGTH_SHORT).show();
                String inputedStatus = statusInputLayout.getEditText().getText().toString();
                // Set Status
                mStatusDatabase.child("status").setValue(inputedStatus).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                           // mProgressDialog.dismiss();
                            Toast.makeText(StatusActivity.this, "Status updated successfully.", Toast.LENGTH_SHORT).show();
                            Intent toProfile = new Intent(StatusActivity.this, MyProfileActivity.class);
                            startActivity(toProfile);
                            finish();
                        }else{
                            Toast.makeText(StatusActivity.this, "Something's wrong...", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });

    }
}
