package me.hexian000.masstransfer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
	private static final int REQUEST_OPEN_DOCUMENT_TREE = 421;
	DiscoverService mService;
	Timer timer;
	Handler refresh = new Handler();
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			DiscoverService.Binder binder =
					(DiscoverService.Binder) service;
			mService = binder.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mService = null;
		}
	};

	@Override
	protected void onPause() {
		Intent intent1 = new Intent(MainActivity.this, DiscoverService.class);
		unbindService(mConnection);
		stopService(intent1);
		Log.d(TransferApp.LOG_TAG, "stop DiscoverService");
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		Intent intent1 = new Intent(MainActivity.this, DiscoverService.class);
		bindService(intent1, mConnection, Context.BIND_AUTO_CREATE);
		startService(intent1);
		Log.d(TransferApp.LOG_TAG, "start DiscoverService");

		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			List<String> items = new ArrayList<>();
			ArrayAdapter adapter = null;

			@Override
			public void run() {
				if (adapter == null) {
					refresh.post(() -> {
						ListView peersList = findViewById(R.id.PeersList);
						adapter = new ArrayAdapter<>(
								MainActivity.this,
								android.R.layout.simple_list_item_1,
								items);
						peersList.setAdapter(adapter);
						peersList.setOnItemClickListener((adapterView, view, i, l) -> {
							String s = (String) adapter.getItem(i);
							Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
							pickFolder();
						});
					});
				} else {
					refresh.post(() -> {
						items.clear();
						if (mService != null)
							items.addAll(mService.discoverer.getPeers());
						adapter.notifyDataSetChanged();
					});
				}
			}
		}, 0, 500);

		super.onResume();
	}

	private void pickFolder() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		startActivityForResult(intent, REQUEST_OPEN_DOCUMENT_TREE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && requestCode == REQUEST_OPEN_DOCUMENT_TREE) {
			Uri uriTree = data.getData();
			if (uriTree != null) {
				DocumentFile root = DocumentFile.fromTreeUri(this, uriTree);
				List<DocumentFile> files = new ArrayList<>();
				listTree(root, files);
				StringBuilder sb = new StringBuilder();
				for (DocumentFile file : files) {
					sb.append(file.getName()).append('\n');
				}
				Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
			}
		}
	}

	private void listTree(DocumentFile root, List<DocumentFile> files) {
		if (files.size() > 20) return;
		for (DocumentFile file : root.listFiles()) {
			if (file.isDirectory()) {
				listTree(file, files);
			} else if (file.canRead()) {
				files.add(file);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
}
