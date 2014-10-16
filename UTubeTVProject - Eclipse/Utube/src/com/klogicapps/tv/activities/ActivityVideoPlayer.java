package com.klogicapps.tv.activities;


import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.VideoView;

import com.klogicapps.tv.R;

public class ActivityVideoPlayer extends Activity {

	public final static String URL = "URL";
	boolean activitySwitchFlag = false;
	int androidSDK = Build.VERSION.SDK_INT; 

	// Progress Dialog
	private ProgressDialog pDialog;
	@SuppressWarnings("unused")
	private Context ctx = null;
	private ActionBar actionBar; 
	private static String videoURL;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Remove Screenshots from the Task Manager
		// TODO Add screenshot option in settings
		/* getWindow().setFlags(LayoutParams.FLAG_SECURE,
				LayoutParams.FLAG_SECURE); */  

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_video_player);

		// ApplicationCore.mContext = this;

		actionBar = getActionBar();
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true); 
		actionBar.setTitle(""); 

		Intent intent = getIntent();
		videoURL = intent.getStringExtra("VIDEO");
		
		Log.d("Video: ",videoURL); 

		final VideoView videoView = (VideoView) findViewById(R.id.videoView1);

		try { 

			videoView.setVideoURI(Uri.parse(videoURL));
			// videoView.setVideoPath(url);
		}
		catch (IllegalArgumentException ex) { 
			ex.printStackTrace();
		} catch (SecurityException ex) { 
			ex.printStackTrace();
		} catch (IllegalStateException ex) { 
			ex.printStackTrace();
		} 
		MediaController mediaController = new MediaController(this);
		mediaController.setAnchorView(videoView);
		videoView.setMediaController(mediaController);

		videoView.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				// mp.release();
				Log.d("Video", "Media player has completed playing");
				// mp.setVideoURI(Uri.parse(videoUrlLow));

				try { 
					videoView.setVideoURI(Uri.parse(videoURL));
					// video.setVideoPath(videoUrlLow);
				}
				catch (IllegalArgumentException ex) { 
					ex.printStackTrace();
				} catch (SecurityException ex) { 
					ex.printStackTrace();
				} catch (IllegalStateException ex) { 
					ex.printStackTrace();
				} 
				// mp.start(); 
			}
		});

		videoView.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				// TODO Auto-generated method stub
				// Toast.makeText(ctx, "Error occured", Toast.LENGTH_SHORT).show();
				return true;
			}
		});

		videoView.setOnPreparedListener(new OnPreparedListener() {
			public void onPrepared(MediaPlayer mp) { 
				pDialog.dismiss();
				// videoView.start();
				try { 
					mp.setLooping(true);
					mp.start(); 
				}
				catch (IllegalArgumentException ex) { 
					ex.printStackTrace();
				} catch (SecurityException ex) { 
					ex.printStackTrace();
				} catch (IllegalStateException ex) { 
					ex.printStackTrace();
				} 
			}
		});

		showDialog("Loading video..."); 	
	}


	private void showDialog(String msg) {  
		pDialog = new ProgressDialog(ActivityVideoPlayer.this);
		pDialog.setMessage(msg);
		pDialog.setIndeterminate(true);
		pDialog.setCancelable(false);
		pDialog.show();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			if (actionBar != null) {
				actionBar.hide(); 
			}

		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			if (actionBar != null) {
				actionBar.show(); 
			}
		}
	}
	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_videoplayer, menu);
		return super.onCreateOptionsMenu(menu);
	}*/

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			finish();
			return true;
		}/*
		else {
			return Downloader.OptionsItemSelected(
					item,
					videoURL,
					ApplicationCore.mContext,
					super.onOptionsItemSelected(item));
		}*/
		else { return super.onOptionsItemSelected(item); }
	}



}