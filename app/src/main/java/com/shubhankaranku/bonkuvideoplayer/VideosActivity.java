package com.shubhankaranku.bonkuvideoplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import com.bumptech.glide.RequestManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class VideosActivity extends AppCompatActivity {

    //UI views
    private ActionBar actionBar;
    FloatingActionButton addVideosButton;
    private RecyclerView videosRv;
    //array list
    private ArrayList<ModelVideo> videoArrayList;
    //adapter
    private AdapterVideo adapterVideo;
    private ProgressBar progressbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videos);


        actionBar = getSupportActionBar();
        //title
        actionBar.setTitle("Bt Video Player");



        //Initializing Ui views
        addVideosButton = findViewById(R.id.addVideosButton);
        videosRv = findViewById(R.id.videosRv);
        progressbar = findViewById(R.id.progressbar);

        //function call, load videos
        loadVideosFromFirebase();
        progressbar.setVisibility(View.VISIBLE);


        //handle click events
        addVideosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(VideosActivity.this, AddVideoActivity.class));
            }
        });
    }



    private void loadVideosFromFirebase() {
        //init array list
        videoArrayList = new ArrayList<>();

        //db reference
        progressbar.setVisibility(View.GONE);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Videos");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //clear list before adding data into it
                for (DataSnapshot ds : snapshot.getChildren()) {
                    //get Data
                    ModelVideo modelVideo = ds.getValue(ModelVideo.class);
                    //add model/data into the list
                    videoArrayList.add(modelVideo);
                    progressbar.setVisibility(View.GONE);

                }
                //setup adapter
                adapterVideo = new AdapterVideo(VideosActivity.this, videoArrayList);
                //set adapter to recyclerview
                videosRv.setAdapter(adapterVideo);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressbar.setVisibility(View.GONE);

            }
        });
    }

    //initialising searchBar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        // Retrieve the SearchView and plug it into SearchManager
        final androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                if(videoArrayList.contains(query)){
                    adapterVideo.getFilter();
                }else{
                    Toast.makeText(VideosActivity.this, "No Match found",Toast.LENGTH_LONG).show();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }







}