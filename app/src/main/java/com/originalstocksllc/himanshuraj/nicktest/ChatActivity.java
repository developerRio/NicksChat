package com.originalstocksllc.himanshuraj.nicktest;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {


    private static final int GALLERY_PICK = 1;
    private static final int TOTAL_ITEMS_TO_LOAD = 6;
    private final List<Messages> messagesList = new ArrayList<>();
    private Toolbar mToolbar;
    private String mChatUserId, mChatUserName;
    private DatabaseReference mRootReference;
    private DatabaseReference mUserRef;
    private FirebaseUser mCurrentUser;
    private FirebaseAuth mAuth;
    private String mCurrentUserId;
    private TextView mTittleView;
    private TextView lastSeenTextView;
    private CircleImageView chatUserImage;
    private ImageButton mChatAddButton;
    private ImageButton mChatSendButton;
    private EditText mChatMessageView;
    private RecyclerView mRecyclerMessageList;
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter messageAdapter;
    private SwipeRefreshLayout mRefreshLayout;
    private int itemPos = 0;
    private int mCurrentPage = 1;
    private String mLastKey = "";
    private String mPrevKey = "";
    private StorageReference mImageStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        mToolbar = findViewById(R.id.chat_app_bar);
        setSupportActionBar(mToolbar);

        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowCustomEnabled(true);

        if (mAuth.getCurrentUser() != null) {
            mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
            mRootReference = FirebaseDatabase.getInstance().getReference();
            mUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());
            mCurrentUserId = mCurrentUser.getUid();
        }
        mChatUserId = getIntent().getStringExtra("user_id");
        mChatUserName = getIntent().getStringExtra("user_name");

        // getSupportActionBar().setTitle(mChatUserName);
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.chat_custom_bar, null);
        mActionBar.setCustomView(actionBarView);

        //------- IMAGE STORAGE ---------
        mImageStorage = FirebaseStorage.getInstance().getReference();

        mRootReference.child("Chat").child(mCurrentUserId).child(mChatUserId).child("seen").setValue(true);


        mTittleView = findViewById(R.id.chat_user_name);
        lastSeenTextView = findViewById(R.id.chat_last_seen);
        chatUserImage = findViewById(R.id.chat_user_image);
        mChatSendButton = findViewById(R.id.chat_send_button);
        mChatAddButton = findViewById(R.id.chat_add_attachments);
        mChatMessageView = findViewById(R.id.chat_message_view);
        messageAdapter = new MessageAdapter(this, messagesList);
        mRecyclerMessageList = findViewById(R.id.messages_recycler_list);
        mRefreshLayout = findViewById(R.id.message_swipe_layout);

        mLinearLayout = new LinearLayoutManager(this);
        mRecyclerMessageList.setHasFixedSize(true);
        mRecyclerMessageList.setLayoutManager(mLinearLayout);
        mRecyclerMessageList.setAdapter(messageAdapter);

        loadMessages();

        getLastSeen();

        //Chatting seen & time Stamp uploading to database "Chat"

        mRootReference.child("Chat").child(mCurrentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (!dataSnapshot.hasChild(mChatUserId)) {

                    Map chatAddMap = new HashMap();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("timestamp", ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("Chat/" + mCurrentUserId + "/" + mChatUserId, chatAddMap);
                    chatUserMap.put("Chat/" + mChatUserId + "/" + mCurrentUserId, chatAddMap);

                    mRootReference.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                            if (databaseError != null) {
                                Log.d("CHAT_LOG", databaseError.getMessage());
                            }
                        }
                    });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i("Chat_Error", "onCancelled: " + databaseError.getMessage());
            }
        });


        mChatSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        mChatAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // add dialog custom to send docs, images, videos etc...
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);
            }
        });


        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mCurrentPage++;
                itemPos = 0;
                loadMoreMessages();

            }
        });


    }//onCreate close


    private void loadMoreMessages() {

        DatabaseReference messageRef = mRootReference.child("messages").child(mCurrentUserId).child(mChatUserId);
        Query messageQuery = messageRef.orderByKey().endAt(mLastKey).limitToLast(10);
        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Messages message = dataSnapshot.getValue(Messages.class);
                String messageKey = dataSnapshot.getKey();

                if (!mPrevKey.equals(messageKey)) {

                    messagesList.add(itemPos++, message);

                } else {

                    mPrevKey = mLastKey;

                }
                if (itemPos == 1) {

                    mLastKey = messageKey;

                }


                //Log.d("TOTALKEYS", "Last Key : " + mLastKey + " | Prev Key : " + mPrevKey + " | Message Key : " + messageKey);

                messageAdapter.notifyDataSetChanged();

                mRefreshLayout.setRefreshing(false);

                mLinearLayout.scrollToPositionWithOffset(10, 0);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void loadMessages() {// Rx

        DatabaseReference messageRef = mRootReference.child("messages").child(mCurrentUserId).child(mChatUserId);

        Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);


        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                Messages message = dataSnapshot.getValue(Messages.class);

                itemPos++;

                if (itemPos == 1) {

                    String messageKey = dataSnapshot.getKey();

                    mLastKey = messageKey;
                    mPrevKey = messageKey;

                }

                messagesList.add(message);
                messageAdapter.notifyDataSetChanged();

                mRecyclerMessageList.scrollToPosition(messagesList.size() - 1);

                mRefreshLayout.setRefreshing(false);

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void sendMessage() { // Tx

        String message = mChatMessageView.getText().toString();
        if (!TextUtils.isEmpty(message)) {

            String current_user_ref = "messages/" + mCurrentUserId + "/" + mChatUserId;
            String chat_user_ref = "messages/" + mChatUserId + "/" + mCurrentUserId;

            DatabaseReference user_message_push = mRootReference.child("messages")
                    .child(mCurrentUserId).child(mChatUserId).push();

            String push_id = user_message_push.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mCurrentUserId);

            Map messageUserMap = new HashMap();
            messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
            messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

            mChatMessageView.setText("");

            mRootReference.child("Chat").child(mCurrentUserId).child(mChatUserId).child("seen").setValue(true);
            mRootReference.child("Chat").child(mCurrentUserId).child(mChatUserId).child("timestamp").setValue(ServerValue.TIMESTAMP);

            mRootReference.child("Chat").child(mChatUserId).child(mCurrentUserId).child("seen").setValue(false);
            mRootReference.child("Chat").child(mChatUserId).child(mCurrentUserId).child("timestamp").setValue(ServerValue.TIMESTAMP);

            mRootReference.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                    if (databaseError != null) {
                        Log.d("CHAT_LOG", databaseError.getMessage().toString());
                    }
                }
            });

        }


    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mCurrentUser != null) {
            mUserRef.child("online").setValue("true");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {

            Uri imageUri = data.getData();

            final String current_user_ref = "messages/" + mCurrentUserId + "/" + mChatUserId;
            final String chat_user_ref = "messages/" + mChatUserId + "/" + mCurrentUserId;

            DatabaseReference user_message_push = mRootReference.child("messages")
                    .child(mCurrentUserId).child(mChatUserId).push();

            final String push_id = user_message_push.getKey();

            final StorageReference filepath = mImageStorage.child("message_images").child(push_id + ".jpg");

            filepath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                    if (task.isSuccessful()) {

                        filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String download_url = uri.toString();

                                Map messageMap = new HashMap();
                                messageMap.put("message", download_url);
                                messageMap.put("seen", false);
                                messageMap.put("type", "image");
                                messageMap.put("time", ServerValue.TIMESTAMP);
                                messageMap.put("from", mCurrentUserId);

                                Map messageUserMap = new HashMap();
                                messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
                                messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

                                mChatMessageView.setText("");

                                mRootReference.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                                        if (databaseError != null) {
                                            Log.d("CHAT_LOG", databaseError.getMessage().toString());
                                        }
                                    }
                                });
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mCurrentUser != null) {
            mUserRef.child("online").setValue(ServerValue.TIMESTAMP);

        }
    }

    private void getLastSeen() {

        mRootReference.child("Users").child(mChatUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChild("online")) {

                    String online = dataSnapshot.child("online").getValue().toString();

                    if (online.equals("true")) {
                        lastSeenTextView.setText(R.string.online);
                    } else {
                        long lastTime = Long.parseLong(online);
                        String timeString = new SimpleDateFormat("HH:mm dd MMM").format(new Date(lastTime));
                        String mLastSeen = "Last seen at " + timeString;
                        lastSeenTextView.setText(mLastSeen);
                    }
                }
                //String onlineStatus = getIntent().getStringExtra("online_status");
                String imageThumb = dataSnapshot.child("image_thumb").getValue().toString();
                String userName = dataSnapshot.child("name").getValue().toString();

                mTittleView.setText(userName);
                Picasso.get().load(imageThumb).placeholder(R.drawable.default_user).into(chatUserImage);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i("LAst seen error", "onCancelled: " + databaseError.getMessage());
            }
        });
    }
}
