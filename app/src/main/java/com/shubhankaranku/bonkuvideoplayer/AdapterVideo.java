package com.shubhankaranku.bonkuvideoplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.internal.Objects;
import com.steelkiwi.library.SlidingSquareLoaderView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AdapterVideo extends RecyclerView.Adapter<AdapterVideo.HolderVideo>{

    //context
    private Context context;

    //arrayList
    private ArrayList<ModelVideo> videoArrayList;
    private ArrayList<ModelVideo> itemsModelListFiltered;


    //constructor

    public AdapterVideo(Context context, ArrayList<ModelVideo> videoArrayList) {
        this.context = context;
        this.videoArrayList = videoArrayList;
        this.itemsModelListFiltered = videoArrayList;

    }

    @NonNull
    @Override
    public HolderVideo onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout row_videos.menu
        View view = LayoutInflater.from(context).inflate(R.layout.row_videos, parent, false);
        return new HolderVideo(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderVideo holder, int position) {
        /*--------Get,set data, format, handle clicks etc--------*/

        //Get data
        ModelVideo modelVideo = videoArrayList.get(position);


        String id = modelVideo.getId();
        String title = modelVideo.getTitle();
        String timestamp = modelVideo.getTimestamp();
        String videoUrl = modelVideo.getVideoUrl();

        //format timestamp
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        String formatDateTime  = DateFormat.format("dd/MM/yyyy K:mm a", calendar).toString();

        //setData
        holder.titleTv.setText(title);
        holder.timeTv.setText(formatDateTime);

        setVideoUrl(modelVideo, holder);



    }


    private void setVideoUrl(ModelVideo modelVideo, HolderVideo holder) {
        //show progress
        holder.progressbar.setVisibility(View.VISIBLE);

        //get video url
        String videoUrl = modelVideo.getVideoUrl();

        //MediaController for play, pause, seekbar, timer etc..
        MediaController mediaController = new MediaController(context);
        mediaController.setAnchorView(holder.videoView);

        Uri videoUri = Uri.parse(videoUrl);
        holder.videoView.setMediaController(mediaController);
        holder.videoView.setVideoURI(videoUri);

        holder.videoView.requestFocus();
        holder.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                //video is ready to play
                mp.start();
                holder.progressbar.setVisibility(View.GONE);

            }
        });
        holder.videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                //to check if buffering, rendering etc.
                switch (what){
                    case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:{
                        //rendering started
                        return true;
                    }
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:{
                        //buffering started
                        holder.progressbar.setVisibility(View.VISIBLE);
                        return true;
                    }
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:{
                        holder.progressbar.setVisibility(View.GONE);
                        return true;
                    }

                }

                return false;
            }
        });
        holder.videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.start(); //restart video if completed
            }
        });


    }

    @Override
    public int getItemCount() {
        return videoArrayList.size();
    }

    //view holder class, holds, inits the UI views
    class HolderVideo extends RecyclerView.ViewHolder{

        //UI view of row_videos.menu
        VideoView videoView;
        TextView titleTv, timeTv;
        ProgressBar progressbar;
        ImageView imageView;


        public HolderVideo(@NonNull View itemView) {
            super(itemView);

            //init Ui videos of row_videos.menu
            videoView = itemView.findViewById(R.id.videoView2);
            titleTv = itemView.findViewById(R.id.titleTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            progressbar = itemView.findViewById(R.id.progressbar);




        }
    }

    //get filter part for the search result

    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults filterResults = new FilterResults();
                if(constraint == null || constraint.length() == 0){
                    filterResults.count = videoArrayList.size();
                    filterResults.values = videoArrayList;

                }else{
                    List<ModelVideo> resultsModel = new ArrayList<>();
                    String searchStr = constraint.toString().toLowerCase();

                    for(ModelVideo itemsModel:videoArrayList){
                        if(itemsModel.getTitle().contains(searchStr) || itemsModel.getTimestamp().contains(searchStr)){
                            resultsModel.add(itemsModel);

                        }
                        filterResults.count = resultsModel.size();
                        filterResults.values = resultsModel;
                    }


                }

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                itemsModelListFiltered = (ArrayList<ModelVideo>) results.values;
                notifyDataSetChanged();

            }
        };
        return filter;
    }

}
