package com.renatoalmeida.gpxmocklocations;

import java.io.File;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class MockLocationService extends Service
{
	private static final String TAG = MockLocationService.class.getSimpleName();

	/* Message definition */
	public static final int MSG_START = 0x50;
	public static final int MSG_STOP = 0x51;
	public static final int MSG_RESTART = 0x52;

	public static final int MSG_GET_STATE = 0x70;
	public static final int MSG_GET_FILE = 0x71;
	public static final int MSG_GET_MODE = 0x72;

	public static final String BUNDLE_KEY_FILE = "MockLocationService:File";
	public static final String BUNDLE_KEY_MODE = "MockLocationService:Mode";
	public static final String BUNDLE_KEY_STATE = "MockLocationService:State";

	private final Messenger mMessenger = new Messenger(new IncomingHandler());

	private final boolean running = false;
	private boolean loop = false;
	private File fileToLoad = null;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		return START_STICKY;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		Log.i(TAG, "Service Started.");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.i(TAG, "Service Stopped.");
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return mMessenger.getBinder();
	}

	private class IncomingHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what) {

			case MSG_START:

				Bundle b = msg.getData();
				fileToLoad = new File(b.getString(BUNDLE_KEY_FILE));
				//TODO Should verify if file exists? but it's internal service...

				loop = !b.getBoolean(BUNDLE_KEY_MODE);

				Toast.makeText(getApplicationContext(), "Start " + fileToLoad + " " + loop, Toast.LENGTH_SHORT).show();

				break;
			case MSG_STOP:
				Toast.makeText(getApplicationContext(), "Stop", Toast.LENGTH_SHORT).show();
				break;
			case MSG_RESTART:
				Toast.makeText(getApplicationContext(), "Restart", Toast.LENGTH_SHORT).show();
				break;
			case MSG_GET_STATE:
				Toast.makeText(getApplicationContext(), "Get State", Toast.LENGTH_SHORT).show();

				int status = (running) ? R.string.activity_main_status_running : R.string.activity_main_status_stopped;

				Message response = Message.obtain(null, MockLocationService.MSG_GET_STATE, status, 0);

				try {
					msg.replyTo.send(response);
				} catch (RemoteException e) {
					e.printStackTrace();
				}

				break;

			default:
				super.handleMessage(msg);
			}
		}
	}
}
