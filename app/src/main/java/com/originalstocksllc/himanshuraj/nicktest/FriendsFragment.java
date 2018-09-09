package com.originalstocksllc.himanshuraj.nicktest;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
public class FriendsFragment extends Fragment {

    private DatabaseReference mFriendDatabase;
    private DatabaseReference mUserDatabase;
    private RecyclerView mFriendList;
    private FirebaseAuth mAuth;
    private String mCurrentUserId;
    private View mMainView;
    private FirebaseRecyclerOptions<FriendList> firebaseRecyclerOptions;
    private FirebaseRecyclerAdapter<FriendList, FriendsViewHolder> firebaseRecyclerAdapter;

    public FriendsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mMainView = inflater.inflate(R.layout.fragment_friends, container, false);
        mFriendList = mMainView.findViewById(R.id.friends_list_frag);
        mAuth = FirebaseAuth.getInstance();
        mCurrentUserId = mAuth.getCurrentUser().getUid();
        //DatabaseRef
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends").child(mCurrentUserId);
        mFriendDatabase.keepSynced(true);
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUserDatabase.keepSynced(true);

        mFriendList.setHasFixedSize(true);
        mFriendList.setLayoutManager(new LinearLayoutManager(getContext()));

        firebaseRecyclerOptions = new FirebaseRecyclerOptions.Builder<FriendList>().setQuery(mFriendDatabase, FriendList.class).build();

        firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<FriendList, FriendsViewHolder>(firebaseRecyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final FriendsViewHolder holder, int position, @NonNull final FriendList model) {

                final String listUserId = getRef(position).getKey();
                mUserDatabase.child(listUserId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        final String userName = dataSnapshot.child("name").getValue().toString();
                        String userStatus = dataSnapshot.child("status").getValue().toString();
                        String userThumbImage = dataSnapshot.child("image_thumb").getValue().toString();

                        holder.setName(userName);
                        holder.setStatus(userStatus);
                        holder.setThumbImage(userThumbImage);
                        holder.setLastSeen("");

                        /*if (dataSnapshot.hasChild("online")) {

                            String lastSeen = dataSnapshot.child("online").getValue().toString();
                            // TODO: 08-Sep-18 Some Error here........
                            String dateString = new SimpleDateFormat("HH:mm dd MMM").format(lastSeen);
                            //show online stuff like
                            holder.setLastSeen(dateString);
                        }*/

                        holder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                CharSequence options[] = new CharSequence[]{"Open Profile", "Send message"};
                                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                                builder.setTitle("Select options");
                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int option) {

                                        if (option == 0) {
                                            Intent profileIntent = new Intent(getContext(), UserProfileActivity.class);
                                            profileIntent.putExtra("user_id", listUserId);
                                            startActivity(profileIntent);
                                        }

                                        if (option == 1) {
                                            Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                            chatIntent.putExtra("user_id", listUserId);
                                            chatIntent.putExtra("user_name", userName);
                                            //chatIntent.putExtra("online_status", lastSeen);
                                            startActivity(chatIntent);
                                        }
                                    }
                                });
                                builder.show();

                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.i("DatabaseError", "onCancelled: " + databaseError.getMessage());
                    }
                });

            }

            @NonNull
            @Override
            public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_layout, parent, false);
                return new FriendsViewHolder(mView);
            }
        };

        mFriendList.setAdapter(firebaseRecyclerAdapter);


        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        firebaseRecyclerAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        firebaseRecyclerAdapter.stopListening();
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public FriendsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setDate(String date) {
            TextView userStatusView = mView.findViewById(R.id.users_status);
            userStatusView.setText(date);
        }

        public void setName(String name) {

            TextView userNameText = mView.findViewById(R.id.users_name);
            userNameText.setText(name);
        }

        public void setStatus(String status) {
            TextView userStatusText = mView.findViewById(R.id.users_status);
            userStatusText.setText(status);
        }

        public void setThumbImage(final String image_thumb) {
            final CircleImageView profileThumbImage = mView.findViewById(R.id.users_image);
//            Log.i("profileThumbImage", "setThumbImage: " + image_thumb);

            Picasso.get().load(image_thumb).networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.default_user)
                    .into(profileThumbImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            // Image is saved in storage as offline.
                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(image_thumb).placeholder(R.drawable.default_user).into(profileThumbImage);
                        }
                    });
        }


        public void setLastSeen(String lastSeen) {
            TextView userTextTime = mView.findViewById(R.id.users_time);
            userTextTime.setText(lastSeen);
        }
    }
}
