package com.renatoalmeida.gpxmocklocations;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

public class MockLocationService extends Service
{
	private static final String TAG = MockLocationService.class.getSimpleName();

	private final Messenger mMessenger = new Messenger(new IncomingHandler());

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

			default:
				super.handleMessage(msg);
			}
		}
	}
}
