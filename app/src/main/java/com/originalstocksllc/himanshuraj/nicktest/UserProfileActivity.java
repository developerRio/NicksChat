package com.originalstocksllc.himanshuraj.nicktest;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private TextView userNameText, userStatusText, userFriendsText;
    private Button sendRequestButton, declineRequestButton;
    private ImageView userProfileImageView;
    private RelativeLayout progressLayout;


    // friend request database
    private String mCurrent_state;
    private DatabaseReference mFriendReqDatabase;
    private DatabaseReference mDatabaseOfFriend;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mNotificationDatabase;
    private DatabaseReference mRootRef;
    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;
    private String mUserId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mRootRef = FirebaseDatabase.getInstance().getReference();

        mFriendReqDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mDatabaseOfFriend = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child("notifications");

        mUserDatabase = FirebaseDatabase.getInstance().getReference();


        userNameText = findViewById(R.id.user_profile_name);
        userStatusText = findViewById(R.id.user_profile_status);
        userFriendsText = findViewById(R.id.user_profile_friends);
        userProfileImageView = findViewById(R.id.user_profile_image);
        sendRequestButton = findViewById(R.id.send_request_button);
        declineRequestButton = findViewById(R.id.decline_request_button);
        progressLayout = findViewById(R.id.progressBarLayout);
        progressLayout.setVisibility(View.VISIBLE);

        mCurrent_state = "not_friends";

        declineRequestButton.setVisibility(View.INVISIBLE);
        declineRequestButton.setEnabled(false);

        if (getIntent().hasExtra("my_user_id") || mUserId != null) {
            mUserId = getIntent().getStringExtra("my_user_id");
        }else {
            mUserId = getIntent().getStringExtra("user_id");

        }// not null yet.
        // FriendsActivity's intent will never be empty....but FMS has chances to get empty

        Log.i("USER_ID_INTENT", "onCreate: " + mUserId);


        mUserDatabase.child("Users").child(mUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String profileImage = dataSnapshot.child("image").getValue().toString();
                String profileName = dataSnapshot.child("name").getValue().toString();
                String profileStatus = dataSnapshot.child("status").getValue().toString();
                //String  profileFriends = dataSnapshot.child("name").getValue().toString();

                userNameText.setText(profileName);
                userStatusText.setText(profileStatus);
                Picasso.get().load(profileImage).placeholder(R.drawable.default_user).into(userProfileImageView);

                if (mCurrentUser.getUid().equals(mUserId)) {

                    declineRequestButton.setVisibility(View.INVISIBLE);
                    declineRequestButton.setEnabled(false);

                    sendRequestButton.setEnabled(false);
                    sendRequestButton.setVisibility(View.INVISIBLE);

                }


                //_________________ FRIENDS LIST/REQUEST_____________________

                mFriendReqDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.hasChild(mUserId)) {
                            String req_type = dataSnapshot.child(mUserId).child("request_type").getValue().toString();

                            if (req_type.equals("received")) {

                                mCurrent_state = "req_received";
                                sendRequestButton.setText("Accept Friend Request");

                                declineRequestButton.setVisibility(View.VISIBLE);
                                declineRequestButton.setEnabled(true);

                            } else if (req_type.equals("sent")) {

                                mCurrent_state = "req_sent";
                                sendRequestButton.setText("Cancel Request");

                                declineRequestButton.setVisibility(View.INVISIBLE);
                                declineRequestButton.setEnabled(false);
                            }
                            progressLayout.setVisibility(View.GONE);


                        } else {

                            mDatabaseOfFriend.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    progressLayout.setVisibility(View.GONE);

                                    if (dataSnapshot.hasChild(mUserId)) {
                                        mCurrent_state = "friends";
                                        sendRequestButton.setText("Remove Friend");

                                        declineRequestButton.setVisibility(View.INVISIBLE);
                                        declineRequestButton.setEnabled(false);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    progressLayout.setVisibility(View.GONE);

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressLayout.setVisibility(View.GONE);
                        Log.i("UserProfileActivity", "onCancelled: " + databaseError.getMessage());
                    }
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressLayout.setVisibility(View.GONE);
                Log.i("UserProfileActivity", "onCancelled: " + databaseError.getMessage());
            }
        });


        sendRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // disabled the sent req button for not creating 2nd query
                sendRequestButton.setEnabled(false);

                // ______________________ NOT FRIENDS STATE __________________________

                if (mCurrent_state.equals("not_friends")) {

                    DatabaseReference newNotificationRef = mRootRef.child("notifications").child(mUserId).push();
                    String newNotificationId = newNotificationRef.getKey();

                    final HashMap<String, String> notificationData = new HashMap<>();
                    notificationData.put("from", mCurrentUser.getUid());
                    notificationData.put("type", "request");

                    Map requestMap = new HashMap();
                    requestMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + mUserId + "/request_type", "sent");
                    requestMap.put("Friend_req/" + mUserId + "/" + mCurrentUser.getUid() + "/request_type", "received");
                    requestMap.put("notifications/" + mUserId + "/" + newNotificationId, notificationData);

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            // Notification stuff

                            if (databaseError != null) {
                                Toast.makeText(UserProfileActivity.this, "Error in sending request, try again in a bit.", Toast.LENGTH_SHORT).show();
                            } else {

                                mCurrent_state = "req_sent";
                                sendRequestButton.setText("Cancel Friend Request");
                                declineRequestButton.setVisibility(View.INVISIBLE);
                                declineRequestButton.setEnabled(false);

                            }
                            sendRequestButton.setEnabled(true);
                        }
                    });
                }

                // ______________________ CANCEL REQUEST STATE __________________________


                if (mCurrent_state.equals("req_sent")) {

                    mFriendReqDatabase.child(mCurrentUser.getUid()).child(mUserId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            mFriendReqDatabase.child(mUserId).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    sendRequestButton.setEnabled(true);
                                    mCurrent_state = "not_friends";
                                    sendRequestButton.setText("Send Friend Request");

                                    declineRequestButton.setVisibility(View.INVISIBLE);
                                    declineRequestButton.setEnabled(false);

                                }
                            });
                        }
                    });
                }

                // _______________________ REQUEST RECEIVED STATE ___________________________

                if (mCurrent_state.equals("req_received")) {

                    final String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());

                    Map friendsMap = new HashMap();
                    friendsMap.put("Friends/" + mCurrentUser.getUid() + "/" + mUserId + "/date", currentDateTime);
                    friendsMap.put("Friends/" + mUserId + "/" + mCurrentUser.getUid() + "/date", currentDateTime);


                    friendsMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + mUserId, null);
                    friendsMap.put("Friend_req/" + mUserId + "/" + mCurrentUser.getUid(), null);


                    mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                            if (databaseError == null) {

                                sendRequestButton.setEnabled(true);
                                mCurrent_state = "friends";
                                sendRequestButton.setText("Remove this Person");

                                declineRequestButton.setVisibility(View.INVISIBLE);
                                declineRequestButton.setEnabled(false);

                            } else {

                                String error = databaseError.getMessage();
                                Toast.makeText(UserProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }


                // _______________________ REMOVE FRIENDS ________________________________

                if (mCurrent_state.equals("friends")) {

                    Map unfriendMap = new HashMap();
                    unfriendMap.put("Friends/" + mCurrentUser.getUid() + "/" + mUserId, null);
                    unfriendMap.put("Friends/" + mUserId + "/" + mCurrentUser.getUid(), null);

                    mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                            if (databaseError == null) {

                                mCurrent_state = "not_friends";
                                sendRequestButton.setText("Send Friend Request");

                                declineRequestButton.setVisibility(View.INVISIBLE);
                                declineRequestButton.setEnabled(false);

                            } else {
                                String error = databaseError.getMessage();
                                Toast.makeText(UserProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                            sendRequestButton.setEnabled(true);

                        }
                    });

                }

            }
        });

        declineRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mCurrent_state.equals("req_received")) {

                    Map unfriendMap = new HashMap();
                    unfriendMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + mUserId, null);
                    unfriendMap.put("Friend_req/" + mUserId + "/" + mCurrentUser.getUid(), null);

                    mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                            if (databaseError == null) {

                                mCurrent_state = "not_friends";
                                sendRequestButton.setText("Send Friend Request");

                                declineRequestButton.setVisibility(View.INVISIBLE);
                                declineRequestButton.setEnabled(false);

                            } else {
                                String error = databaseError.getMessage();
                                Toast.makeText(UserProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                            sendRequestButton.setEnabled(true);

                        }
                    });

                }

            }
        });
    }


}
