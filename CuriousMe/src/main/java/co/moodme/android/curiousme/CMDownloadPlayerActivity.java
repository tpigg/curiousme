/*
 * SimplePlayer
 * Android example of Panframe library
 * The example plays back an panoramic movie from a resource.
 * 
 * (c) 2012-2013 Mindlight. All rights reserved.
 * Visit www.panframe.com for more information. 
 * 
 */

package co.moodme.android.curiousme;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.panframe.android.lib.PFAsset;
import com.panframe.android.lib.PFAssetObserver;
import com.panframe.android.lib.PFAssetStatus;
import com.panframe.android.lib.PFNavigationMode;
import com.panframe.android.lib.PFObjectFactory;
import com.panframe.android.lib.PFView;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class CMDownloadPlayerActivity extends FragmentActivity implements PFAssetObserver, OnSeekBarChangeListener {

	PFView				_pfview;
	PFAsset 			_pfasset;
    PFNavigationMode 	_currentNavigationMode = PFNavigationMode.MOTION;
	
	boolean 			_updateThumb = true;;
    Timer 				_scrubberMonitorTimer;    

    ViewGroup 			_frameContainer;
	Button				_stopButton;
	Button				_playButton;
	Button				_touchButton;
	SeekBar				_scrubber;
	ProgressBar			_progress1;
	ProgressBar			_progress2;

	/* Shaker stuff */
	private Vibrator vibrator;
	private ShakeListener mShaker;
	final Context context = this;

	/**
	 * Creation and initalization of the Activitiy.
	 * Initializes variables, listeners, and starts request of a movie list.
	 *
	 * @param  savedInstanceState  a saved instance of the Bundle
	 */
	public void onCreate(Bundle savedInstanceState) {        
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);                
        setContentView(R.layout.activity_main);
        
        _frameContainer = (ViewGroup) findViewById(R.id.framecontainer);
        _frameContainer.setBackgroundColor(0xFF000000);
        
		_playButton = (Button)findViewById(R.id.playbutton);     
		_stopButton = (Button)findViewById(R.id.stopbutton);      
		_touchButton = (Button)findViewById(R.id.touchbutton);        
		_scrubber = (SeekBar)findViewById(R.id.scrubber); 
		_progress1 = (ProgressBar)findViewById(R.id.progressBar1);        
		_progress2 = (ProgressBar)findViewById(R.id.progressBar2); 
		
		_playButton.setOnClickListener(playListener);               
		_stopButton.setOnClickListener(stopListener);        		
		_touchButton.setOnClickListener(touchListener);         
		_scrubber.setOnSeekBarChangeListener(this);
		
		_scrubber.setEnabled(false);
		
		showControls(false);
		showDownloadProgress(true);		
		
    	/* Shaker stuff */
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		mShaker = new ShakeListener(this);
		mShaker.setOnShakeListener(new ShakeListener.OnShakeListener() {
			public void onShake() {
				vibrator.vibrate(100);

				Random r = new Random();
				int randomVideoCode = r.nextInt(3 - 1) + 1;

				loadVideo("http://www.moodme.co/curiousme/content/" + randomVideoCode + ".mp4");
			}
		});

	}
   
	/**
	 * Show/Hide the playback controls
	 *
	 * @param  bShow  Show or hide the controls. Pass either true or false.
	 */
    public void showControls(boolean bShow)
    {
    	int visibility = View.GONE;
    	
    	if (bShow)
    		visibility = View.VISIBLE;
    		
		_playButton.setVisibility(visibility);
		_stopButton.setVisibility(visibility);
		_touchButton.setVisibility(visibility);		
		_scrubber.setVisibility(visibility);		
		
		if (_pfview != null)
		{
			if (!_pfview.supportsNavigationMode(PFNavigationMode.MOTION))
				_touchButton.setVisibility(View.GONE);
		}		
    }
    
	/**
	 * Show/Hide the donload progress
	 *
	 * @param  bShow  Show or hide the progress indicators. Pass either true or false.
	 */
    public void showDownloadProgress(boolean bShow)
    {
    	int visibility = View.GONE;
    	
    	if (bShow)
    		visibility = View.VISIBLE;
    		
    	_progress1.setVisibility(visibility);
    	_progress2.setVisibility(visibility);
    }

    
	/**
	 * Start the video with a local file path
	 *
	 * @param  filename  The file path on device storage
	 */
    public void loadVideo(String filename)
    {
		if (_pfview != null && _pfview.getView() != null && _pfasset != null) {
			_pfasset.stop();
			_frameContainer.removeView(_pfview.getView());
		}

        _pfview = PFObjectFactory.view(this);               
        _pfasset = PFObjectFactory.assetFromUrl(this, filename, this);
        
        _pfview.displayAsset(_pfasset);
        _pfview.setNavigationMode(_currentNavigationMode);
		_pfview.setMode(2, 0);

        _frameContainer.addView(_pfview.getView(), 0);
    }
	
	/**
	 * Status callback from the PFAsset instance.
	 * Based on the status this function selects the appropriate action.
	 *
	 * @param  asset  The asset who is calling the function
	 * @param  status The current status of the asset.
	 */
	public void onStatusMessage(final PFAsset asset, PFAssetStatus status) {
		switch (status)
		{
			case LOADED:
				Log.d("SimplePlayer", "Loaded");
				break;
			case DOWNLOADING:
				Log.d("SimplePlayer", "Downloading 360-degree movie: "+_pfasset.getDownloadProgress()+" percent complete");
				_progress1.setMax(100);
				_progress2.setMax(100);
				_progress1.setProgress(_pfasset.getDownloadProgress());
				_progress2.setProgress(_pfasset.getDownloadProgress());
				break;
			case DOWNLOADED:
				Log.d("SimplePlayer", "Downloaded to "+asset.getUrl());
				showDownloadProgress(false);		
				showControls(true);		
				break;
			case DOWNLOADCANCELLED:
				Log.d("SimplePlayer", "Download cancelled");
				break;
			case PLAYING:
				Log.d("SimplePlayer", "Playing");
		        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				_scrubber.setEnabled(true);
				_scrubber.setMax((int) asset.getDuration());
				_playButton.setText("pause");
				_scrubberMonitorTimer = new Timer();				
				final TimerTask task = new TimerTask() {
					public void run() {
						if (_updateThumb)
							_scrubber.setProgress((int) asset.getPlaybackTime());						
					}
				};
				_scrubberMonitorTimer.schedule(task, 0, 33);
				break;
			case PAUSED:
				Log.d("SimplePlayer", "Paused");
				_playButton.setText("play");
				break;
			case STOPPED:
				Log.d("SimplePlayer", "Stopped");
				_playButton.setText("play");
				_scrubberMonitorTimer.cancel();
				_scrubberMonitorTimer = null;
				_scrubber.setProgress(0);
				_scrubber.setEnabled(false);
		        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				break;
			case COMPLETE:
				Log.d("SimplePlayer", "Complete");
				_playButton.setText("play");
				_scrubberMonitorTimer.cancel();
				_scrubberMonitorTimer = null;
		        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				break;
			case ERROR:
				Log.d("SimplePlayer", "Error");
				break;
		}
	}
	
	/**
	 * Click listener for the play/pause button
	 *
	 */
	private OnClickListener playListener = new OnClickListener() {
		public void onClick(View v) {
			if (_pfasset.getStatus() == PFAssetStatus.PLAYING)
			{
				_pfasset.pause();
			}
			else
				_pfasset.play();
		}
	};
    
	/**
	 * Click listener for the stop/back button
	 *
	 */
	private OnClickListener stopListener = new OnClickListener() {
		public void onClick(View v) {
			_pfasset.stop();
		}
	};

	/**
	 * Click listener for the navigation mode (touch/motion (if available))
	 *
	 */
	private OnClickListener touchListener = new OnClickListener() {
		public void onClick(View v) {
			if (_pfview != null)
			{
				Button touchButton = (Button)findViewById(R.id.touchbutton);    
				if (_currentNavigationMode == PFNavigationMode.TOUCH)
				{
					_currentNavigationMode = PFNavigationMode.MOTION;
					touchButton.setText("motion");
				}
				else
				{
					_currentNavigationMode = PFNavigationMode.TOUCH;
					touchButton.setText("touch");
				}
				_pfview.setNavigationMode(_currentNavigationMode);
			}
		}
	};
		
	/**
	 * Setup the options menu
	 *
	 * @param menu The options menu
	 */
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	/**
	 * Called when pausing the app.
	 * This function pauses the playback of the asset when it is playing.
	 *
	 */
    public void onPause() {
        super.onPause(); 
        if (_pfasset != null)
        {
	        if (_pfasset.getStatus() == PFAssetStatus.PLAYING)
	        	_pfasset.pause();
        }
    }

	/**
	 * Called when a previously created loader is being reset, and thus making its data unavailable.
	 * 
	 * @param seekbar The SeekBar whose progress has changed
	 * @param progress The current progress level.
	 * @param fromUser True if the progress change was initiated by the user.
	 * 
	 */
	public void onProgressChanged (SeekBar seekbar, int progress, boolean fromUser) {
	}

	/**
	 * Notification that the user has started a touch gesture.
	 * In this function we signal the timer not to update the playback thumb while we are adjusting it.
	 * 
	 * @param seekbar The SeekBar in which the touch gesture began
	 * 
	 */
	public void onStartTrackingTouch(SeekBar seekbar) {
		_updateThumb = false;
	}

	/**
	 * Notification that the user has finished a touch gesture. 
	 * In this function we request the asset to seek until a specific time and signal the timer to resume the update of the playback thumb based on playback.
	 * 
	 * @param seekbar The SeekBar in which the touch gesture began
	 * 
	 */
	public void onStopTrackingTouch(SeekBar seekbar) {
		_pfasset.setPLaybackTime(seekbar.getProgress());
		_updateThumb = true;
	}    
    
}
