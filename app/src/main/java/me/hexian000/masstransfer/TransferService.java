package me.hexian000.masstransfer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.v4.provider.DocumentFile;
import android.widget.Toast;

import static me.hexian000.masstransfer.TransferApp.CHANNEL_TRANSFER_STATE;

public abstract class TransferService extends Service {
	protected DocumentFile root = null;
	protected Handler handler = new Handler();
	protected Notification.Builder builder;
	protected NotificationManager notificationManager = null;
	protected Thread thread = null;
	protected int startId = 0;
	protected boolean result = false;


	protected void initNotification(@StringRes int title) {
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		if (builder == null) {
			builder = new Notification.Builder(this.getApplicationContext());
		}

		builder.setContentIntent(null)
				.setContentTitle(getResources().getString(title))
				.setSmallIcon(R.drawable.ic_send_black_24dp)
				.setWhen(System.currentTimeMillis())
				.setProgress(0, 0, true)
				.setOngoing(true)
				.setVisibility(Notification.VISIBILITY_PUBLIC);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			// Android 8.0+
			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			if (manager != null) {
				TransferApp.createNotificationChannels(manager, getResources());
				builder.setChannelId(CHANNEL_TRANSFER_STATE);
			}
		} else {
			// Android 7.1
			builder.setPriority(Notification.PRIORITY_DEFAULT).setLights(0, 0, 0).setVibrate(null).setSound(null);
		}

		Intent cancel = new Intent(this, SendService.class);
		cancel.setAction("cancel");
		builder.addAction(new Notification.Action.Builder(null, getResources().getString(R.string.cancel),
				PendingIntent.getService(this, startId, cancel, 0)).build())
				.setContentText(getResources().getString(R.string.notification_starting));

		Notification notification = builder.build();
		startForeground(startId, notification);
	}

	protected void stop() {
		if (thread != null) {
			thread.interrupt();
			thread = null;
		}
		notificationManager = null;
		builder = null;
		stopSelf();
	}

	protected void showResultToast() {
		if (result) {
			Toast.makeText(this, R.string.transfer_success, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, R.string.transfer_failed, Toast.LENGTH_SHORT).show();
		}
	}

}
