package com.originalstocksllc.himanshuraj.nicktest;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestFragment extends Fragment {

    private DatabaseReference mRequestDatabase;
    private DatabaseReference mUsersDatabase;
    private FirebaseAuth mAuth;
    private String mCurrent_user_id;
    private View mMainView;
    private FirebaseRecyclerOptions<FriendRequest> firebaseRecyclerOptions;
    private FirebaseRecyclerAdapter<FriendRequest, RequestFragment.FriendRequestHolder> firebaseRequestAdapter;
    private RecyclerView mRequestRecyclerView;


    public RequestFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mMainView = inflater.inflate(R.layout.fragment_request, container, false);

        //Database
        mAuth = FirebaseAuth.getInstance();
        mCurrent_user_id = mAuth.getCurrentUser().getUid();
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mRequestDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req").child(mCurrent_user_id);

        // Views
        mRequestRecyclerView = mMainView.findViewById(R.id.request_recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        mRequestRecyclerView.setLayoutManager(linearLayoutManager);
        mRequestRecyclerView.setHasFixedSize(true);


        final Query requestQuery = mRequestDatabase.orderByChild("timestamp");
        firebaseRecyclerOptions = new FirebaseRecyclerOptions.Builder<FriendRequest>()
                .setIndexedQuery(requestQuery, mRequestDatabase, FriendRequest.class).build();

        firebaseRequestAdapter = new FirebaseRecyclerAdapter<FriendRequest, FriendRequestHolder>(firebaseRecyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final FriendRequestHolder holder, int position, @NonNull FriendRequest model) {

               // if request_type is received then get the user info  else not
                final String otherUserId = getRef(position).getKey();
                mUsersDatabase.child(otherUserId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        final String userName = dataSnapshot.child("name").getValue().toString();
                        String userStatus = dataSnapshot.child("status").getValue().toString();
                        String userThumbImage = dataSnapshot.child("image_thumb").getValue().toString();

                        holder.setName(userName);
                        holder.setStatus(userStatus);
                        holder.setThumbImage(userThumbImage);

                        holder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                Intent profileIntent = new Intent(getContext(), UserProfileActivity.class);
                                profileIntent.putExtra("user_id", otherUserId);
                                startActivity(profileIntent);

                            }
                        });


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }

            @NonNull
            @Override
            public FriendRequestHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_layout, parent, false);
                return new FriendRequestHolder(mView);
            }
        };




        mRequestRecyclerView.setAdapter(firebaseRequestAdapter);


        return mMainView;
    }

    public static class FriendRequestHolder extends RecyclerView.ViewHolder{

        View mView;

        public FriendRequestHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setName(String userName) {
            TextView userNameText = mView.findViewById(R.id.users_name);
            userNameText.setText(userName);
        }

        public void setStatus(String userStatus) {
            TextView userStatusView = mView.findViewById(R.id.users_status);
            userStatusView.setText(userStatus);
        }

        public void setThumbImage(final String userThumbImage) {

            final CircleImageView profileThumbImage = mView.findViewById(R.id.users_image);

            Picasso.get().load(userThumbImage).networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.default_user)
                    .into(profileThumbImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            // Image is saved in storage as offline.
                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(userThumbImage).placeholder(R.drawable.default_user).into(profileThumbImage);
                        }
                    });

        }
        // set request_type now
    }

    @Override
    public void onStop() {
        super.onStop();
        firebaseRequestAdapter.stopListening();

    }

    @Override
    public void onStart() {
        super.onStart();
        firebaseRequestAdapter.startListening();
    }
}
