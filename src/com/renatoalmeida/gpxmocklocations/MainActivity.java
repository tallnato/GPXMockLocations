package com.renatoalmeida.gpxmocklocations;

import java.io.File;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;

public class MainActivity extends Activity
{
	private static final String TAG = MainActivity.class.getSimpleName();

	private static final int REQUEST_CODE = 1234;
	private static final String CHOOSER_TITLE = "Select a GPX file";

	/**
	 * Messenger for communicating with service.
	 */
	Messenger mService = null;

	/**
	 * Flag indicating whether we have called bind on the service.
	 */
	boolean mIsBound;

	private final Messenger mMessenger = new Messenger(new IncomingHandler());

	private TextView status;
	private EditText edit;
	private RadioButton singleRun;
	private RadioButton loop;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		status = (TextView) findViewById(R.id.activity_main_status);
		Button start = (Button) findViewById(R.id.activity_main_start);
		Button stop = (Button) findViewById(R.id.activity_main_stop);
		Button restart = (Button) findViewById(R.id.activity_main_restart);
		edit = (EditText) findViewById(R.id.activity_main_editbox_file);
		singleRun = (RadioButton) findViewById(R.id.activity_main_mode_single_run);
		loop = (RadioButton) findViewById(R.id.activity_main_mode_single_loop);

		start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				if(edit.getText().length() == 0) {
					Toast.makeText(getApplicationContext(), R.string.activity_main_choose_no_file, Toast.LENGTH_LONG).show();
					return;
				}

				Message msg = Message.obtain(null, MockLocationService.MSG_START, 0, 0);
				Bundle b = new Bundle();
				b.putBoolean(MockLocationService.BUNDLE_KEY_MODE, singleRun.isChecked());
				b.putString(MockLocationService.BUNDLE_KEY_FILE, edit.getText().toString());

				msg.setData(b);
				try {
					mService.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});

		stop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				Message msg = Message.obtain(null, MockLocationService.MSG_STOP);

				try {
					mService.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});

		restart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				Message msg = Message.obtain(null, MockLocationService.MSG_RESTART);

				try {
					mService.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});

		edit.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if(event.getAction() == MotionEvent.ACTION_UP) {
					Intent target = FileUtils.createGetContentIntent();
					Intent intent = Intent.createChooser(target, CHOOSER_TITLE);
					try {
						startActivityForResult(intent, REQUEST_CODE);
					} catch (ActivityNotFoundException e) {
						e.printStackTrace();
					}

					return true;
				}

				return false;
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch(requestCode) {
		case REQUEST_CODE:
			if(resultCode == RESULT_OK) {
				// Create a File from this Uri
				File file = FileUtils.getFile(data.getData());

				if(!file.exists()) {
					Toast.makeText(getApplicationContext(), R.string.activity_main_choose_file_inexistent, Toast.LENGTH_LONG).show();
					return;
				}

				if(!file.canRead()) {
					Toast.makeText(getApplicationContext(), R.string.activity_main_choose_file_cannot_read, Toast.LENGTH_LONG).show();
					return;
				}

				String filenameArray[] = file.getName().split("\\.");
				if(!filenameArray[filenameArray.length - 1].equals("gpx")) {
					Toast.makeText(getApplicationContext(), R.string.activity_main_choose_file_not_gpx, Toast.LENGTH_LONG).show();
					return;
				}

				EditText edit = (EditText) findViewById(R.id.activity_main_editbox_file);
				edit.setText(file.getAbsolutePath());
			}
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		doBindService();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		doUnbindService();
	}

	void doBindService()
	{
		Intent i = new Intent(MainActivity.this, MockLocationService.class);
		startService(i);
		bindService(i, mConnection, Context.BIND_AUTO_CREATE);
	}

	void doUnbindService()
	{
		if(mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	private class IncomingHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what) {
			case MockLocationService.MSG_GET_STATE:

				status.setText(msg.arg1);
				Bundle b = msg.getData();

				String file = b.getString(MockLocationService.BUNDLE_KEY_FILE);
				if(file != null)
					edit.setText(file);

				if(b.getBoolean(MockLocationService.BUNDLE_KEY_MODE, true))
					singleRun.setChecked(true);
				else
					loop.setChecked(true);

				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void getState()
	{
		Message msg = Message.obtain(null, MockLocationService.MSG_GET_STATE, 0, 0);
		msg.replyTo = mMessenger;
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private final ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mService = new Messenger(service);
			mIsBound = true;
			status.setText(R.string.activity_main_status_binded);

			getState();
		}

		@Override
		public void onServiceDisconnected(ComponentName className)
		{
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;
			mIsBound = false;
			status.setText(R.string.activity_main_status_unbinded);
		}
	};
}
