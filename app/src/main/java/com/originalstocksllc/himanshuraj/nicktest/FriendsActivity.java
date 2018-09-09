package com.originalstocksllc.himanshuraj.nicktest;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendsActivity extends AppCompatActivity {

    private RecyclerView mUsersRecyclerView;
    private Toolbar mToolbar;
    private DatabaseReference mUsersDatabase;
    private FirebaseRecyclerAdapter<Users, UsersViewHolder> firebaseRecyclerAdapter;
    private FirebaseRecyclerOptions<Users> recyclerOptions;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        mToolbar = findViewById(R.id.users_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Friends");

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersDatabase.keepSynced(true);

        mUsersRecyclerView = findViewById(R.id.users_recycler_view);
        mUsersRecyclerView.setHasFixedSize(true);
        mUsersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerOptions = new FirebaseRecyclerOptions.Builder<Users>().setQuery(mUsersDatabase, Users.class).build();

        firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Users, UsersViewHolder>(recyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull UsersViewHolder holder, int position, @NonNull Users users) {
                holder.setName(users.getName());
                holder.setStatus(users.getStatus());
                holder.setThumbImage(users.getImage_thumb());

                // To get the user uniqueId or key
                final String uId = getRef(position).getKey();

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Intent profileIntent = new Intent(FriendsActivity.this, UserProfileActivity.class);
                        profileIntent.putExtra("user_id", uId);
                        startActivity(profileIntent);

                    }
                });
            }

            @NonNull
            @Override
            public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_layout, parent, false);
                return new UsersViewHolder(mView);
            }
        };

        mUsersRecyclerView.setAdapter(firebaseRecyclerAdapter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseRecyclerAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseRecyclerAdapter.stopListening();
    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public UsersViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
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

            Picasso.get().load(image_thumb)
                    .networkPolicy(NetworkPolicy.OFFLINE)
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
    }
}
