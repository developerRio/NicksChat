package com.originalstocksllc.himanshuraj.nicktest;


import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment {

    private RecyclerView mConvRecyclerView;
    private LinearLayoutManager linearLayoutManager;
    private DatabaseReference mChatUserDatabase;
    private DatabaseReference mMessageDatabase;
    private DatabaseReference mUsersDatabase;
    private FirebaseAuth mAuth;
    private String mCurrent_user_id;
    private View mMainView;
    private FirebaseRecyclerOptions<Conv> firebaseRecyclerOptions;
    private FirebaseRecyclerAdapter<Conv, ConvViewHolder> firebaseConvAdapter;

    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mMainView = inflater.inflate(R.layout.fragment_chat, container, false);

        mConvRecyclerView = mMainView.findViewById(R.id.conv_list);
        mAuth = FirebaseAuth.getInstance();

        mCurrent_user_id = mAuth.getCurrentUser().getUid();

        mChatUserDatabase = FirebaseDatabase.getInstance().getReference().child("Chat").child(mCurrent_user_id);
        mChatUserDatabase.keepSynced(true);

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersDatabase.keepSynced(true);

        mMessageDatabase = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrent_user_id);
        mMessageDatabase.keepSynced(true);

        linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        mConvRecyclerView.setHasFixedSize(true);
        mConvRecyclerView.setLayoutManager(linearLayoutManager);

        Query conversationQuery = mChatUserDatabase.orderByChild("timestamp");

        firebaseRecyclerOptions = new FirebaseRecyclerOptions.Builder<Conv>()
                .setIndexedQuery(conversationQuery, mChatUserDatabase, Conv.class).build();

        firebaseConvAdapter = new FirebaseRecyclerAdapter<Conv, ConvViewHolder>(firebaseRecyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final ConvViewHolder holder, int position, @NonNull final Conv conv) {

                final String list_user_id = getRef(position).getKey();
                Query lastMessageQuery = mMessageDatabase.child(list_user_id).limitToLast(1);

                lastMessageQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        String data = dataSnapshot.child("message").getValue().toString();
                        long lastTime = (long) dataSnapshot.child("time").getValue();

                        String dateString = new SimpleDateFormat("HH:mm").format(new Date(lastTime));

                        holder.setLastSeenTime(dateString);
                        holder.setMessage(data, conv.isSeen());
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

                mUsersDatabase.child(list_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String userName = dataSnapshot.child("name").getValue().toString();
                        String userThumbImage = dataSnapshot.child("image_thumb").getValue().toString();

                        holder.setName(userName);
                        holder.setUserImage(userThumbImage);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                // if user is friend then fire up the intent else not

                mMessageDatabase.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.hasChild(list_user_id)) {

                            holder.mView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                    chatIntent.putExtra("user_id", list_user_id);
                                    startActivity(chatIntent);
                                }
                            });

                            holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View v) {

                                    final int pos = holder.getAdapterPosition();
                                    final String selectedUserId = getRef(pos).getKey();

                                    CharSequence options[] = new CharSequence[]{"Open Profile", "Delete chat"};
                                    final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                                    builder.setTitle("Select options");
                                    builder.setCancelable(true);
                                    builder.setItems(options, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int option) {

                                            if (option == 0) {
                                                Intent profileIntent = new Intent(getContext(), UserProfileActivity.class);
                                                profileIntent.putExtra("user_id", list_user_id);
                                                startActivity(profileIntent);
                                            }else if (option == 1){

                                                mChatUserDatabase.child(selectedUserId).removeValue()
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Toast.makeText(getContext(), "Chat deleted from database", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });

                                            }
                                        }
                                    });
                                    builder.show();
                                    return true;
                                }
                            });
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


            }


            @NonNull
            @Override
            public ConvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_layout, parent, false);
                return new ConvViewHolder(mView);
            }
        };

        mConvRecyclerView.setHasFixedSize(true);
        mConvRecyclerView.setAdapter(firebaseConvAdapter);

        return mMainView;
    }


    @Override
    public void onStart() {
        super.onStart();
        firebaseConvAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        firebaseConvAdapter.stopListening();
    }

    public static class ConvViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public ConvViewHolder(View itemView) {
            super(itemView);

            mView = itemView;

        }

        public void setMessage(String message, boolean isSeen) {

            TextView userStatusView = mView.findViewById(R.id.users_status);
            userStatusView.setText(message);

            if (!isSeen) {
                userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.BOLD);
            } else {
                userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.NORMAL);
            }

        }

        public void setName(String name) {

            TextView userNameView = mView.findViewById(R.id.users_name);
            userNameView.setText(name);

        }

        public void setUserImage(final String thumb_image) {

            final CircleImageView userImageView = mView.findViewById(R.id.users_image);
            // Picasso.get().load(thumb_image).placeholder(R.drawable.default_user).into(userImageView);

            Picasso.get().load(thumb_image).networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.default_user)
                    .into(userImageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            // Image is saved in storage as offline.
                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(thumb_image).placeholder(R.drawable.default_user).into(userImageView);
                        }
                    });

        }

        public void setLastSeenTime(String lastTime) {
            TextView userTextTime = mView.findViewById(R.id.users_time);
            userTextTime.setText(lastTime);
        }

       /* public void setUserOnline(String online_status) {
            ImageView userOnlineView = mView.findViewById(R.id.user_single_online_icon);
            if (online_status.equals("true")) {
                userOnlineView.setVisibility(View.VISIBLE);
            } else {
                userOnlineView.setVisibility(View.INVISIBLE);
            }
        }*/

    }


}
