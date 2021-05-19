/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.LayoutHelper;

public class IntroActivity extends Activity {

    private int currentAccount = UserConfig.selectedAccount;

    private boolean startPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_TMessages);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        preferences.edit().putLong("intro_crashed_time", System.currentTimeMillis()).commit();

        setContentView(R.layout.sbdv_intro_layout);

        TextView startMessagingButtonTextView = getWindow().getDecorView().getRootView().findViewById(R.id.startMessagingButton);
        startMessagingButtonTextView.setOnClickListener(view -> {
            if (startPressed) {
                return;
            }
            startPressed = true;
            Intent intent2 = new Intent(IntroActivity.this, LaunchActivity.class);
            intent2.putExtra("fromIntro", true);
            startActivity(intent2);
            finish();
        });
        if (BuildVars.DEBUG_PRIVATE_VERSION) {
            startMessagingButtonTextView.setOnLongClickListener(v -> {
                ConnectionsManager.getInstance(currentAccount).switchBackend();
                return true;
            });
        }

        AndroidUtilities.handleProxyIntent(this, getIntent());
        setRussianLocale();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ConnectionsManager.getInstance(currentAccount).setAppPaused(false, false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ConnectionsManager.getInstance(currentAccount).setAppPaused(true, false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        preferences.edit().putLong("intro_crashed_time", 0).commit();
    }

    private void setRussianLocale() {
        for (LocaleController.LocaleInfo info : LocaleController.getInstance().languages) {
            if (info.shortName.equals("ru")){
                LocaleController.getInstance().applyLanguage(info, true, false, currentAccount);
                return;
            }
        }
    }
}
