package com.originalstocksllc.himanshuraj.nicktest;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private BottomNavigationView mNavFrame;
    private FrameLayout mMainFrame;

    private FirebaseAuth mAuth;
    private DatabaseReference mUserRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            // mUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid());
        }
        toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Nicks");


        initFragmentUIs();

    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null){
           // mUserRef.child("online").setValue("true");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null){
        //    mUserRef.child("online").setValue(ServerValue.TIMESTAMP);

        }
    }

    private void initFragmentUIs() {
        mNavFrame = findViewById(R.id.bottom_nav_view);
        mMainFrame = findViewById(R.id.main_frame);

        final ChatFragment chatFragment = new ChatFragment();
        final RequestFragment requestFragment = new RequestFragment();
        final FriendsFragment friendsFragment = new FriendsFragment();

        setFragment(chatFragment);

        mNavFrame.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {

                    case R.id.bottom_chat_tab:
                        setFragment(chatFragment);
                        break;
                    case R.id.bottom_request_tab:
                        setFragment(requestFragment);
                        break;
                    case R.id.bottom_friends_tab:
                        setFragment(friendsFragment);
                        break;
                }

                return true;
            }
        });

    }

    private void setFragment(android.support.v4.app.Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {

            case R.id.user_profile_menu:
                startActivity(new Intent(MainActivity.this, MyProfileActivity.class));
                break;
            case R.id.all_users:
                startActivity(new Intent(MainActivity.this, FriendsActivity.class));
                break;

        }

        return true;
    }
}
