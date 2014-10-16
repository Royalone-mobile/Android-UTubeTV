package com.klogicapps.tv.activities;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;

import com.keyes.youtube.OpenYouTubePlayerActivity;

public class youtubeActivity extends OpenYouTubePlayerActivity{
	
	private ActionBar actionBar; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
  
		super.onCreate(savedInstanceState);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
  
		actionBar = getActionBar();
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true); 
		actionBar.setTitle(""); 
		


		
	}
	
	
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
