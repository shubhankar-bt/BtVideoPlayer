package com.shubhankaranku.bonkuvideoplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;


public class AddVideoActivity extends AppCompatActivity {

    //actionbar
    private ActionBar actionBar;

    //UI views
    private TextInputEditText titleVideo;
    private VideoView videoView;
    private Button uploadVideoButton;
    private FloatingActionButton pickVideo;
    private ProgressDialog progressDialog;

    private static final int VIDEO_PICK_GALLERY_CODE = 100;
    private static final int VIDEO_PICK_CAMERA_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;

    private String[] cameraPermissions;
    private String title;

    private Uri videoUri = null; //uri of picked video


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_video);

        //init actionbar
        actionBar = getSupportActionBar();
        //title
        actionBar.setTitle("Add New Video");
        //add Back Button
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //init ui views
        titleVideo = findViewById(R.id.titleVideo);
        videoView = findViewById(R.id.videoView);
        uploadVideoButton = findViewById(R.id.uploadVideoButton);
        pickVideo = findViewById(R.id.pickVideo);

        //set up progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Uploading Video");
        progressDialog.setCanceledOnTouchOutside(false);



        //camera permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //Handle click, upload video
        uploadVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                title = titleVideo.getText().toString().trim();
                if (TextUtils.isEmpty(title)){
                    Snackbar.make(v, "Title is required...", Snackbar.LENGTH_SHORT).show();
                }else if (videoUri == null){
                    //video is not picked
                    Snackbar.make(v, "Pick a video before you can upload...", Snackbar.LENGTH_SHORT).show();
                }else {
                    //upload video function call
                    uploadVideoToFirebase();
                }
            }
        });

        //Handle click, pick video
        pickVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoPickDialog();
            }
        });

    }

    private void uploadVideoToFirebase() {
        //show progress
        progressDialog.show();

        //timestamp
        String timestamp = "" + System.currentTimeMillis();

        //file path and name in firebase storage
        String filePathAndName = "Videos/" + "video_"+timestamp;

        //storage reference
        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
        //upload video
        storageReference.putFile(videoUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //video uploaded, get url of uploaded of video
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        Uri downloadUri = uriTask.getResult();
                        if (uriTask.isSuccessful()){
                            //url of uploaded video is received
                            //now we can add video details to our database
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("id", "" + timestamp);
                            hashMap.put("title", "" + title);
                            hashMap.put("timestamp", "" + timestamp);
                            hashMap.put("videoUrl", "" + downloadUri);

                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Videos");
                            reference.child(timestamp)
                                    .setValue(hashMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            //video uploaded to db
                                            progressDialog.dismiss();
                                            Toast.makeText(AddVideoActivity.this, "Video Uploaded...", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //failing adding details to db
                                            progressDialog.dismiss();
                                            Toast.makeText(AddVideoActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed uploading to storage
                        Toast.makeText(AddVideoActivity.this, ""+e.getMessage() , Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });


    }

    private void videoPickDialog() {
        //options to display in dialog
        String[] options = {"Camera", "Gallery"};

        //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Video From")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        if (i == 0){
                            //camera clicked
                            if (!checkCameraPermission()){
                                //camera permission not allowed, request it
                                requestCameraPermission();
                            }else {
                                //permission already allowed, take video
                                videoPickCamera();
                            }

                        }else if (i ==1){
                            //gallery clicked
                            videoPickGallery();
                        }
                    }
                })
                .show();
    }

    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean result2 = ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_DENIED;

        return result1 && result2;
    }

    private void videoPickGallery(){
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Videos"), VIDEO_PICK_GALLERY_CODE);
    }

    private void videoPickCamera(){
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(intent, VIDEO_PICK_CAMERA_CODE);
    }
    private void setVideoToVideoView(){
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);

        //set media controller to video view
        videoView.setMediaController(mediaController);

        //set video uri
        videoView.setVideoURI(videoUri);
        videoView.requestFocus();
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoView.pause();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0){
                    //check permission is allowed or not
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted){
                        //both permission allowed
                        videoPickCamera();
                    }else {
                        //both or one of them denied
                        Toast.makeText(this, "Camera and Storage Permissions are required", Toast.LENGTH_SHORT).show();
                    }

                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //called after picking video from camera/gallery
        if (resultCode == RESULT_OK){
            if (requestCode == VIDEO_PICK_GALLERY_CODE){
                videoUri = data.getData();
                //show picked video in video view
                setVideoToVideoView();
            }else if (requestCode == VIDEO_PICK_CAMERA_CODE){
                videoUri = data.getData();
                setVideoToVideoView();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();//go to previous activity by pressing back button on action bar
        return super.onSupportNavigateUp();
    }
}