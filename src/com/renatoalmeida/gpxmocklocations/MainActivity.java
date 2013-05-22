package com.renatoalmeida.gpxmocklocations;

import android.app.Activity;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity
{
	private static final String TAG = MainActivity.class.getSimpleName();

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

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		status = (TextView) findViewById(R.id.activity_main_status);
		Button start = (Button) findViewById(R.id.activity_main_start);
		Button stop = (Button) findViewById(R.id.activity_main_stop);
		Button restart = (Button) findViewById(R.id.activity_main_restart);

		start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				Message msg = Message.obtain(null, MockLocationService.MSG_START, 0, 0);

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
