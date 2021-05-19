package org.telegram.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.telegram.ui.Components.voip.VoIPHelper;

public class VoIPFeedbackActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		super.onCreate(savedInstanceState);
		overridePendingTransition(0, 0);
		FrameLayout contentLayout = new FrameLayout(this);
		setContentView(contentLayout);
		VoIPHelper.showRateAlert(contentLayout, this::finish, getIntent().getBooleanExtra("call_video", false), getIntent().getLongExtra("call_id", 0), getIntent().getLongExtra("call_access_hash", 0), getIntent().getIntExtra("account", 0), false);
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(0, 0);
	}
}
