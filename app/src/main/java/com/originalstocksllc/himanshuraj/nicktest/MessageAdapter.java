package com.originalstocksllc.himanshuraj.nicktest;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter {

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private Context mContext;
    private List<Messages> mMessagesList;
    private DatabaseReference mUserDatabase;
    private FirebaseAuth mAuth;
    private String name;
    private String image;

    public MessageAdapter(Context context, List<Messages> mMessagesList) {
        this.mContext = context;
        this.mMessagesList = mMessagesList;
    }

    @Override
    public int getItemViewType(int position) {
        mAuth = FirebaseAuth.getInstance();
        String currentUserId = mAuth.getCurrentUser().getUid();

        if (mMessagesList.get(position).getFrom().equals(currentUserId)) {

            // If the current user is the sender of the message

            return VIEW_TYPE_MESSAGE_SENT;
        } else {

            //If some other user sent the message

            return VIEW_TYPE_MESSAGE_RECEIVED;
        }

    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {

        mAuth = FirebaseAuth.getInstance();
        String currentUserId = mAuth.getCurrentUser().getUid();


        switch (holder.getItemViewType()) {

            case VIEW_TYPE_MESSAGE_SENT:
                final SentMessageHolder sentMessageHolder = (SentMessageHolder) holder;

                final Messages senderMessages = mMessagesList.get(position);
                final String senderMessageType = senderMessages.getType();

                String byUser = senderMessages.getFrom();
                mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(byUser);
                mUserDatabase.keepSynced(true);

                mUserDatabase.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        long time = senderMessages.getTime();
                        String dateString = new SimpleDateFormat("HH:mm").format(new Date(time));
                        sentMessageHolder.txTimeText.setText(dateString);


                        if (senderMessageType.equals("text")) {
                            sentMessageHolder.txMessageText.setText(senderMessages.getMessage());

                        } else {// Image for now
                            sentMessageHolder.txMessageText.setVisibility(View.INVISIBLE);
                            Picasso.get().load(senderMessages.getMessage())
                                    .placeholder(R.drawable.default_user).into(sentMessageHolder.txImageView);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:

                final Messages messages = mMessagesList.get(position);
                final String messageType = messages.getType();

                String fromUser = messages.getFrom();
                mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUser);
                mUserDatabase.keepSynced(true);

                final ReceivedMessageHolder receivedMessageHolder = (ReceivedMessageHolder) holder;
                mUserDatabase.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        name = dataSnapshot.child("name").getValue().toString();
                        image = dataSnapshot.child("image_thumb").getValue().toString();
                        long time = messages.getTime();
                        String dateString = new SimpleDateFormat("HH:mm").format(new Date(time));
                        receivedMessageHolder.rxTimeText.setText(dateString);
                        receivedMessageHolder.userName.setText(name);
                        Picasso.get().load(image).placeholder(R.drawable.default_user)
                                .into(receivedMessageHolder.messageUserImage);

                        if (messageType.equals("text")) {
                            receivedMessageHolder.messageText.setText(messages.getMessage());
                            //receivedMessageHolder.messageUserImage.setVisibility(View.INVISIBLE);

                        } else {// Image for now
                            receivedMessageHolder.messageText.setVisibility(View.INVISIBLE);
                            Picasso.get().load(messages.getMessage()).placeholder(R.drawable.default_user).into(receivedMessageHolder.messageImage);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                break;

        }
    }

    @Override
    public int getItemCount() {
        return mMessagesList.size();
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {

        TextView messageText;
        CircleImageView messageUserImage;
        TextView userName;
        TextView rxTimeText;
        ImageView messageImage;

        ReceivedMessageHolder(View itemView) {
            super(itemView);

            userName = itemView.findViewById(R.id.rx_message_user_name);
            messageText = itemView.findViewById(R.id.rx_message_texts);
            messageUserImage = itemView.findViewById(R.id.rx_message_image_user);
            messageImage = itemView.findViewById(R.id.rx_message_image);
            rxTimeText = itemView.findViewById(R.id.rx_message_time);
        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {

        TextView txMessageText, txTimeText;
        ImageView txImageView;

        SentMessageHolder(View itemView) {
            super(itemView);

            txMessageText = itemView.findViewById(R.id.tx_message_texts);
            txTimeText = itemView.findViewById(R.id.tx_message_time);
            txImageView = itemView.findViewById(R.id.tx_message_image);

        }
    }


}
