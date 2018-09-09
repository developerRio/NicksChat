package com.originalstocksllc.himanshuraj.nicktest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class MyProfileActivity extends AppCompatActivity {

    private static final String GOOGLE_PHOTOS_PACKAGE_NAME = "com.google.android.apps.photos";
    private Button mLogoutButton, editStatusButton, changeProfileImageButton;
    private FirebaseAuth mAuth;
    private TextView nameText, statusText, emailText;
    private CircleImageView profileImage;
    private int PICK_IMAGE_REQUEST = 1;

    // DatabaseRef
    private DatabaseReference mUserDatabase;
    private FirebaseUser firebaseUser;

    private byte[] thumb_byte;

    //Storage firebase
    private StorageReference mImageStorage;

    public static String dateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT);
        String currentDateandTime = sdf.format(new Date());
        return currentDateandTime;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        mAuth = FirebaseAuth.getInstance();
        mImageStorage = FirebaseStorage.getInstance().getReference();

        mLogoutButton = findViewById(R.id.button_logout);
        nameText = findViewById(R.id.user_name);
        editStatusButton = findViewById(R.id.edit_status_button);
        changeProfileImageButton = findViewById(R.id.change_image_button);
        profileImage = findViewById(R.id.profile_image);
        statusText = findViewById(R.id.user_status);

        // Get user stuff from fire_base database :

        firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {

            String current_Uid = firebaseUser.getUid();
            mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(current_Uid);

            mUserDatabase.keepSynced(true);

            mUserDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    String mName = dataSnapshot.child("name").getValue().toString();
                    final String mImage = dataSnapshot.child("image").getValue().toString();
                    String mEmail = dataSnapshot.child("email").getValue().toString();
                    String mStatus = dataSnapshot.child("status").getValue().toString();
                    String mProfileImage = dataSnapshot.child("profile_image").getValue().toString();

                    if (mName == null || mProfileImage == null || mEmail == null) {
                        Toast.makeText(MyProfileActivity.this, "Fetching data from servers...", Toast.LENGTH_SHORT).show();
                    }
                    nameText.setText(mName);
                    statusText.setText(mStatus);
                    if (!mImage.equals("default")) {

                        //Picasso.get().load(mImage).placeholder(R.drawable.default_user).into(profileImage);

                        Picasso.get().load(mImage)
                                .networkPolicy(NetworkPolicy.OFFLINE)
                                .placeholder(R.drawable.default_user)
                                .into(profileImage, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        // Image is saved in storage as offline.
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Picasso.get().load(mImage).placeholder(R.drawable.default_user).into(profileImage);
                                    }
                                });


                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.i("DatabaseError", "onCancelled: " + databaseError.getMessage());
                }
            });
        }

        editStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), StatusActivity.class));
                finish();
            }
        });

        changeProfileImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //launchGooglePhotosPicker(MyProfileActivity.this);
                launchGalleryPhotosPicker();

            }
        });

        mLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                if (mAuth.getCurrentUser() == null) {
                    updateUI();
                }
            }
        });

    }//onCreate closed

    public void launchGalleryPhotosPicker() {

        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), PICK_IMAGE_REQUEST);

    }

    public void launchGooglePhotosPicker(Activity callingActivity) {
        if (callingActivity != null) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_PICK);
            intent.setType("image/*");
            List<ResolveInfo> resolveInfoList = callingActivity.getPackageManager().queryIntentActivities(intent, 0);
            for (int i = 0; i < resolveInfoList.size(); i++) {
                if (resolveInfoList.get(i) != null) {
                    String packageName = resolveInfoList.get(i).activityInfo.packageName;
                    if (GOOGLE_PHOTOS_PACKAGE_NAME.equals(packageName)) {
                        intent.setComponent(new ComponentName(packageName, resolveInfoList.get(i).activityInfo.name));
                        callingActivity.startActivityForResult(intent, PICK_IMAGE_REQUEST);
                        return;
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // for google photos
       /* if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                // Log.d(TAG, String.valueOf(bitmap));
                profileImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/

        // for gallery photos

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                Uri resultUri = result.getUri();
                // upload cropped image on firebase
                File thumbFilePath = new File(resultUri.getPath());

                String current_uid = firebaseUser.getUid();

                // compressing image
                try {
                    Bitmap thumb_Bitmap = new Compressor(this)
                            .setMaxHeight(200)
                            .setMaxWidth(200)
                            .setQuality(75)
                            .compressToBitmap(thumbFilePath);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thumb_Bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    thumb_byte = baos.toByteArray();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                final StorageReference thumb_filePathRef = mImageStorage.child("nicks_profile_images").child("thumbs").child(current_uid + ".jpg");

                final StorageReference filePath = mImageStorage.child("nicks_profile_images").child(current_uid + ".jpg");
                Toast.makeText(MyProfileActivity.this, "Uploading your Image...", Toast.LENGTH_LONG).show();

                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {

                            UploadTask uploadTask = thumb_filePathRef.putBytes(thumb_byte);

                            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {

                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumbTask) {


                                    if (thumbTask.isSuccessful()) {

                                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                final String downloadImageUrl = uri.toString();
                                                //Log.i("downloadImageUrl", "onSuccess: " + downloadImageUrl);

                                                thumb_filePathRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                    @Override
                                                    public void onSuccess(Uri uri) {
                                                        String thumb_downloadedUrl = uri.toString();

                                                        //Log.i("THUMBNAIL_URL", "onComplete: " + thumb_downloadedUrl);

                                                        Map update_HashMap = new HashMap<>();
                                                        update_HashMap.put("image", downloadImageUrl);
                                                        update_HashMap.put("image_thumb", thumb_downloadedUrl);


                                                        mUserDatabase.updateChildren(update_HashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                Toast.makeText(MyProfileActivity.this, "Successfully updated...", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });

                                                    }
                                                });
                                            }
                                        });
                                    } else {
                                        Toast.makeText(MyProfileActivity.this, "Error in uploading thumbnail", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });


                        } else {
                            Toast.makeText(MyProfileActivity.this, "Error in uploading image", Toast.LENGTH_LONG).show();
                        }
                    }
                });

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri imgUri = data.getData();
            CropImage.activity(imgUri)
                    .setAspectRatio(1, 1)
                    .start(this);


        }
    }

    private void updateUI() {
        Snackbar.make(findViewById(R.id.profile_container), "You've successfully logged out.", Snackbar.LENGTH_SHORT).show();
        Intent accIntent = new Intent(MyProfileActivity.this, LoginUserActivity.class);
        accIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(accIntent);
        finish();
    }

}
