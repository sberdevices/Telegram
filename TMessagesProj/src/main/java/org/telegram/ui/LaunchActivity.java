/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.arch.core.util.Function;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.util.Log;
import com.google.android.gms.common.api.Status;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.builders.AssistActionBuilder;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.ContactsLoadingObserver;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LocationController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.camera.CameraController;
import org.telegram.messenger.voip.VoIPPendingCall;
import org.telegram.messenger.voip.VoIPService;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.DrawerLayoutContainer;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Adapters.DrawerLayoutAdapter;
import org.telegram.ui.Cells.DrawerAddCell;
import org.telegram.ui.Cells.DrawerProfileCell;
import org.telegram.ui.Cells.DrawerUserCell;
import org.telegram.ui.Cells.LanguageCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AudioPlayerAlert;
import org.telegram.ui.Components.BlockingUpdateView;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.Easings;
import org.telegram.ui.Components.EmbedBottomSheet;
import org.telegram.ui.Components.GroupCallPip;
import org.telegram.ui.Components.JoinGroupAlert;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PasscodeView;
import org.telegram.ui.Components.PhonebookShareAlert;
import org.telegram.ui.Components.PipRoundVideoView;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SharingLocationsAlert;
import org.telegram.ui.Components.SideMenultItemAnimator;
import org.telegram.ui.Components.StickersAlert;
import org.telegram.ui.Components.TermsOfServiceView;
import org.telegram.ui.Components.ThemeEditorView;
import org.telegram.ui.Components.UpdateAppAlertDialog;
import org.telegram.ui.Components.voip.VoIPHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ru.sberdevices.sbdv.SbdvMainContainer;
import ru.sberdevices.sbdv.SbdvServiceLocator;
import ru.sberdevices.sbdv.analytics.AnalyticsCollector;
import ru.sberdevices.sbdv.config.Config;
import ru.sberdevices.sbdv.config.LocalConfiguration;
import ru.sberdevices.sbdv.model.AppEvent;

public class LaunchActivity extends AppCompatActivity implements ActionBarLayout.ActionBarLayoutDelegate, NotificationCenter.NotificationCenterDelegate, DialogsActivity.DialogsActivityDelegate {

    private static final String TAG = "LaunchActivity";

    private static final String EXTRA_ACTION_TOKEN = "actions.fulfillment.extra.ACTION_TOKEN";

    private boolean finished;
    private String videoPath;
    private String sendingText;
    private ArrayList<SendMessagesHelper.SendingMediaInfo> photoPathsArray;
    private ArrayList<String> documentsPathsArray;
    private ArrayList<Uri> documentsUrisArray;
    private String documentsMimeType;
    private ArrayList<String> documentsOriginalPathsArray;
    private ArrayList<TLRPC.User> contactsToSend;
    private Uri contactsToSendUri;
    private int currentConnectionState;
    private static ArrayList<BaseFragment> mainFragmentsStack = new ArrayList<>();
    private static ArrayList<BaseFragment> layerFragmentsStack = new ArrayList<>();
    private static ArrayList<BaseFragment> rightFragmentsStack = new ArrayList<>();
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;

    private ActionMode visibleActionMode;

    // not used in SberPortal
    private ImageView themeSwitchImageView;
    // not used in SberPortal
    private View themeSwitchSunView;
    // not used in SberPortal
    private RLottieDrawable themeSwitchSunDrawable;
    private ActionBarLayout actionBarLayout;
    private ActionBarLayout layersActionBarLayout;
    private ActionBarLayout rightActionBarLayout;
    // not used in SberPortal
    private FrameLayout shadowTablet;
    // not used in SberPortal
    private FrameLayout shadowTabletSide;
    private View backgroundTablet;
    private FrameLayout frameLayout;
    protected DrawerLayoutContainer drawerLayoutContainer;
    // not used in SberPortal
    private DrawerLayoutAdapter drawerLayoutAdapter;
    // not used in SberPortal
    private PasscodeView passcodeView;
    private TermsOfServiceView termsOfServiceView;
    private BlockingUpdateView blockingUpdateView;
    private AlertDialog visibleDialog;
    private AlertDialog proxyErrorDialog;
    // not used in SberPortal
    private RecyclerListView sideMenu;
    private SideMenultItemAnimator itemAnimator;

    private AlertDialog localeDialog;
    private boolean loadingLocaleDialog;
    private HashMap<String, String> systemLocaleStrings;
    private HashMap<String, String> englishLocaleStrings;

    private int currentAccount;

    private Intent passcodeSaveIntent;
    private boolean passcodeSaveIntentIsNew;
    private boolean passcodeSaveIntentIsRestore;

    private boolean tabletFullSize;

    private String loadingThemeFileName;
    private String loadingThemeWallpaperName;
    private TLRPC.TL_wallPaper loadingThemeWallpaper;
    private Theme.ThemeInfo loadingThemeInfo;
    private TLRPC.TL_theme loadingTheme;
    private boolean loadingThemeAccent;
    private AlertDialog loadingThemeProgressDialog;

    private Runnable lockRunnable;

    private SbdvMainContainer sbdvContainer;

    private static final int PLAY_SERVICES_REQUEST_CHECK_SETTINGS = 140;

    private final AnalyticsCollector analyticsCollector = SbdvServiceLocator.getAnalyticsSdkSharedInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        // 2000 msek, but 0 because already launched on startup background service
        ApplicationLoader.postInitApplication();
        AndroidUtilities.checkDisplaySize(this, getResources().getConfiguration());
        currentAccount = UserConfig.selectedAccount;
        if (!UserConfig.getInstance(currentAccount).isClientActivated()) {
            Log.d(TAG, "onCreate() isClientActivated == false");
            Intent intent = getIntent();
            boolean isProxy = false;
            if (intent != null && intent.getAction() != null) {
                if (Intent.ACTION_SEND.equals(intent.getAction()) || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
                    super.onCreate(savedInstanceState);
                    finish();
                    return;
                } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                    Uri uri = intent.getData();
                    if (uri != null) {
                        String url = uri.toString().toLowerCase();
                        isProxy = url.startsWith("tg:proxy") || url.startsWith("tg://proxy") || url.startsWith("tg:socks") || url.startsWith("tg://socks");
                    }
                }
            }
            SharedPreferences preferences = MessagesController.getGlobalMainSettings();
            long crashed_time = preferences.getLong("intro_crashed_time", 0);
            boolean fromIntro = intent != null && intent.getBooleanExtra("fromIntro", false);
            if (fromIntro) {
                preferences.edit().putLong("intro_crashed_time", 0).commit();
            } else {
                // такое условие необходимо, тк после отработки IntroActivity создается новый экземпляр LaunchActivity
                analyticsCollector.onAppEvent(AppEvent.OPEN_APP_UI);
            }
            if (!isProxy && Math.abs(crashed_time - System.currentTimeMillis()) >= 60 * 2 * 1000 && intent != null && !fromIntro) {
                preferences = ApplicationLoader.applicationContext.getSharedPreferences("logininfo2", MODE_PRIVATE);
                Map<String, ?> state = preferences.getAll();
                if (state.isEmpty()) {
                    Intent intent2 = new Intent(this, IntroActivity.class);
                    Log.d(TAG, "startIntroActivity");
                    analyticsCollector.onAppEvent(AppEvent.OPEN_INTRODUCE_SCREEN);
                    intent2.setData(intent.getData());
                    startActivity(intent2);
                    super.onCreate(savedInstanceState);
                    finish();
                    return;
                }
            }
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setTheme(R.style.Theme_TMessages);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                setTaskDescription(new ActivityManager.TaskDescription(null, null, Theme.getColor(Theme.key_actionBarDefault) | 0xff000000));
            } catch (Exception ignore) {

            }
            try {
                getWindow().setNavigationBarColor(0xff000000);
            } catch (Exception ignore) {

            }
        }
        getWindow().setBackgroundDrawableResource(R.drawable.transparent);
        if (SharedConfig.passcodeHash.length() > 0 && !SharedConfig.allowScreenCapture) {
            try {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }

        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 24) {
            AndroidUtilities.isInMultiwindow = isInMultiWindowMode();
        }

        if (!Config.IS_SBERDEVICE) Theme.createChatResources(this, false);
        if (SharedConfig.passcodeHash.length() != 0 && SharedConfig.appLocked) {
            SharedConfig.lastPauseTime = (int) (SystemClock.elapsedRealtime() / 1000);
        }
        //FileLog.d("UI create5 time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        AndroidUtilities.fillStatusBarHeight(this);
        actionBarLayout = new ActionBarLayout(this) {
            @Override
            public void setThemeAnimationValue(float value) {
                super.setThemeAnimationValue(value);
                if (ArticleViewer.hasInstance() && ArticleViewer.getInstance().isVisible()) {
                    ArticleViewer.getInstance().updateThemeColors(value);
                }
                if (!Config.IS_SBERDEVICE) drawerLayoutContainer.setBehindKeyboardColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                if (PhotoViewer.hasInstance()) {
                    PhotoViewer.getInstance().updateColors();
                }
            }
        };

        frameLayout = new FrameLayout(this);
        setContentView(frameLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (!Config.IS_SBERDEVICE && Build.VERSION.SDK_INT >= 21) {
            themeSwitchImageView = new ImageView(this);
            themeSwitchImageView.setVisibility(View.GONE);
        }

        drawerLayoutContainer = new DrawerLayoutContainer(this);
        drawerLayoutContainer.setBehindKeyboardColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        frameLayout.addView(drawerLayoutContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        if (!Config.IS_SBERDEVICE && Build.VERSION.SDK_INT >= 21) {
            themeSwitchSunView = new View(this) {
                @Override
                protected void onDraw(Canvas canvas) {
                    if (themeSwitchSunDrawable != null) {
                        themeSwitchSunDrawable.draw(canvas);
                        invalidate();
                    }
                }
            };
            frameLayout.addView(themeSwitchSunView, LayoutHelper.createFrame(48, 48));
            themeSwitchSunView.setVisibility(View.GONE);
        }

        // 600 msek on create SbdvMainContainer object!
        sbdvContainer = new SbdvMainContainer(this);
        frameLayout.addView(sbdvContainer);

        if (AndroidUtilities.isTablet()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

            RelativeLayout launchLayout = new RelativeLayout(this) {

                private boolean inLayout;

                @Override
                public void requestLayout() {
                    if (inLayout) {
                        return;
                    }
                    super.requestLayout();
                }

                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    inLayout = true;
                    int width = MeasureSpec.getSize(widthMeasureSpec);
                    int height = MeasureSpec.getSize(heightMeasureSpec);
                    setMeasuredDimension(width, height);

                    if (!AndroidUtilities.isInMultiwindow && (!AndroidUtilities.isSmallTablet() || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)) {
                        tabletFullSize = false;
                        int leftWidth = width / 100 * 35;
                        if (leftWidth < AndroidUtilities.dp(320)) {
                            leftWidth = AndroidUtilities.dp(320);
                        }
                        actionBarLayout.measure(MeasureSpec.makeMeasureSpec(leftWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                        if (!Config.IS_SBERDEVICE) shadowTabletSide.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                        rightActionBarLayout.measure(MeasureSpec.makeMeasureSpec(width - leftWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                    } else {
                        tabletFullSize = true;
                        actionBarLayout.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                    }
                    backgroundTablet.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                    if (!Config.IS_SBERDEVICE) shadowTablet.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                    layersActionBarLayout.measure(MeasureSpec.makeMeasureSpec(Math.min(AndroidUtilities.dp(530), width), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(Math.min(AndroidUtilities.dp(528), height), MeasureSpec.EXACTLY));

                    inLayout = false;
                }

                @Override
                protected void onLayout(boolean changed, int l, int t, int r, int b) {
                    int width = r - l;
                    int height = b - t;

                    if (!AndroidUtilities.isInMultiwindow && (!AndroidUtilities.isSmallTablet() || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)) {
                        int leftWidth = width / 100 * 35;
                        if (leftWidth < AndroidUtilities.dp(320)) {
                            leftWidth = AndroidUtilities.dp(320);
                        }
                        if (!Config.IS_SBERDEVICE)  shadowTabletSide.layout(leftWidth, 0, leftWidth + shadowTabletSide.getMeasuredWidth(), shadowTabletSide.getMeasuredHeight());
                        actionBarLayout.layout(0, 0, actionBarLayout.getMeasuredWidth(), actionBarLayout.getMeasuredHeight());
                        rightActionBarLayout.layout(leftWidth, 0, leftWidth + rightActionBarLayout.getMeasuredWidth(), rightActionBarLayout.getMeasuredHeight());
                    } else {
                        actionBarLayout.layout(0, 0, actionBarLayout.getMeasuredWidth(), actionBarLayout.getMeasuredHeight());
                    }
                    int x = (width - layersActionBarLayout.getMeasuredWidth()) / 2;
                    int y = (height - layersActionBarLayout.getMeasuredHeight()) / 2;
                    layersActionBarLayout.layout(x, y, x + layersActionBarLayout.getMeasuredWidth(), y + layersActionBarLayout.getMeasuredHeight());
                    backgroundTablet.layout(0, 0, backgroundTablet.getMeasuredWidth(), backgroundTablet.getMeasuredHeight());
                    if (!Config.IS_SBERDEVICE)  shadowTablet.layout(0, 0, shadowTablet.getMeasuredWidth(), shadowTablet.getMeasuredHeight());
                }
            };
            drawerLayoutContainer.addView(launchLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            backgroundTablet = new View(this);
            backgroundTablet.setBackgroundResource(R.drawable.sbdv_background);
            launchLayout.addView(backgroundTablet, LayoutHelper.createRelative(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            launchLayout.addView(actionBarLayout);

            rightActionBarLayout = new ActionBarLayout(this);
            rightActionBarLayout.init(rightFragmentsStack);
            rightActionBarLayout.setDelegate(this);
            launchLayout.addView(rightActionBarLayout);

            if (!Config.IS_SBERDEVICE) {
            shadowTabletSide = new FrameLayout(this);
            shadowTabletSide.setBackgroundColor(0x40295274);
            launchLayout.addView(shadowTabletSide);

            shadowTablet = new FrameLayout(this);
            shadowTablet.setVisibility(layerFragmentsStack.isEmpty() ? View.GONE : View.VISIBLE);
            shadowTablet.setBackgroundColor(0x7f000000);
            launchLayout.addView(shadowTablet);
            shadowTablet.setOnTouchListener((v, event) -> {
                if (!actionBarLayout.fragmentsStack.isEmpty() && event.getAction() == MotionEvent.ACTION_UP) {
                    float x = event.getX();
                    float y = event.getY();
                    int[] location = new int[2];
                    layersActionBarLayout.getLocationOnScreen(location);
                    int viewX = location[0];
                    int viewY = location[1];

                    if (layersActionBarLayout.checkTransitionAnimation() || x > viewX && x < viewX + layersActionBarLayout.getWidth() && y > viewY && y < viewY + layersActionBarLayout.getHeight()) {
                        return false;
                    } else {
                        if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                            for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                                layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                                a--;
                            }
                            layersActionBarLayout.closeLastFragment(true);
                        }
                        return true;
                    }
                }
                return false;
            });

            shadowTablet.setOnClickListener(v -> {

            });
            }

            layersActionBarLayout = new ActionBarLayout(this);
            layersActionBarLayout.setRemoveActionBarExtraHeight(true);
            layersActionBarLayout.setUseAlphaAnimations(true);
            layersActionBarLayout.init(layerFragmentsStack);
            layersActionBarLayout.setBackgroundColor(0x00000000);
            layersActionBarLayout.setDelegate(this);
            layersActionBarLayout.setDrawerLayoutContainer(drawerLayoutContainer);
            layersActionBarLayout.setVisibility(layerFragmentsStack.isEmpty() ? View.GONE : View.VISIBLE);
            launchLayout.addView(layersActionBarLayout);
        } else {
            drawerLayoutContainer.addView(actionBarLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        if (!Config.IS_SBERDEVICE) {
        sideMenu = new RecyclerListView(this) {
            @Override
            public boolean drawChild(Canvas canvas, View child, long drawingTime) {
                int restore = -1;
                if (itemAnimator != null && itemAnimator.isRunning() && itemAnimator.isAnimatingChild(child)) {
                    restore = canvas.save();
                    canvas.clipRect(0, itemAnimator.getAnimationClipTop(), getMeasuredWidth(), getMeasuredHeight());
                }
                boolean result = super.drawChild(canvas, child, drawingTime);
                if (restore >= 0) {
                    canvas.restoreToCount(restore);
                    invalidate();
                    invalidateViews();
                }
                return result;
            }
        };
        //FileLog.d("UI create34 time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        itemAnimator = new SideMenultItemAnimator(sideMenu);
        sideMenu.setItemAnimator(itemAnimator);
        sideMenu.setBackgroundColor(Theme.getColor(Theme.key_chats_menuBackground));
        sideMenu.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        sideMenu.setAllowItemsInteractionDuringAnimation(false);
        sideMenu.setAdapter(drawerLayoutAdapter = new DrawerLayoutAdapter(this, itemAnimator));
        drawerLayoutContainer.setDrawerLayout(sideMenu);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) sideMenu.getLayoutParams();
        Point screenSize = AndroidUtilities.getRealScreenSize();
        layoutParams.width = AndroidUtilities.isTablet() ? AndroidUtilities.dp(320) : Math.min(AndroidUtilities.dp(320), Math.min(screenSize.x, screenSize.y) - AndroidUtilities.dp(56));
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        sideMenu.setLayoutParams(layoutParams);
        sideMenu.setOnItemClickListener((view, position, x, y) -> {
            if (position == 0) {
                DrawerProfileCell profileCell = (DrawerProfileCell) view;
                if (profileCell.isInAvatar(x, y)) {
                    openSettings(profileCell.hasAvatar());
                } else {
                    drawerLayoutAdapter.setAccountsShown(!drawerLayoutAdapter.isAccountsShown(), true);
                }
            } else if (view instanceof DrawerUserCell) {
                switchToAccount(((DrawerUserCell) view).getAccountNumber(), true);
                drawerLayoutContainer.closeDrawer(false);
            } else if (view instanceof DrawerAddCell) {
                int freeAccount = -1;
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    if (!UserConfig.getInstance(a).isClientActivated()) {
                        freeAccount = a;
                        break;
                    }
                }
                if (freeAccount >= 0) {
                    presentFragment(new LoginActivity(freeAccount));
                }
                drawerLayoutContainer.closeDrawer(false);
            } else {
                int id = drawerLayoutAdapter.getId(position);
                if (id == 2) {
                    Bundle args = new Bundle();
                    presentFragment(new GroupCreateActivity(args));
                    drawerLayoutContainer.closeDrawer(false);
                } else if (id == 3) {
                    Bundle args = new Bundle();
                    args.putBoolean("onlyUsers", true);
                    args.putBoolean("destroyAfterSelect", true);
                    args.putBoolean("createSecretChat", true);
                    args.putBoolean("allowBots", false);
                    args.putBoolean("allowSelf", false);
                    presentFragment(new ContactsActivity(args));
                    drawerLayoutContainer.closeDrawer(false);
                } else if (id == 4) {
                    SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                    if (!BuildVars.DEBUG_VERSION && preferences.getBoolean("channel_intro", false)) {
                        Bundle args = new Bundle();
                        args.putInt("step", 0);
                        presentFragment(new ChannelCreateActivity(args));
                    } else {
                        presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANNEL_CREATE));
                        preferences.edit().putBoolean("channel_intro", true).commit();
                    }
                    drawerLayoutContainer.closeDrawer(false);
                } else if (id == 6) {
                    presentFragment(new ContactsActivity(null));
                    drawerLayoutContainer.closeDrawer(false);
                } else if (id == 7) {
                    presentFragment(new InviteContactsActivity());
                    drawerLayoutContainer.closeDrawer(false);
                } else if (id == 8) {
                    openSettings(false);
                } else if (id == 9) {
                    Browser.openUrl(LaunchActivity.this, LocaleController.getString("TelegramFaqUrl", R.string.TelegramFaqUrl));
                    drawerLayoutContainer.closeDrawer(false);
                } else if (id == 10) {
                    presentFragment(new CallLogActivity());
                    drawerLayoutContainer.closeDrawer(false);
                } else if (id == 11) {
                    Bundle args = new Bundle();
                    args.putInt("user_id", UserConfig.getInstance(currentAccount).getClientUserId());
                    presentFragment(new ChatActivity(args));
                    drawerLayoutContainer.closeDrawer(false);
                } else if (id == 12) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_NEARBY_LOCATION_ACCESS));
                            drawerLayoutContainer.closeDrawer(false);
                            return;
                        }
                    }
                    boolean enabled = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        LocationManager lm = (LocationManager) ApplicationLoader.applicationContext.getSystemService(Context.LOCATION_SERVICE);
                        enabled = lm.isLocationEnabled();
                    } else if (Build.VERSION.SDK_INT >= 19) {
                        try {
                            int mode = Settings.Secure.getInt(ApplicationLoader.applicationContext.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
                            enabled = (mode != Settings.Secure.LOCATION_MODE_OFF);
                        } catch (Throwable e) {
                            FileLog.e(e);
                        }
                    }
                    if (enabled) {
                        presentFragment(new PeopleNearbyActivity());
                    } else {
                        presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_NEARBY_LOCATION_ENABLED));
                    }
                    drawerLayoutContainer.closeDrawer(false);
                }
            }
        });
        //FileLog.d("UI create33 time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        final ItemTouchHelper sideMenuTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            private RecyclerView.ViewHolder selectedViewHolder;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (viewHolder.getItemViewType() != target.getItemViewType()) {
                    return false;
                }
                drawerLayoutAdapter.swapElements(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                clearSelectedViewHolder();
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    selectedViewHolder = viewHolder;
                    final View view = viewHolder.itemView;
                    sideMenu.cancelClickRunnables(false);
                    view.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));
                    if (Build.VERSION.SDK_INT >= 21) {
                        ObjectAnimator.ofFloat(view, "elevation", AndroidUtilities.dp(1)).setDuration(150).start();
                    }
                }
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                clearSelectedViewHolder();
            }

            private void clearSelectedViewHolder() {
                if (selectedViewHolder != null) {
                    final View view = selectedViewHolder.itemView;
                    selectedViewHolder = null;
                    view.setTranslationX(0f);
                    view.setTranslationY(0f);
                    if (Build.VERSION.SDK_INT >= 21) {
                        final ObjectAnimator animator = ObjectAnimator.ofFloat(view, "elevation", 0f);
                        animator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                view.setBackground(null);
                            }
                        });
                        animator.setDuration(150).start();
                    }
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                final View view = viewHolder.itemView;
                if (drawerLayoutAdapter.isAccountsShown()) {
                    RecyclerView.ViewHolder topViewHolder = recyclerView.findViewHolderForAdapterPosition(drawerLayoutAdapter.getFirstAccountPosition() - 1);
                    RecyclerView.ViewHolder bottomViewHolder = recyclerView.findViewHolderForAdapterPosition(drawerLayoutAdapter.getLastAccountPosition() + 1);
                    if (topViewHolder != null && topViewHolder.itemView != null && topViewHolder.itemView.getBottom() == view.getTop() && dY < 0f) {
                        dY = 0f;
                    } else if (bottomViewHolder != null && bottomViewHolder.itemView != null && bottomViewHolder.itemView.getTop() == view.getBottom() && dY > 0f) {
                        dY = 0f;
                    }
                }
                view.setTranslationX(dX);
                view.setTranslationY(dY);
            }
        });
        //FileLog.d("UI create32 time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        sideMenuTouchHelper.attachToRecyclerView(sideMenu);
        sideMenu.setOnItemLongClickListener((view, position) -> {
            if (view instanceof DrawerUserCell) {
                final int accountNumber = ((DrawerUserCell) view).getAccountNumber();
                if (accountNumber == currentAccount || AndroidUtilities.isTablet()) {
                    sideMenuTouchHelper.startDrag(sideMenu.getChildViewHolder(view));
                } else {
                    final BaseFragment fragment = new DialogsActivity(null) {
                        @Override
                        protected void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
                            super.onTransitionAnimationEnd(isOpen, backward);
                            if (!isOpen && backward) { // closed
                                drawerLayoutContainer.setDrawCurrentPreviewFragmentAbove(false);
                            }
                        }

                        @Override
                        protected void onPreviewOpenAnimationEnd() {
                            super.onPreviewOpenAnimationEnd();
                            drawerLayoutContainer.setAllowOpenDrawer(false, false);
                            drawerLayoutContainer.setDrawCurrentPreviewFragmentAbove(false);
                            switchToAccount(accountNumber, true);
                        }
                    };
                    fragment.setCurrentAccount(accountNumber);
                    actionBarLayout.presentFragmentAsPreview(fragment);
                    drawerLayoutContainer.setDrawCurrentPreviewFragmentAbove(true);
                    return true;
                }
            }
            return false;
        });
        }

        drawerLayoutContainer.setParentActionBarLayout(actionBarLayout);
        actionBarLayout.setDrawerLayoutContainer(drawerLayoutContainer);
        actionBarLayout.init(mainFragmentsStack);
        actionBarLayout.setDelegate(this);

        if (!Config.IS_SBERDEVICE) Theme.loadWallpaper();

        if (!Config.IS_SBERDEVICE) {
            passcodeView = new PasscodeView(this);
            drawerLayoutContainer.addView(passcodeView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        }

        checkCurrentAccount();
        updateCurrentConnectionState(currentAccount);

        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.closeOtherAppActivities, this);

        currentConnectionState = ConnectionsManager.getInstance(currentAccount).getConnectionState();
        //FileLog.d("UI create10 time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.needShowAlert);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.reloadInterface);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.suggestedLangpack);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didSetNewTheme);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.needSetDayNightTheme);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.needCheckSystemBarColors);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.closeOtherAppActivities);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didSetPasscode);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didSetNewWallpapper);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.notificationsCountUpdated);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.screenStateChanged);

        Log.d(TAG, "NotificationCenter.needShowAlert: " + NotificationCenter.needShowAlert);
        Log.d(TAG, "NotificationCenter.reloadInterface: " + NotificationCenter.reloadInterface);
        Log.d(TAG, "NotificationCenter.closeOtherAppActivities: " + NotificationCenter.closeOtherAppActivities);
        Log.d(TAG, "NotificationCenter.didSetPasscode: " + NotificationCenter.didSetPasscode);
        Log.d(TAG, "NotificationCenter.notificationsCountUpdated: " + NotificationCenter.notificationsCountUpdated);
        Log.d(TAG, "NotificationCenter.screenStateChanged: " + NotificationCenter.screenStateChanged);

        if (actionBarLayout.fragmentsStack.isEmpty()) {
            if (!UserConfig.getInstance(currentAccount).isClientActivated()) {
                actionBarLayout.addFragmentToStack(new LoginActivity());
                drawerLayoutContainer.setAllowOpenDrawer(false, false);

                sbdvContainer.setVisibility(View.GONE);
            } else {
                if (!Config.IS_SBERDEVICE) {
                DialogsActivity dialogsActivity = new DialogsActivity(null);
                dialogsActivity.setSideMenu(sideMenu);
                actionBarLayout.addFragmentToStack(dialogsActivity);
                }
                drawerLayoutContainer.setAllowOpenDrawer(true, false);

                sbdvContainer.setVisibility(View.VISIBLE);
                analyticsCollector.onAppEvent(AppEvent.OPEN_MAIN_SCREEN);
            }

            try {
                if (savedInstanceState != null) {
                    String fragmentName = savedInstanceState.getString("fragment");
                    if (fragmentName != null) {
                        Bundle args = savedInstanceState.getBundle("args");
                        switch (fragmentName) {
                            case "chat":
                                if (args != null) {
                                    ChatActivity chat = new ChatActivity(args);
                                    if (actionBarLayout.addFragmentToStack(chat)) {
                                        chat.restoreSelfArgs(savedInstanceState);
                                    }
                                }
                                break;
                            case "settings": {
                                args.putInt("user_id", UserConfig.getInstance(currentAccount).clientUserId);
                                ProfileActivity settings = new ProfileActivity(args);
                                actionBarLayout.addFragmentToStack(settings);
                                settings.restoreSelfArgs(savedInstanceState);
                                break;
                            }
                            case "group":
                                if (args != null) {
                                    GroupCreateFinalActivity group = new GroupCreateFinalActivity(args);
                                    if (actionBarLayout.addFragmentToStack(group)) {
                                        group.restoreSelfArgs(savedInstanceState);
                                    }
                                }
                                break;
                            case "channel":
                                if (args != null) {
                                    ChannelCreateActivity channel = new ChannelCreateActivity(args);
                                    if (actionBarLayout.addFragmentToStack(channel)) {
                                        channel.restoreSelfArgs(savedInstanceState);
                                    }
                                }
                                break;
                            case "chat_profile":
                                if (args != null) {
                                    ProfileActivity profile = new ProfileActivity(args);
                                    if (actionBarLayout.addFragmentToStack(profile)) {
                                        profile.restoreSelfArgs(savedInstanceState);
                                    }
                                }
                                break;
                            case "wallpapers": {
                                WallpapersListActivity settings = new WallpapersListActivity(WallpapersListActivity.TYPE_ALL);
                                actionBarLayout.addFragmentToStack(settings);
                                settings.restoreSelfArgs(savedInstanceState);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        } else {
            BaseFragment fragment = actionBarLayout.fragmentsStack.get(0);
            if (!Config.IS_SBERDEVICE && fragment instanceof DialogsActivity) {
                ((DialogsActivity) fragment).setSideMenu(sideMenu);
            }
            boolean allowOpen = true;
            if (AndroidUtilities.isTablet()) {
                allowOpen = actionBarLayout.fragmentsStack.size() <= 1 && layersActionBarLayout.fragmentsStack.isEmpty();
                if (layersActionBarLayout.fragmentsStack.size() == 1 && layersActionBarLayout.fragmentsStack.get(0) instanceof LoginActivity) {
                    allowOpen = false;
                }
            }
            if (actionBarLayout.fragmentsStack.size() == 1 && actionBarLayout.fragmentsStack.get(0) instanceof LoginActivity) {
                allowOpen = false;
            }
            drawerLayoutContainer.setAllowOpenDrawer(allowOpen, false);
        }
        //FileLog.d("UI create11 time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        checkLayout();
        checkSystemBarColors();
        //FileLog.d("UI create12 time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        handleIntent(getIntent(), false, savedInstanceState != null, false);
        //FileLog.d("UI create9 time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        try {
            String os1 = Build.DISPLAY;
            String os2 = Build.USER;
            if (os1 != null) {
                os1 = os1.toLowerCase();
            } else {
                os1 = "";
            }
            if (os2 != null) {
                os2 = os1.toLowerCase();
            } else {
                os2 = "";
            }
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("OS name " + os1 + " " + os2);
            }
            if ((os1.contains("flyme") || os2.contains("flyme")) && Build.VERSION.SDK_INT <= 24) {
                AndroidUtilities.incorrectDisplaySizeFix = true;
                final View view = getWindow().getDecorView().getRootView();
                view.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener = () -> {
                    int height = view.getMeasuredHeight();
                    FileLog.d("height = " + height + " displayHeight = " + AndroidUtilities.displaySize.y);
                    if (Build.VERSION.SDK_INT >= 21) {
                        height -= AndroidUtilities.statusBarHeight;
                    }
                    if (height > AndroidUtilities.dp(100) && height < AndroidUtilities.displaySize.y && height + AndroidUtilities.dp(100) > AndroidUtilities.displaySize.y) {
                        AndroidUtilities.displaySize.y = height;
                        if (BuildVars.LOGS_ENABLED) {
                            FileLog.d("fix display size y to " + AndroidUtilities.displaySize.y);
                        }
                    }
                });
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (!Config.IS_SBERDEVICE) MediaController.getInstance().setBaseActivity(this, true);
    }

    private void openSettings(boolean expanded) {
        Bundle args = new Bundle();
        args.putInt("user_id", UserConfig.getInstance(currentAccount).clientUserId);
        if (expanded) {
            args.putBoolean("expandPhoto", true);
        }
        ProfileActivity fragment = new ProfileActivity(args);
        presentFragment(fragment);
        drawerLayoutContainer.closeDrawer(false);
    }

    private void checkSystemBarColors() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int color = Theme.getColor(Theme.key_actionBarDefault, null, true);
            AndroidUtilities.setLightStatusBar(getWindow(), color == Color.WHITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final Window window = getWindow();
                color = Theme.getColor(Theme.key_windowBackgroundGray, null, true);
                if (window.getNavigationBarColor() != color) {
                    window.setNavigationBarColor(color);
                    final float brightness = AndroidUtilities.computePerceivedBrightness(color);
                    AndroidUtilities.setLightNavigationBar(getWindow(), brightness >= 0.721f);
                }
            }
        }
        if (SharedConfig.noStatusBar) {
            getWindow().setStatusBarColor(0);
        }
    }

    public void switchToAccount(int account, boolean removeAll) {
        Log.d(TAG, "switchToAccount: " + account + removeAll);

        if (account == UserConfig.selectedAccount) {
            return;
        }

        ConnectionsManager.getInstance(currentAccount).setAppPaused(true, false);
        UserConfig.selectedAccount = account;
        UserConfig.getInstance(0).saveConfig(false);

        checkCurrentAccount();
        if (AndroidUtilities.isTablet()) {
            layersActionBarLayout.removeAllFragments();
            rightActionBarLayout.removeAllFragments();
            if (!tabletFullSize) {
                if (!Config.IS_SBERDEVICE) shadowTabletSide.setVisibility(View.VISIBLE);
                if (rightActionBarLayout.fragmentsStack.isEmpty()) {
                    backgroundTablet.setVisibility(View.VISIBLE);
                }
                rightActionBarLayout.setVisibility(View.GONE);
            }
            layersActionBarLayout.setVisibility(View.GONE);
        }
        if (removeAll) {
            actionBarLayout.removeAllFragments();
        } else {
            actionBarLayout.removeFragmentFromStack(0);
        }
        if (!Config.IS_SBERDEVICE) {
        DialogsActivity dialogsActivity = new DialogsActivity(null);
        dialogsActivity.setSideMenu(sideMenu);
        actionBarLayout.addFragmentToStack(dialogsActivity, 0);
        }
        drawerLayoutContainer.setAllowOpenDrawer(true, false);
        actionBarLayout.showLastFragment();
        if (AndroidUtilities.isTablet()) {
            layersActionBarLayout.showLastFragment();
            rightActionBarLayout.showLastFragment();
        }
        if (!ApplicationLoader.mainInterfacePaused) {
            ConnectionsManager.getInstance(currentAccount).setAppPaused(false, false);
        }
        if (UserConfig.getInstance(account).unacceptedTermsOfService != null) {
            showTosActivity(account, UserConfig.getInstance(account).unacceptedTermsOfService);
        }
        updateCurrentConnectionState(currentAccount);
    }

    private void switchToAvailableAccountOrLogout() {
        int account = -1;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                account = a;
                break;
            }
        }
        if (termsOfServiceView != null) {
            termsOfServiceView.setVisibility(View.GONE);
        }
        Log.d(TAG, "switchToAvailableAccountOrLogout: " + account);

        if (account != -1) {
            switchToAccount(account, true);
        } else {
            if (drawerLayoutAdapter != null) {
                drawerLayoutAdapter.notifyDataSetChanged();
            }
            for (BaseFragment fragment : actionBarLayout.fragmentsStack) {
                fragment.onFragmentDestroy();
            }
            actionBarLayout.fragmentsStack.clear();
            if (AndroidUtilities.isTablet()) {
                for (BaseFragment fragment : layersActionBarLayout.fragmentsStack) {
                    fragment.onFragmentDestroy();
                }
                layersActionBarLayout.fragmentsStack.clear();
                for (BaseFragment fragment : rightActionBarLayout.fragmentsStack) {
                    fragment.onFragmentDestroy();
                }
                rightActionBarLayout.fragmentsStack.clear();
            }
            Intent intent2 = new Intent(this, IntroActivity.class);
            startActivity(intent2);
            onFinish();
            finish();
        }
    }

    public int getMainFragmentsCount() {
        return mainFragmentsStack.size();
    }

    private void checkCurrentAccount() {
        Log.d(TAG, "checkCurrentAccount()");

        if (currentAccount != UserConfig.selectedAccount) {
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.appDidLogout);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.mainUserInfoChanged);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didUpdateConnectionState);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.needShowAlert);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.wasUnableToFindCurrentLocation);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.openArticle);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.hasNewContactsToImport);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.needShowPlayServicesAlert);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileDidLoad);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileDidFailToLoad);
        }
        currentAccount = UserConfig.selectedAccount;
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.appDidLogout);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.mainUserInfoChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didUpdateConnectionState);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.needShowAlert);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.wasUnableToFindCurrentLocation);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.openArticle);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.hasNewContactsToImport);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.needShowPlayServicesAlert);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileDidLoad);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileDidFailToLoad);
    }

    private void checkLayout() {
        if (!AndroidUtilities.isTablet() || rightActionBarLayout == null) {
            return;
        }

        if (!AndroidUtilities.isInMultiwindow && (!AndroidUtilities.isSmallTablet() || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)) {
            tabletFullSize = false;
            if (actionBarLayout.fragmentsStack.size() >= 2) {
                for (int a = 1; a < actionBarLayout.fragmentsStack.size(); a++) {
                    BaseFragment chatFragment = actionBarLayout.fragmentsStack.get(a);
                    if (chatFragment instanceof ChatActivity) {
                        ((ChatActivity) chatFragment).setIgnoreAttachOnPause(true);
                    }
                    chatFragment.onPause();
                    actionBarLayout.fragmentsStack.remove(a);
                    rightActionBarLayout.fragmentsStack.add(chatFragment);
                    a--;
                }
                if (!Config.IS_SBERDEVICE && passcodeView.getVisibility() != View.VISIBLE) {
                    actionBarLayout.showLastFragment();
                    rightActionBarLayout.showLastFragment();
                }
            }
            rightActionBarLayout.setVisibility(rightActionBarLayout.fragmentsStack.isEmpty() ? View.GONE : View.VISIBLE);
            backgroundTablet.setVisibility(rightActionBarLayout.fragmentsStack.isEmpty() ? View.VISIBLE : View.GONE);
            if (!Config.IS_SBERDEVICE) shadowTabletSide.setVisibility(!actionBarLayout.fragmentsStack.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            tabletFullSize = true;
            if (!rightActionBarLayout.fragmentsStack.isEmpty()) {
                for (int a = 0; a < rightActionBarLayout.fragmentsStack.size(); a++) {
                    BaseFragment chatFragment = rightActionBarLayout.fragmentsStack.get(a);
                    if (chatFragment instanceof ChatActivity) {
                        ((ChatActivity) chatFragment).setIgnoreAttachOnPause(true);
                    }
                    chatFragment.onPause();
                    rightActionBarLayout.fragmentsStack.remove(a);
                    actionBarLayout.fragmentsStack.add(chatFragment);
                    a--;
                }
                if (!Config.IS_SBERDEVICE && passcodeView.getVisibility() != View.VISIBLE) {
                    actionBarLayout.showLastFragment();
                }
            }
            if (!Config.IS_SBERDEVICE) shadowTabletSide.setVisibility(View.GONE);
            rightActionBarLayout.setVisibility(View.GONE);
            backgroundTablet.setVisibility(!actionBarLayout.fragmentsStack.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void showUpdateActivity(int account, TLRPC.TL_help_appUpdate update, boolean check) {
        if (blockingUpdateView == null) {
            blockingUpdateView = new BlockingUpdateView(LaunchActivity.this) {
                @Override
                public void setVisibility(int visibility) {
                    super.setVisibility(visibility);
                    if (visibility == View.GONE) {
                        drawerLayoutContainer.setAllowOpenDrawer(true, false);
                    }
                }
            };
            drawerLayoutContainer.addView(blockingUpdateView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        }
        blockingUpdateView.show(account, update, check);
        drawerLayoutContainer.setAllowOpenDrawer(false, false);
    }

    private void showTosActivity(int account, TLRPC.TL_help_termsOfService tos) {
        if (termsOfServiceView == null) {
            termsOfServiceView = new TermsOfServiceView(this);
            termsOfServiceView.setAlpha(0f);
            drawerLayoutContainer.addView(termsOfServiceView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            termsOfServiceView.setDelegate(new TermsOfServiceView.TermsOfServiceViewDelegate() {
                @Override
                public void onAcceptTerms(int account) {
                    UserConfig.getInstance(account).unacceptedTermsOfService = null;
                    UserConfig.getInstance(account).saveConfig(false);
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                    if (mainFragmentsStack.size() > 0) {
                        mainFragmentsStack.get(mainFragmentsStack.size() - 1).onResume();
                    }
                    termsOfServiceView.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .setInterpolator(AndroidUtilities.accelerateInterpolator)
                            .withEndAction(() -> termsOfServiceView.setVisibility(View.GONE))
                            .start();
                }

                @Override
                public void onDeclineTerms(int account) {
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                    termsOfServiceView.setVisibility(View.GONE);
                }
            });
        }
        TLRPC.TL_help_termsOfService currentTos = UserConfig.getInstance(account).unacceptedTermsOfService;
        if (currentTos != tos && (currentTos == null || !currentTos.id.data.equals(tos.id.data))) {
            UserConfig.getInstance(account).unacceptedTermsOfService = tos;
            UserConfig.getInstance(account).saveConfig(false);
        }
        termsOfServiceView.show(account, tos);
        drawerLayoutContainer.setAllowOpenDrawer(false, false);
        termsOfServiceView.animate().alpha(1f).setDuration(150).setInterpolator(AndroidUtilities.decelerateInterpolator).setListener(null).start();
    }

    private void showPasscodeActivity() {
        Log.d(TAG, "showPasscodeActivity()");

        if (passcodeView == null) {
            return;
        }
        SharedConfig.appLocked = true;
        if (SecretMediaViewer.hasInstance() && SecretMediaViewer.getInstance().isVisible()) {
            SecretMediaViewer.getInstance().closePhoto(false, false);
        } else if (PhotoViewer.hasInstance() && PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().closePhoto(false, true);
        } else if (ArticleViewer.hasInstance() && ArticleViewer.getInstance().isVisible()) {
            ArticleViewer.getInstance().close(false, true);
        }
        passcodeView.onShow();
        SharedConfig.isWaitingForPasscodeEnter = true;
        drawerLayoutContainer.setAllowOpenDrawer(false, false);
        passcodeView.setDelegate(() -> {
            Log.d(TAG, "passcodeView.delegate called");

            SharedConfig.isWaitingForPasscodeEnter = false;
            if (passcodeSaveIntent != null) {
                handleIntent(passcodeSaveIntent, passcodeSaveIntentIsNew, passcodeSaveIntentIsRestore, true);
                passcodeSaveIntent = null;
            }
            drawerLayoutContainer.setAllowOpenDrawer(true, false);
            actionBarLayout.setVisibility(View.VISIBLE);
            actionBarLayout.showLastFragment();
            if (AndroidUtilities.isTablet()) {
                layersActionBarLayout.showLastFragment();
                rightActionBarLayout.showLastFragment();
                if (layersActionBarLayout.getVisibility() == View.INVISIBLE) {
                    layersActionBarLayout.setVisibility(View.VISIBLE);
                }
                rightActionBarLayout.setVisibility(View.VISIBLE);
            }
        });
        actionBarLayout.setVisibility(View.INVISIBLE);
        if (AndroidUtilities.isTablet()) {
            if (layersActionBarLayout.getVisibility() == View.VISIBLE) {
                layersActionBarLayout.setVisibility(View.INVISIBLE);
            }
            rightActionBarLayout.setVisibility(View.INVISIBLE);
        }
    }

    private boolean handleIntent(Intent intent, boolean isNew, boolean restore, boolean fromPassword) {
        Log.d(TAG, "handleIntent");

        if (AndroidUtilities.handleProxyIntent(this, intent)) {
            actionBarLayout.showLastFragment();
            if (AndroidUtilities.isTablet()) {
                layersActionBarLayout.showLastFragment();
                rightActionBarLayout.showLastFragment();
            }
            return true;
        }
        //FileLog.d("UI create13 time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        if (PhotoViewer.hasInstance() && PhotoViewer.getInstance().isVisible()) {
            if (intent == null || !Intent.ACTION_MAIN.equals(intent.getAction())) {
                PhotoViewer.getInstance().closePhoto(false, true);
            }
        }
        int flags = intent.getFlags();
        String action = intent.getAction();
        final int[] intentAccount = new int[]{intent.getIntExtra("currentAccount", UserConfig.selectedAccount)};
        switchToAccount(intentAccount[0], true);
        boolean isVoipIntent = action != null && action.equals("voip");
        if (!fromPassword && (AndroidUtilities.needShowPasscode(true) || SharedConfig.isWaitingForPasscodeEnter)) {
            showPasscodeActivity();
            UserConfig.getInstance(currentAccount).saveConfig(false);
            if (!isVoipIntent) {
                passcodeSaveIntent = intent;
                passcodeSaveIntentIsNew = isNew;
                passcodeSaveIntentIsRestore = restore;
                return false;
            }
        }
        boolean pushOpened = false;
        //FileLog.d("UI create14 time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        int push_user_id = 0;
        int push_chat_id = 0;
        int push_enc_id = 0;
        int push_msg_id = 0;
        int open_settings = 0;
        int open_new_dialog = 0;
        long dialogId = 0;
        boolean showDialogsList = false;
        boolean showPlayer = false;
        boolean showLocations = false;
        boolean showGroupVoip = false;
        boolean showCallLog = false;
        boolean audioCallUser = false;
        boolean videoCallUser = false;
        boolean needCallAlert = false;
        boolean newContact = false;
        boolean newContactAlert = false;
        boolean scanQr = false;
        String searchQuery = null;
        String callSearchQuery = null;
        String newContactName = null;
        String newContactPhone = null;

        photoPathsArray = null;
        videoPath = null;
        sendingText = null;
        documentsPathsArray = null;
        documentsOriginalPathsArray = null;
        documentsMimeType = null;
        documentsUrisArray = null;
        contactsToSend = null;
        contactsToSendUri = null;

        if ((flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            if (intent != null && intent.getAction() != null && !restore) {
                if (Intent.ACTION_SEND.equals(intent.getAction())) {
                    if (SharedConfig.directShare && intent != null && intent.getExtras() != null) {
                        dialogId = intent.getExtras().getLong("dialogId", 0);
                        String hash = null;
                        if (dialogId == 0) {
                            try {
                                String id = intent.getExtras().getString(ShortcutManagerCompat.EXTRA_SHORTCUT_ID);
                                if (id != null) {
                                    List<ShortcutInfoCompat> list = ShortcutManagerCompat.getDynamicShortcuts(ApplicationLoader.applicationContext);
                                    for (int a = 0, N = list.size(); a < N; a++) {
                                        ShortcutInfoCompat info = list.get(a);
                                        if (id.equals(info.getId())) {
                                            Bundle extras = info.getIntent().getExtras();
                                            dialogId = extras.getLong("dialogId", 0);
                                            hash = extras.getString("hash", null);
                                            break;
                                        }
                                    }
                                }
                            } catch (Throwable e) {
                                FileLog.e(e);
                            }
                        } else {
                            hash = intent.getExtras().getString("hash", null);
                        }
                        if (SharedConfig.directShareHash == null || !SharedConfig.directShareHash.equals(hash)) {
                            dialogId = 0;
                        }
                    }

                    boolean error = false;
                    String type = intent.getType();
                    if (type != null && type.equals(ContactsContract.Contacts.CONTENT_VCARD_TYPE)) {
                        try {
                            Uri uri = (Uri) intent.getExtras().get(Intent.EXTRA_STREAM);
                            if (uri != null) {
                                contactsToSend = AndroidUtilities.loadVCardFromStream(uri, currentAccount, false, null, null);
                                if (contactsToSend.size() > 5) {
                                    contactsToSend = null;
                                    documentsUrisArray = new ArrayList<>();
                                    documentsUrisArray.add(uri);
                                    documentsMimeType = type;
                                } else {
                                    contactsToSendUri = uri;
                                }
                            } else {
                                error = true;
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                            error = true;
                        }
                    } else {
                        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                        if (text == null) {
                            CharSequence textSequence = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                            if (textSequence != null) {
                                text = textSequence.toString();
                            }
                        }
                        String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);

                        if (!TextUtils.isEmpty(text)) {
                            if ((text.startsWith("http://") || text.startsWith("https://")) && !TextUtils.isEmpty(subject)) {
                                text = subject + "\n" + text;
                            }
                            sendingText = text;
                        } else if (!TextUtils.isEmpty(subject)) {
                            sendingText = subject;
                        }

                        Parcelable parcelable = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                        if (parcelable != null) {
                            String path;
                            if (!(parcelable instanceof Uri)) {
                                parcelable = Uri.parse(parcelable.toString());
                            }
                            Uri uri = (Uri) parcelable;
                            if (uri != null) {
                                if (AndroidUtilities.isInternalUri(uri)) {
                                    error = true;
                                }
                            }
                            if (!error) {
                                if (uri != null && (type != null && type.startsWith("image/") || uri.toString().toLowerCase().endsWith(".jpg"))) {
                                    if (photoPathsArray == null) {
                                        photoPathsArray = new ArrayList<>();
                                    }
                                    SendMessagesHelper.SendingMediaInfo info = new SendMessagesHelper.SendingMediaInfo();
                                    info.uri = uri;
                                    photoPathsArray.add(info);
                                } else {
                                    path = AndroidUtilities.getPath(uri);
                                    if (path != null) {
                                        if (path.startsWith("file:")) {
                                            path = path.replace("file://", "");
                                        }
                                        if (type != null && type.startsWith("video/")) {
                                            videoPath = path;
                                        } else {
                                            if (documentsPathsArray == null) {
                                                documentsPathsArray = new ArrayList<>();
                                                documentsOriginalPathsArray = new ArrayList<>();
                                            }
                                            documentsPathsArray.add(path);
                                            documentsOriginalPathsArray.add(uri.toString());
                                        }
                                    } else {
                                        if (documentsUrisArray == null) {
                                            documentsUrisArray = new ArrayList<>();
                                        }
                                        documentsUrisArray.add(uri);
                                        documentsMimeType = type;
                                    }
                                }
                            }
                        } else if (sendingText == null) {
                            error = true;
                        }
                    }
                    if (error) {
                        Toast.makeText(this, "Unsupported content", Toast.LENGTH_SHORT).show();
                    }
                } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
                    boolean error = false;
                    try {
                        ArrayList<Parcelable> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                        String type = intent.getType();
                        if (uris != null) {
                            for (int a = 0; a < uris.size(); a++) {
                                Parcelable parcelable = uris.get(a);
                                if (!(parcelable instanceof Uri)) {
                                    parcelable = Uri.parse(parcelable.toString());
                                }
                                Uri uri = (Uri) parcelable;
                                if (uri != null) {
                                    if (AndroidUtilities.isInternalUri(uri)) {
                                        uris.remove(a);
                                        a--;
                                    }
                                }
                            }
                            if (uris.isEmpty()) {
                                uris = null;
                            }
                        }
                        if (uris != null) {
                            if (type != null && type.startsWith("image/")) {
                                for (int a = 0; a < uris.size(); a++) {
                                    Parcelable parcelable = uris.get(a);
                                    if (!(parcelable instanceof Uri)) {
                                        parcelable = Uri.parse(parcelable.toString());
                                    }
                                    Uri uri = (Uri) parcelable;
                                    if (photoPathsArray == null) {
                                        photoPathsArray = new ArrayList<>();
                                    }
                                    SendMessagesHelper.SendingMediaInfo info = new SendMessagesHelper.SendingMediaInfo();
                                    info.uri = uri;
                                    photoPathsArray.add(info);
                                }
                            } else {
                                for (int a = 0; a < uris.size(); a++) {
                                    Parcelable parcelable = uris.get(a);
                                    if (!(parcelable instanceof Uri)) {
                                        parcelable = Uri.parse(parcelable.toString());
                                    }
                                    Uri uri = (Uri) parcelable;
                                    String path = AndroidUtilities.getPath(uri);
                                    String originalPath = parcelable.toString();
                                    if (originalPath == null) {
                                        originalPath = path;
                                    }
                                    if (path != null) {
                                        if (path.startsWith("file:")) {
                                            path = path.replace("file://", "");
                                        }
                                        if (documentsPathsArray == null) {
                                            documentsPathsArray = new ArrayList<>();
                                            documentsOriginalPathsArray = new ArrayList<>();
                                        }
                                        documentsPathsArray.add(path);
                                        documentsOriginalPathsArray.add(originalPath);
                                    } else {
                                        if (documentsUrisArray == null) {
                                            documentsUrisArray = new ArrayList<>();
                                        }
                                        documentsUrisArray.add(uri);
                                        documentsMimeType = type;
                                    }
                                }
                            }
                        } else {
                            error = true;
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                        error = true;
                    }
                    if (error) {
                        Toast.makeText(this, "Unsupported content", Toast.LENGTH_SHORT).show();
                    }
                } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                    Uri data = intent.getData();
                    if (data != null) {
                        String username = null;
                        String login = null;
                        String group = null;
                        String sticker = null;
                        HashMap<String, String> auth = null;
                        String unsupportedUrl = null;
                        String botUser = null;
                        String botChat = null;
                        String message = null;
                        String phone = null;
                        String game = null;
                        String phoneHash = null;
                        String lang = null;
                        String theme = null;
                        String code = null;
                        TLRPC.TL_wallPaper wallPaper = null;
                        Integer messageId = null;
                        Integer channelId = null;
                        Integer threadId = null;
                        Integer commentId = null;
                        boolean hasUrl = false;
                        final String scheme = data.getScheme();
                        if (scheme != null) {
                            switch (scheme) {
                                case "http":
                                case "https": {
                                    String host = data.getHost().toLowerCase();
                                    if (host.equals("telegram.me") || host.equals("t.me") || host.equals("telegram.dog")) {
                                        String path = data.getPath();
                                        if (path != null && path.length() > 1) {
                                            path = path.substring(1);
                                            if (path.startsWith("bg/")) {
                                                wallPaper = new TLRPC.TL_wallPaper();
                                                wallPaper.settings = new TLRPC.TL_wallPaperSettings();
                                                wallPaper.slug = path.replace("bg/", "");
                                                if (wallPaper.slug != null && wallPaper.slug.length() == 6) {
                                                    try {
                                                        wallPaper.settings.background_color = Integer.parseInt(wallPaper.slug, 16) | 0xff000000;
                                                    } catch (Exception ignore) {

                                                    }
                                                    wallPaper.slug = null;
                                                } else if (wallPaper.slug != null && wallPaper.slug.length() == 13 && wallPaper.slug.charAt(6) == '-') {
                                                    try {
                                                        wallPaper.settings.background_color = Integer.parseInt(wallPaper.slug.substring(0, 6), 16) | 0xff000000;
                                                        wallPaper.settings.second_background_color = Integer.parseInt(wallPaper.slug.substring(7), 16) | 0xff000000;
                                                        wallPaper.settings.rotation = 45;
                                                    } catch (Exception ignore) {

                                                    }
                                                    try {
                                                        String rotation = data.getQueryParameter("rotation");
                                                        if (!TextUtils.isEmpty(rotation)) {
                                                            wallPaper.settings.rotation = Utilities.parseInt(rotation);
                                                        }
                                                    } catch (Exception ignore) {

                                                    }
                                                    wallPaper.slug = null;
                                                } else {
                                                    String mode = data.getQueryParameter("mode");
                                                    if (mode != null) {
                                                        mode = mode.toLowerCase();
                                                        String[] modes = mode.split(" ");
                                                        if (modes != null && modes.length > 0) {
                                                            for (int a = 0; a < modes.length; a++) {
                                                                if ("blur".equals(modes[a])) {
                                                                    wallPaper.settings.blur = true;
                                                                } else if ("motion".equals(modes[a])) {
                                                                    wallPaper.settings.motion = true;
                                                                }
                                                            }
                                                        }
                                                    }
                                                    String intensity = data.getQueryParameter("intensity");
                                                    if (!TextUtils.isEmpty(intensity)) {
                                                        wallPaper.settings.intensity = Utilities.parseInt(intensity);
                                                    } else {
                                                        wallPaper.settings.intensity = 50;
                                                    }
                                                    try {
                                                        String bgColor = data.getQueryParameter("bg_color");
                                                        if (!TextUtils.isEmpty(bgColor)) {
                                                            wallPaper.settings.background_color = Integer.parseInt(bgColor.substring(0, 6), 16) | 0xff000000;
                                                            if (bgColor.length() > 6) {
                                                                wallPaper.settings.second_background_color = Integer.parseInt(bgColor.substring(7), 16) | 0xff000000;
                                                                wallPaper.settings.rotation = 45;
                                                            }
                                                        } else {
                                                            wallPaper.settings.background_color = 0xffffffff;
                                                        }
                                                    } catch (Exception ignore) {

                                                    }
                                                    try {
                                                        String rotation = data.getQueryParameter("rotation");
                                                        if (!TextUtils.isEmpty(rotation)) {
                                                            wallPaper.settings.rotation = Utilities.parseInt(rotation);
                                                        }
                                                    } catch (Exception ignore) {

                                                    }
                                                }
                                            } else if (path.startsWith("login/")) {
                                                int intCode = Utilities.parseInt(path.replace("login/", ""));
                                                if (intCode != 0) {
                                                    code = "" + intCode;
                                                }
                                            } else if (path.startsWith("joinchat/")) {
                                                group = path.replace("joinchat/", "");
                                            } else if (path.startsWith("addstickers/")) {
                                                sticker = path.replace("addstickers/", "");
                                            } else if (path.startsWith("msg/") || path.startsWith("share/")) {
                                                message = data.getQueryParameter("url");
                                                if (message == null) {
                                                    message = "";
                                                }
                                                if (data.getQueryParameter("text") != null) {
                                                    if (message.length() > 0) {
                                                        hasUrl = true;
                                                        message += "\n";
                                                    }
                                                    message += data.getQueryParameter("text");
                                                }
                                                if (message.length() > 4096 * 4) {
                                                    message = message.substring(0, 4096 * 4);
                                                }
                                                while (message.endsWith("\n")) {
                                                    message = message.substring(0, message.length() - 1);
                                                }
                                            } else if (path.startsWith("confirmphone")) {
                                                phone = data.getQueryParameter("phone");
                                                phoneHash = data.getQueryParameter("hash");
                                            } else if (path.startsWith("setlanguage/")) {
                                                lang = path.substring(12);
                                            } else if (path.startsWith("addtheme/")) {
                                                theme = path.substring(9);
                                            } else if (path.startsWith("c/")) {
                                                List<String> segments = data.getPathSegments();
                                                if (segments.size() == 3) {
                                                    channelId = Utilities.parseInt(segments.get(1));
                                                    messageId = Utilities.parseInt(segments.get(2));
                                                    if (messageId == 0 || channelId == 0) {
                                                        messageId = null;
                                                        channelId = null;
                                                    }
                                                    threadId = Utilities.parseInt(data.getQueryParameter("thread"));
                                                    if (threadId == 0) {
                                                        threadId = null;
                                                    }
                                                }
                                            } else if (path.length() >= 1) {
                                                ArrayList<String> segments = new ArrayList<>(data.getPathSegments());
                                                if (segments.size() > 0 && segments.get(0).equals("s")) {
                                                    segments.remove(0);
                                                }
                                                if (segments.size() > 0) {
                                                    username = segments.get(0);
                                                    if (segments.size() > 1) {
                                                        messageId = Utilities.parseInt(segments.get(1));
                                                        if (messageId == 0) {
                                                            messageId = null;
                                                        }
                                                    }
                                                }
                                                botUser = data.getQueryParameter("start");
                                                botChat = data.getQueryParameter("startgroup");
                                                game = data.getQueryParameter("game");
                                                threadId = Utilities.parseInt(data.getQueryParameter("thread"));
                                                if (threadId == 0) {
                                                    threadId = null;
                                                }
                                                commentId = Utilities.parseInt(data.getQueryParameter("comment"));
                                                if (commentId == 0) {
                                                    commentId = null;
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                                case "tg": {
                                    String url = data.toString();
                                    if (url.startsWith("tg:resolve") || url.startsWith("tg://resolve")) {
                                        url = url.replace("tg:resolve", "tg://telegram.org").replace("tg://resolve", "tg://telegram.org");
                                        data = Uri.parse(url);
                                        username = data.getQueryParameter("domain");
                                        if ("telegrampassport".equals(username)) {
                                            username = null;
                                            auth = new HashMap<>();
                                            String scope = data.getQueryParameter("scope");
                                            if (!TextUtils.isEmpty(scope) && scope.startsWith("{") && scope.endsWith("}")) {
                                                auth.put("nonce", data.getQueryParameter("nonce"));
                                            } else {
                                                auth.put("payload", data.getQueryParameter("payload"));
                                            }
                                            auth.put("bot_id", data.getQueryParameter("bot_id"));
                                            auth.put("scope", scope);
                                            auth.put("public_key", data.getQueryParameter("public_key"));
                                            auth.put("callback_url", data.getQueryParameter("callback_url"));
                                        } else {
                                            botUser = data.getQueryParameter("start");
                                            botChat = data.getQueryParameter("startgroup");
                                            game = data.getQueryParameter("game");
                                            messageId = Utilities.parseInt(data.getQueryParameter("post"));
                                            if (messageId == 0) {
                                                messageId = null;
                                            }
                                            threadId = Utilities.parseInt(data.getQueryParameter("thread"));
                                            if (threadId == 0) {
                                                threadId = null;
                                            }
                                            commentId = Utilities.parseInt(data.getQueryParameter("comment"));
                                            if (commentId == 0) {
                                                commentId = null;
                                            }
                                        }
                                    } else if (url.startsWith("tg:privatepost") || url.startsWith("tg://privatepost")) {
                                        url = url.replace("tg:privatepost", "tg://telegram.org").replace("tg://privatepost", "tg://telegram.org");
                                        data = Uri.parse(url);
                                        messageId = Utilities.parseInt(data.getQueryParameter("post"));
                                        channelId = Utilities.parseInt(data.getQueryParameter("channel"));
                                        if (messageId == 0 || channelId == 0) {
                                            messageId = null;
                                            channelId = null;
                                        }
                                        threadId = Utilities.parseInt(data.getQueryParameter("thread"));
                                        if (threadId == 0) {
                                            threadId = null;
                                        }
                                        commentId = Utilities.parseInt(data.getQueryParameter("comment"));
                                        if (commentId == 0) {
                                            commentId = null;
                                        }
                                    } else if (url.startsWith("tg:bg") || url.startsWith("tg://bg")) {
                                        url = url.replace("tg:bg", "tg://telegram.org").replace("tg://bg", "tg://telegram.org");
                                        data = Uri.parse(url);
                                        wallPaper = new TLRPC.TL_wallPaper();
                                        wallPaper.settings = new TLRPC.TL_wallPaperSettings();
                                        wallPaper.slug = data.getQueryParameter("slug");
                                        if (wallPaper.slug == null) {
                                            wallPaper.slug = data.getQueryParameter("color");
                                        }
                                        if (wallPaper.slug != null && wallPaper.slug.length() == 6) {
                                            try {
                                                wallPaper.settings.background_color = Integer.parseInt(wallPaper.slug, 16) | 0xff000000;
                                            } catch (Exception ignore) {

                                            }
                                            wallPaper.slug = null;
                                        } else if (wallPaper.slug != null && wallPaper.slug.length() == 13 && wallPaper.slug.charAt(6) == '-') {
                                            try {
                                                wallPaper.settings.background_color = Integer.parseInt(wallPaper.slug.substring(0, 6), 16) | 0xff000000;
                                                wallPaper.settings.second_background_color = Integer.parseInt(wallPaper.slug.substring(7), 16) | 0xff000000;
                                                wallPaper.settings.rotation = 45;
                                            } catch (Exception ignore) {

                                            }
                                            try {
                                                String rotation = data.getQueryParameter("rotation");
                                                if (!TextUtils.isEmpty(rotation)) {
                                                    wallPaper.settings.rotation = Utilities.parseInt(rotation);
                                                }
                                            } catch (Exception ignore) {

                                            }
                                            wallPaper.slug = null;
                                        } else {
                                            String mode = data.getQueryParameter("mode");
                                            if (mode != null) {
                                                mode = mode.toLowerCase();
                                                String[] modes = mode.split(" ");
                                                if (modes != null && modes.length > 0) {
                                                    for (int a = 0; a < modes.length; a++) {
                                                        if ("blur".equals(modes[a])) {
                                                            wallPaper.settings.blur = true;
                                                        } else if ("motion".equals(modes[a])) {
                                                            wallPaper.settings.motion = true;
                                                        }
                                                    }
                                                }
                                            }
                                            wallPaper.settings.intensity = Utilities.parseInt(data.getQueryParameter("intensity"));
                                            try {
                                                String bgColor = data.getQueryParameter("bg_color");
                                                if (!TextUtils.isEmpty(bgColor)) {
                                                    wallPaper.settings.background_color = Integer.parseInt(bgColor.substring(0, 6), 16) | 0xff000000;
                                                    if (bgColor.length() > 6) {
                                                        wallPaper.settings.second_background_color = Integer.parseInt(bgColor.substring(7), 16) | 0xff000000;
                                                        wallPaper.settings.rotation = 45;
                                                    }
                                                }
                                            } catch (Exception ignore) {

                                            }
                                            try {
                                                String rotation = data.getQueryParameter("rotation");
                                                if (!TextUtils.isEmpty(rotation)) {
                                                    wallPaper.settings.rotation = Utilities.parseInt(rotation);
                                                }
                                            } catch (Exception ignore) {

                                            }
                                        }
                                    } else if (url.startsWith("tg:join") || url.startsWith("tg://join")) {
                                        url = url.replace("tg:join", "tg://telegram.org").replace("tg://join", "tg://telegram.org");
                                        data = Uri.parse(url);
                                        group = data.getQueryParameter("invite");
                                    } else if (url.startsWith("tg:addstickers") || url.startsWith("tg://addstickers")) {
                                        url = url.replace("tg:addstickers", "tg://telegram.org").replace("tg://addstickers", "tg://telegram.org");
                                        data = Uri.parse(url);
                                        sticker = data.getQueryParameter("set");
                                    } else if (url.startsWith("tg:msg") || url.startsWith("tg://msg") || url.startsWith("tg://share") || url.startsWith("tg:share")) {
                                        url = url.replace("tg:msg", "tg://telegram.org").replace("tg://msg", "tg://telegram.org").replace("tg://share", "tg://telegram.org").replace("tg:share", "tg://telegram.org");
                                        data = Uri.parse(url);
                                        message = data.getQueryParameter("url");
                                        if (message == null) {
                                            message = "";
                                        }
                                        if (data.getQueryParameter("text") != null) {
                                            if (message.length() > 0) {
                                                hasUrl = true;
                                                message += "\n";
                                            }
                                            message += data.getQueryParameter("text");
                                        }
                                        if (message.length() > 4096 * 4) {
                                            message = message.substring(0, 4096 * 4);
                                        }
                                        while (message.endsWith("\n")) {
                                            message = message.substring(0, message.length() - 1);
                                        }
                                    } else if (url.startsWith("tg:confirmphone") || url.startsWith("tg://confirmphone")) {
                                        url = url.replace("tg:confirmphone", "tg://telegram.org").replace("tg://confirmphone", "tg://telegram.org");
                                        data = Uri.parse(url);

                                        phone = data.getQueryParameter("phone");
                                        phoneHash = data.getQueryParameter("hash");
                                    } else if (url.startsWith("tg:login") || url.startsWith("tg://login")) {
                                        url = url.replace("tg:login", "tg://telegram.org").replace("tg://login", "tg://telegram.org");
                                        data = Uri.parse(url);
                                        login = data.getQueryParameter("token");
                                        int intCode = Utilities.parseInt(data.getQueryParameter("code"));
                                        if (intCode != 0) {
                                            code = "" + intCode;
                                        }
                                    } else if (url.startsWith("tg:openmessage") || url.startsWith("tg://openmessage")) {
                                        url = url.replace("tg:openmessage", "tg://telegram.org").replace("tg://openmessage", "tg://telegram.org");
                                        data = Uri.parse(url);

                                        String userID = data.getQueryParameter("user_id");
                                        String chatID = data.getQueryParameter("chat_id");
                                        String msgID = data.getQueryParameter("message_id");
                                        if (userID != null) {
                                            try {
                                                push_user_id = Integer.parseInt(userID);
                                            } catch (NumberFormatException ignore) {
                                            }
                                        } else if (chatID != null) {
                                            try {
                                                push_chat_id = Integer.parseInt(chatID);
                                            } catch (NumberFormatException ignore) {
                                            }
                                        }
                                        if (msgID != null) {
                                            try {
                                                push_msg_id = Integer.parseInt(msgID);
                                            } catch (NumberFormatException ignore) {
                                            }
                                        }
                                    } else if (url.startsWith("tg:passport") || url.startsWith("tg://passport") || url.startsWith("tg:secureid")) {
                                        url = url.replace("tg:passport", "tg://telegram.org").replace("tg://passport", "tg://telegram.org").replace("tg:secureid", "tg://telegram.org");
                                        data = Uri.parse(url);
                                        auth = new HashMap<>();
                                        String scope = data.getQueryParameter("scope");
                                        if (!TextUtils.isEmpty(scope) && scope.startsWith("{") && scope.endsWith("}")) {
                                            auth.put("nonce", data.getQueryParameter("nonce"));
                                        } else {
                                            auth.put("payload", data.getQueryParameter("payload"));
                                        }
                                        auth.put("bot_id", data.getQueryParameter("bot_id"));
                                        auth.put("scope", scope);
                                        auth.put("public_key", data.getQueryParameter("public_key"));
                                        auth.put("callback_url", data.getQueryParameter("callback_url"));
                                    } else if (url.startsWith("tg:setlanguage") || url.startsWith("tg://setlanguage")) {
                                        url = url.replace("tg:setlanguage", "tg://telegram.org").replace("tg://setlanguage", "tg://telegram.org");
                                        data = Uri.parse(url);
                                        lang = data.getQueryParameter("lang");
                                    } else if (url.startsWith("tg:addtheme") || url.startsWith("tg://addtheme")) {
                                        url = url.replace("tg:addtheme", "tg://telegram.org").replace("tg://addtheme", "tg://telegram.org");
                                        data = Uri.parse(url);
                                        theme = data.getQueryParameter("slug");
                                    } else if (url.startsWith("tg:settings") || url.startsWith("tg://settings")) {
                                        if (url.contains("themes")) {
                                            open_settings = 2;
                                        } else if (url.contains("devices")) {
                                            open_settings = 3;
                                        } else if (url.contains("folders")) {
                                            open_settings = 4;
                                        } else if (url.contains("change_number")) {
                                            open_settings = 5;
                                        } else {
                                            open_settings = 1;
                                        }
                                    } else if ((url.startsWith("tg:search") || url.startsWith("tg://search"))) {
                                        url = url.replace("tg:search", "tg://telegram.org").replace("tg://search", "tg://telegram.org");
                                        data = Uri.parse(url);
                                        searchQuery = data.getQueryParameter("query");
                                        if (searchQuery != null) {
                                            searchQuery = searchQuery.trim();
                                        } else {
                                            searchQuery = "";
                                        }
                                    } else if ((url.startsWith("tg:calllog") || url.startsWith("tg://calllog"))) {
                                        showCallLog = true;
                                    } else if ((url.startsWith("tg:call") || url.startsWith("tg://call"))) {
                                        if (UserConfig.getInstance(currentAccount).isClientActivated()) {
                                            final String extraForceCall = "extra_force_call";
                                            if (ContactsController.getInstance(currentAccount).contactsLoaded || intent.hasExtra(extraForceCall)) {
                                                final String callFormat = data.getQueryParameter("format");
                                                final String callUserName = data.getQueryParameter("name");
                                                final String callPhone = data.getQueryParameter("phone");
                                                final List<TLRPC.TL_contact> contacts = findContacts(callUserName, callPhone, false);

                                                if (contacts.isEmpty() && callPhone != null) {
                                                    newContactName = callUserName;
                                                    newContactPhone = callPhone;
                                                    newContactAlert = true;
                                                } else {
                                                    if (contacts.size() == 1) {
                                                        push_user_id = contacts.get(0).user_id;
                                                    }

                                                    if (push_user_id == 0) {
                                                        callSearchQuery = callUserName != null ? callUserName : "";
                                                    }

                                                    if ("video".equalsIgnoreCase(callFormat)) {
                                                        videoCallUser = true;
                                                    } else {
                                                        audioCallUser = true;
                                                    }

                                                    needCallAlert = true;
                                                }
                                            } else {
                                                final Intent copyIntent = new Intent(intent);
                                                copyIntent.removeExtra(EXTRA_ACTION_TOKEN);
                                                copyIntent.putExtra(extraForceCall, true);
                                                ContactsLoadingObserver.observe((contactsLoaded) -> handleIntent(copyIntent, true, false, false), 1000);
                                            }
                                        }
                                    } else if ((url.startsWith("tg:scanqr") || url.startsWith("tg://scanqr"))) {
                                        scanQr = true;
                                    } else if ((url.startsWith("tg:addcontact") || url.startsWith("tg://addcontact"))) {
                                        url = url.replace("tg:addcontact", "tg://telegram.org").replace("tg://addcontact", "tg://telegram.org");
                                        data = Uri.parse(url);
                                        newContactName = data.getQueryParameter("name");
                                        newContactPhone = data.getQueryParameter("phone");
                                        newContact = true;
                                    } else {
                                        unsupportedUrl = url.replace("tg://", "").replace("tg:", "");
                                        int index;
                                        if ((index = unsupportedUrl.indexOf('?')) >= 0) {
                                            unsupportedUrl = unsupportedUrl.substring(0, index);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        if (intent.hasExtra(EXTRA_ACTION_TOKEN)) {
                            final boolean success = UserConfig.getInstance(currentAccount).isClientActivated() && "tg".equals(scheme) && unsupportedUrl == null;
                            final Action assistAction = new AssistActionBuilder()
                                    .setActionToken(intent.getStringExtra(EXTRA_ACTION_TOKEN))
                                    .setActionStatus(success ? Action.Builder.STATUS_TYPE_COMPLETED : Action.Builder.STATUS_TYPE_FAILED)
                                    .build();
                            FirebaseUserActions.getInstance().end(assistAction);
                            intent.removeExtra(EXTRA_ACTION_TOKEN);
                        }
                        if (code != null || UserConfig.getInstance(currentAccount).isClientActivated()) {
                            if (phone != null || phoneHash != null) {
                                final Bundle args = new Bundle();
                                args.putString("phone", phone);
                                args.putString("hash", phoneHash);
                                AndroidUtilities.runOnUIThread(() -> presentFragment(new CancelAccountDeletionActivity(args)));
                            } else if (username != null || group != null || sticker != null || message != null || game != null || auth != null || unsupportedUrl != null || lang != null || code != null || wallPaper != null || channelId != null || theme != null || login != null) {
                                if (message != null && message.startsWith("@")) {
                                    message = " " + message;
                                }
                                runLinkRequest(intentAccount[0], username, group, sticker, botUser, botChat, message, hasUrl, messageId, channelId, threadId, commentId, game, auth, lang, unsupportedUrl, code, login, wallPaper, theme, 0);
                            } else {
                                try (Cursor cursor = getContentResolver().query(intent.getData(), null, null, null, null)) {
                                    if (cursor != null) {
                                        if (cursor.moveToFirst()) {
                                            int accountId = Utilities.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)));
                                            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                                                if (UserConfig.getInstance(a).getClientUserId() == accountId) {
                                                    intentAccount[0] = a;
                                                    switchToAccount(intentAccount[0], true);
                                                    break;
                                                }
                                            }
                                            int userId = cursor.getInt(cursor.getColumnIndex(ContactsContract.Data.DATA4));
                                            NotificationCenter.getInstance(intentAccount[0]).postNotificationName(NotificationCenter.closeChats);
                                            push_user_id = userId;
                                            String mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
                                            if (TextUtils.equals(mimeType, "vnd.android.cursor.item/vnd.org.telegram.messenger.android.call")) {
                                                audioCallUser = true;
                                            } else if (TextUtils.equals(mimeType, "vnd.android.cursor.item/vnd.org.telegram.messenger.android.call.video")) {
                                                videoCallUser = true;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    FileLog.e(e);
                                }
                            }
                        }
                    }
                } else if (intent.getAction().equals("org.telegram.messenger.OPEN_ACCOUNT")) {
                    open_settings = 1;
                } else if (intent.getAction().equals("new_dialog")) {
                    open_new_dialog = 1;
                } else if (intent.getAction().startsWith("com.tmessages.openchat")) {
                    int chatId = intent.getIntExtra("chatId", 0);
                    int userId = intent.getIntExtra("userId", 0);
                    int encId = intent.getIntExtra("encId", 0);
                    if (chatId != 0) {
                        NotificationCenter.getInstance(intentAccount[0]).postNotificationName(NotificationCenter.closeChats);
                        push_chat_id = chatId;
                    } else if (userId != 0) {
                        NotificationCenter.getInstance(intentAccount[0]).postNotificationName(NotificationCenter.closeChats);
                        push_user_id = userId;
                    } else if (encId != 0) {
                        NotificationCenter.getInstance(intentAccount[0]).postNotificationName(NotificationCenter.closeChats);
                        push_enc_id = encId;
                    } else {
                        showDialogsList = true;
                    }
                } else if (intent.getAction().equals("com.tmessages.openplayer")) {
                    showPlayer = true;
                } else if (intent.getAction().equals("org.tmessages.openlocations")) {
                    showLocations = true;
                } else if (action.equals("voip_chat")) {
                    showGroupVoip = true;
                }
            }
        }
        //FileLog.d("UI create15 time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        if (UserConfig.getInstance(currentAccount).isClientActivated()) {
            if (searchQuery != null) {
                final BaseFragment lastFragment = actionBarLayout.getLastFragment();
                if (lastFragment instanceof DialogsActivity) {
                    final DialogsActivity dialogsActivity = (DialogsActivity) lastFragment;
                    if (dialogsActivity.isMainDialogList()) {
                        if (dialogsActivity.getFragmentView() != null) {
                            dialogsActivity.search(searchQuery, true);
                        } else {
                            dialogsActivity.setInitialSearchString(searchQuery);
                        }
                    }
                } else {
                    showDialogsList = true;
                }
            }

            if (push_user_id != 0) {
                if (audioCallUser || videoCallUser) {
                    if (needCallAlert) {
                        final BaseFragment lastFragment = actionBarLayout.getLastFragment();
                        if (lastFragment != null) {
                            AlertsCreator.createCallDialogAlert(lastFragment, lastFragment.getMessagesController().getUser(push_user_id), videoCallUser);
                        }
                    } else {
                        VoIPPendingCall.startOrSchedule(this, push_user_id, videoCallUser);
                    }
                } else {
                    Bundle args = new Bundle();
                    args.putInt("user_id", push_user_id);
                    if (push_msg_id != 0) {
                        args.putInt("message_id", push_msg_id);
                    }
                    if (mainFragmentsStack.isEmpty() || MessagesController.getInstance(intentAccount[0]).checkCanOpenChat(args, mainFragmentsStack.get(mainFragmentsStack.size() - 1))) {
                        ChatActivity fragment = new ChatActivity(args);
                        if (actionBarLayout.presentFragment(fragment, false, true, true, false)) {
                            pushOpened = true;
                        }
                    }
                }
            } else if (push_chat_id != 0) {
                Bundle args = new Bundle();
                args.putInt("chat_id", push_chat_id);
                if (push_msg_id != 0) {
                    args.putInt("message_id", push_msg_id);
                }
                if (mainFragmentsStack.isEmpty() || MessagesController.getInstance(intentAccount[0]).checkCanOpenChat(args, mainFragmentsStack.get(mainFragmentsStack.size() - 1))) {
                    ChatActivity fragment = new ChatActivity(args);
                    if (actionBarLayout.presentFragment(fragment, false, true, true, false)) {
                        pushOpened = true;
                    }
                }
            } else if (push_enc_id != 0) {
                Bundle args = new Bundle();
                args.putInt("enc_id", push_enc_id);
                ChatActivity fragment = new ChatActivity(args);
                if (actionBarLayout.presentFragment(fragment, false, true, true, false)) {
                    pushOpened = true;
                }
            } else if (showDialogsList) {
                if (!AndroidUtilities.isTablet()) {
                    actionBarLayout.removeAllFragments();
                } else {
                    if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                        for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                            layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                            a--;
                        }
                        layersActionBarLayout.closeLastFragment(false);
                    }
                }
                pushOpened = false;
                isNew = false;
            } else if (showPlayer) {
                if (!actionBarLayout.fragmentsStack.isEmpty()) {
                    BaseFragment fragment = actionBarLayout.fragmentsStack.get(0);
                    fragment.showDialog(new AudioPlayerAlert(this));
                }
                pushOpened = false;
            } else if (showLocations) {
                if (!actionBarLayout.fragmentsStack.isEmpty()) {
                    BaseFragment fragment = actionBarLayout.fragmentsStack.get(0);
                    fragment.showDialog(new SharingLocationsAlert(this, info -> {
                        intentAccount[0] = info.messageObject.currentAccount;
                        switchToAccount(intentAccount[0], true);

                        LocationActivity locationActivity = new LocationActivity(2);
                        locationActivity.setMessageObject(info.messageObject);
                        final long dialog_id = info.messageObject.getDialogId();
                        locationActivity.setDelegate((location, live, notify, scheduleDate) -> SendMessagesHelper.getInstance(intentAccount[0]).sendMessage(location, dialog_id, null, null, null, null, notify, scheduleDate));
                        presentFragment(locationActivity);
                    }));
                }
                pushOpened = false;
            } else if (videoPath != null || photoPathsArray != null || sendingText != null || documentsPathsArray != null || contactsToSend != null || documentsUrisArray != null) {
                if (!AndroidUtilities.isTablet()) {
                    NotificationCenter.getInstance(intentAccount[0]).postNotificationName(NotificationCenter.closeChats);
                }
                if (dialogId == 0) {
                    Bundle args = new Bundle();
                    args.putBoolean("onlySelect", true);
                    args.putInt("dialogsType", 3);
                    args.putBoolean("allowSwitchAccount", true);
                    if (contactsToSend != null) {
                        if (contactsToSend.size() != 1) {
                            args.putString("selectAlertString", LocaleController.getString("SendContactToText", R.string.SendMessagesToText));
                            args.putString("selectAlertStringGroup", LocaleController.getString("SendContactToGroupText", R.string.SendContactToGroupText));
                        }
                    } else {
                        args.putString("selectAlertString", LocaleController.getString("SendMessagesToText", R.string.SendMessagesToText));
                        args.putString("selectAlertStringGroup", LocaleController.getString("SendMessagesToGroupText", R.string.SendMessagesToGroupText));
                    }
                    DialogsActivity fragment = new DialogsActivity(args);
                    fragment.setDelegate(this);
                    boolean removeLast;
                    if (AndroidUtilities.isTablet()) {
                        removeLast = layersActionBarLayout.fragmentsStack.size() > 0 && layersActionBarLayout.fragmentsStack.get(layersActionBarLayout.fragmentsStack.size() - 1) instanceof DialogsActivity;
                    } else {
                        removeLast = actionBarLayout.fragmentsStack.size() > 1 && actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1) instanceof DialogsActivity;
                    }
                    actionBarLayout.presentFragment(fragment, removeLast, true, true, false);
                    pushOpened = true;
                    if (SecretMediaViewer.hasInstance() && SecretMediaViewer.getInstance().isVisible()) {
                        SecretMediaViewer.getInstance().closePhoto(false, false);
                    } else if (PhotoViewer.hasInstance() && PhotoViewer.getInstance().isVisible()) {
                        PhotoViewer.getInstance().closePhoto(false, true);
                    } else if (ArticleViewer.hasInstance() && ArticleViewer.getInstance().isVisible()) {
                        ArticleViewer.getInstance().close(false, true);
                    }

                    drawerLayoutContainer.setAllowOpenDrawer(false, false);
                    if (AndroidUtilities.isTablet()) {
                        actionBarLayout.showLastFragment();
                        rightActionBarLayout.showLastFragment();
                    } else {
                        drawerLayoutContainer.setAllowOpenDrawer(true, false);
                    }
                } else {
                    ArrayList<Long> dids = new ArrayList<>();
                    dids.add(dialogId);
                    didSelectDialogs(null, dids, null, false);
                }
            } else if (open_settings != 0) {
                BaseFragment fragment;
                boolean closePrevious = false;
                if (open_settings == 1) {
                    Bundle args = new Bundle();
                    args.putInt("user_id", UserConfig.getInstance(currentAccount).clientUserId);
                    fragment = new ProfileActivity(args);
                } else if (open_settings == 2) {
                    fragment = new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC);
                } else if (open_settings == 3) {
                    fragment = new SessionsActivity(0);
                } else if (open_settings == 4) {
                    fragment = new FiltersSetupActivity();
                } else if (open_settings == 5) {
                    fragment = new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANGE_PHONE_NUMBER);
                    closePrevious = true;
                } else {
                    fragment = null;
                }
                boolean closePreviousFinal = closePrevious;
                AndroidUtilities.runOnUIThread(() -> presentFragment(fragment, closePreviousFinal, false));
                if (AndroidUtilities.isTablet()) {
                    actionBarLayout.showLastFragment();
                    rightActionBarLayout.showLastFragment();
                    drawerLayoutContainer.setAllowOpenDrawer(false, false);
                } else {
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                }
                pushOpened = true;
            } else if (open_new_dialog != 0) {
                Bundle args = new Bundle();
                args.putBoolean("destroyAfterSelect", true);
                actionBarLayout.presentFragment(new ContactsActivity(args), false, true, true, false);
                if (AndroidUtilities.isTablet()) {
                    actionBarLayout.showLastFragment();
                    rightActionBarLayout.showLastFragment();
                    drawerLayoutContainer.setAllowOpenDrawer(false, false);
                } else {
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                }
                pushOpened = true;
            } else if (callSearchQuery != null) {
                final Bundle args = new Bundle();
                args.putBoolean("destroyAfterSelect", true);
                args.putBoolean("returnAsResult", true);
                args.putBoolean("onlyUsers", true);
                args.putBoolean("allowSelf", false);
                final ContactsActivity contactsFragment = new ContactsActivity(args);
                contactsFragment.setInitialSearchString(callSearchQuery);
                final boolean videoCall = videoCallUser;
                contactsFragment.setDelegate((user, param, activity) -> {
                    final TLRPC.UserFull userFull = MessagesController.getInstance(currentAccount).getUserFull(user.id);
                    VoIPHelper.startCall(user, videoCall, userFull != null && userFull.video_calls_available, LaunchActivity.this, userFull);
                });
                actionBarLayout.presentFragment(contactsFragment, actionBarLayout.getLastFragment() instanceof ContactsActivity, true, true, false);
                if (AndroidUtilities.isTablet()) {
                    actionBarLayout.showLastFragment();
                    rightActionBarLayout.showLastFragment();
                    drawerLayoutContainer.setAllowOpenDrawer(false, false);
                } else {
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                }
                pushOpened = true;
            } else if (scanQr) {
                ActionIntroActivity fragment = new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_QR_LOGIN);
                fragment.setQrLoginDelegate(code -> {
                    AlertDialog progressDialog = new AlertDialog(LaunchActivity.this, 3);
                    progressDialog.setCanCacnel(false);
                    progressDialog.show();
                    byte[] token = Base64.decode(code.substring("tg://login?token=".length()), Base64.URL_SAFE);
                    TLRPC.TL_auth_acceptLoginToken req = new TLRPC.TL_auth_acceptLoginToken();
                    req.token = token;
                    ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                        try {
                            progressDialog.dismiss();
                        } catch (Exception ignore) {
                        }
                        if (!(response instanceof TLRPC.TL_authorization)) {
                            AndroidUtilities.runOnUIThread(() -> AlertsCreator.showSimpleAlert(fragment, LocaleController.getString("AuthAnotherClient", R.string.AuthAnotherClient), LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + error.text));
                        }
                    }));
                });
                actionBarLayout.presentFragment(fragment, false, true, true, false);
                if (AndroidUtilities.isTablet()) {
                    actionBarLayout.showLastFragment();
                    rightActionBarLayout.showLastFragment();
                    drawerLayoutContainer.setAllowOpenDrawer(false, false);
                } else {
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                }
                pushOpened = true;
            } else if (newContact) {
                final NewContactActivity fragment = new NewContactActivity();
                if (newContactName != null) {
                    final String[] names = newContactName.split(" ", 2);
                    fragment.setInitialName(names[0], names.length > 1 ? names[1] : null);
                }
                if (newContactPhone != null) {
                    fragment.setInitialPhoneNumber(PhoneFormat.stripExceptNumbers(newContactPhone, true), false);
                }
                actionBarLayout.presentFragment(fragment, false, true, true, false);
                if (AndroidUtilities.isTablet()) {
                    actionBarLayout.showLastFragment();
                    rightActionBarLayout.showLastFragment();
                    drawerLayoutContainer.setAllowOpenDrawer(false, false);
                } else {
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                }
                pushOpened = true;
            } else if (showGroupVoip) {
                GroupCallActivity.create(this, AccountInstance.getInstance(currentAccount));
                if (GroupCallActivity.groupCallInstance != null) {
                    GroupCallActivity.groupCallUiVisible = true;
                }
            } else if (newContactAlert) {
                final BaseFragment lastFragment = actionBarLayout.getLastFragment();
                if (lastFragment != null && lastFragment.getParentActivity() != null) {
                    final String finalNewContactName = newContactName;
                    final String finalNewContactPhone = NewContactActivity.getPhoneNumber(this, UserConfig.getInstance(currentAccount).getCurrentUser(), newContactPhone, false);
                    final AlertDialog newContactAlertDialog = new AlertDialog.Builder(lastFragment.getParentActivity())
                            .setTitle(LocaleController.getString("NewContactAlertTitle", R.string.NewContactAlertTitle))
                            .setMessage(AndroidUtilities.replaceTags(LocaleController.formatString("NewContactAlertMessage", R.string.NewContactAlertMessage, PhoneFormat.getInstance().format(finalNewContactPhone))))
                            .setPositiveButton(LocaleController.getString("NewContactAlertButton", R.string.NewContactAlertButton), (d, i) -> {
                                final NewContactActivity fragment = new NewContactActivity();
                                fragment.setInitialPhoneNumber(finalNewContactPhone, false);
                                if (finalNewContactName != null) {
                                    final String[] names = finalNewContactName.split(" ", 2);
                                    fragment.setInitialName(names[0], names.length > 1 ? names[1] : null);
                                }
                                lastFragment.presentFragment(fragment);
                            })
                            .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                            .create();
                    lastFragment.showDialog(newContactAlertDialog);
                    pushOpened = true;
                }
            } else if (showCallLog) {
                actionBarLayout.presentFragment(new CallLogActivity(), false, true, true, false);
                if (AndroidUtilities.isTablet()) {
                    actionBarLayout.showLastFragment();
                    rightActionBarLayout.showLastFragment();
                    drawerLayoutContainer.setAllowOpenDrawer(false, false);
                } else {
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                }
                pushOpened = true;
            }
        }
        //FileLog.d("UI create16 time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        if (!pushOpened && !isNew) {
            if (AndroidUtilities.isTablet()) {
                if (!UserConfig.getInstance(currentAccount).isClientActivated()) {
                    if (layersActionBarLayout.fragmentsStack.isEmpty()) {
                        layersActionBarLayout.addFragmentToStack(new LoginActivity());
                        drawerLayoutContainer.setAllowOpenDrawer(false, false);

                        sbdvContainer.setVisibility(View.GONE);
                    }
                } else {
                    if (actionBarLayout.fragmentsStack.isEmpty()) {
                        if (!Config.IS_SBERDEVICE) {
                        DialogsActivity dialogsActivity = new DialogsActivity(null);
                        dialogsActivity.setSideMenu(sideMenu);
                        if (searchQuery != null) {
                            dialogsActivity.setInitialSearchString(searchQuery);
                        }
                        actionBarLayout.addFragmentToStack(dialogsActivity);
                        }
                        drawerLayoutContainer.setAllowOpenDrawer(true, false);

                        if (sbdvContainer.getVisibility() != View.VISIBLE) {
                            sbdvContainer.setVisibility(View.VISIBLE);
                            analyticsCollector.onAppEvent(AppEvent.OPEN_MAIN_SCREEN);
                        }
                    }
                }
            } else {
                if (actionBarLayout.fragmentsStack.isEmpty()) {
                    if (!UserConfig.getInstance(currentAccount).isClientActivated()) {
                        actionBarLayout.addFragmentToStack(new LoginActivity());
                        drawerLayoutContainer.setAllowOpenDrawer(false, false);

                        sbdvContainer.setVisibility(View.GONE);
                    } else {
                        if (!Config.IS_SBERDEVICE) {
                        DialogsActivity dialogsActivity = new DialogsActivity(null);
                        dialogsActivity.setSideMenu(sideMenu);
                        if (searchQuery != null) {
                            dialogsActivity.setInitialSearchString(searchQuery);
                        }
                        actionBarLayout.addFragmentToStack(dialogsActivity);
                        }
                        drawerLayoutContainer.setAllowOpenDrawer(true, false);

                        if (sbdvContainer.getVisibility() != View.VISIBLE) {
                            sbdvContainer.setVisibility(View.VISIBLE);
                            analyticsCollector.onAppEvent(AppEvent.OPEN_MAIN_SCREEN);
                        }
                    }
                }
            }
            actionBarLayout.showLastFragment();
            if (AndroidUtilities.isTablet()) {
                layersActionBarLayout.showLastFragment();
                rightActionBarLayout.showLastFragment();
            }
        }
        //FileLog.d("UI create17 time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        if (isVoipIntent) {
            VoIPFragment.show(this, intentAccount[0]);
        }

        intent.setAction(null);
        //FileLog.d("UI create18 time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        return pushOpened;
    }

    @MainThread
    private void showMockCallToast(TLRPC.User caleeUser) {
        android.util.Log.d(TAG, "showMockCallToast()");
        String firstName = caleeUser.first_name != null ? caleeUser.first_name : "";
        String lastName = caleeUser.last_name != null ? caleeUser.last_name : "";
        String userName = caleeUser.username != null ? caleeUser.username : "";
        Toast mockToast = Toast.makeText(this, "Mock voice call to " + firstName + " " + lastName + " " + userName, Toast.LENGTH_LONG);
        mockToast.setGravity(Gravity.CENTER, 0, 0);
        mockToast.show();
    }

    private int runCommentRequest(int intentAccount, AlertDialog progressDialog, Integer messageId, Integer commentId, Integer threadId, TLRPC.Chat chat) {
        if (chat == null) {
            return 0;
        }
        TLRPC.TL_messages_getDiscussionMessage req = new TLRPC.TL_messages_getDiscussionMessage();
        req.peer = MessagesController.getInputPeer(chat);
        req.msg_id = commentId != null ? messageId : threadId;
        return ConnectionsManager.getInstance(intentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            boolean chatOpened = false;
            if (response instanceof TLRPC.TL_messages_discussionMessage) {
                TLRPC.TL_messages_discussionMessage res = (TLRPC.TL_messages_discussionMessage) response;
                MessagesController.getInstance(intentAccount).putUsers(res.users, false);
                MessagesController.getInstance(intentAccount).putChats(res.chats, false);
                ArrayList<MessageObject> arrayList = new ArrayList<>();
                for (int a = 0, N = res.messages.size(); a < N; a++) {
                    arrayList.add(new MessageObject(UserConfig.selectedAccount, res.messages.get(a), true, true));
                }
                if (!arrayList.isEmpty()) {
                    Bundle args = new Bundle();
                    args.putInt("chat_id", (int) -arrayList.get(0).getDialogId());
                    args.putInt("message_id", Math.max(1, messageId));
                    ChatActivity chatActivity = new ChatActivity(args);
                    chatActivity.setThreadMessages(arrayList, chat, req.msg_id, res.read_inbox_max_id, res.read_outbox_max_id);
                    if (commentId != null) {
                        chatActivity.setHighlightMessageId(commentId);
                    } else if (threadId != null) {
                        chatActivity.setHighlightMessageId(messageId);
                    }
                    presentFragment(chatActivity);
                    chatOpened = true;
                }
            }
            if (!chatOpened) {
                try {
                    Toast.makeText(LaunchActivity.this, LocaleController.getString("ChannelPostDeleted", R.string.ChannelPostDeleted), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }));
    }

    private void runLinkRequest(final int intentAccount,
                                final String username,
                                final String group,
                                final String sticker,
                                final String botUser,
                                final String botChat,
                                final String message,
                                final boolean hasUrl,
                                final Integer messageId,
                                final Integer channelId,
                                final Integer threadId,
                                final Integer commentId,
                                final String game,
                                final HashMap<String, String> auth,
                                final String lang,
                                final String unsupportedUrl,
                                final String code,
                                final String loginToken,
                                final TLRPC.TL_wallPaper wallPaper,
                                final String theme,
                                final int state) {
        if (state == 0 && UserConfig.getActivatedAccountsCount() >= 2 && auth != null) {
            AlertsCreator.createAccountSelectDialog(this, account -> {
                if (account != intentAccount) {
                    switchToAccount(account, true);
                }
                runLinkRequest(account, username, group, sticker, botUser, botChat, message, hasUrl, messageId, channelId, threadId, commentId, game, auth, lang, unsupportedUrl, code, loginToken, wallPaper, theme, 1);
            }).show();
            return;
        } else if (code != null) {
            if (NotificationCenter.getGlobalInstance().hasObservers(NotificationCenter.didReceiveSmsCode)) {
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didReceiveSmsCode, code);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(LaunchActivity.this);
                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                builder.setMessage(AndroidUtilities.replaceTags(LocaleController.formatString("OtherLoginCode", R.string.OtherLoginCode, code)));
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                showAlertDialog(builder);
            }
            return;
        } else if (loginToken != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(LaunchActivity.this);
            builder.setTitle(LocaleController.getString("AuthAnotherClient", R.string.AuthAnotherClient));
            builder.setMessage(LocaleController.getString("AuthAnotherClientUrl", R.string.AuthAnotherClientUrl));
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
            showAlertDialog(builder);
            return;
        }
        final AlertDialog progressDialog = new AlertDialog(this, 3);
        final int[] requestId = new int[]{0};
        Runnable cancelRunnable = null;

        if (username != null) {
            TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
            req.username = username;
            requestId[0] = ConnectionsManager.getInstance(intentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                if (!LaunchActivity.this.isFinishing()) {
                    boolean hideProgressDialog = true;
                    final TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                    if (error == null && actionBarLayout != null && (game == null || game != null && !res.users.isEmpty())) {
                        MessagesController.getInstance(intentAccount).putUsers(res.users, false);
                        MessagesController.getInstance(intentAccount).putChats(res.chats, false);
                        MessagesStorage.getInstance(intentAccount).putUsersAndChats(res.users, res.chats, false, true);
                        if (messageId != null && (commentId != null || threadId != null) && !res.chats.isEmpty()) {
                            requestId[0] = runCommentRequest(intentAccount, progressDialog, messageId, commentId, threadId, res.chats.get(0));
                            if (requestId[0] != 0) {
                                hideProgressDialog = false;
                            }
                        } else if (game != null) {
                            Bundle args = new Bundle();
                            args.putBoolean("onlySelect", true);
                            args.putBoolean("cantSendToChannels", true);
                            args.putInt("dialogsType", 1);
                            args.putString("selectAlertString", LocaleController.getString("SendGameToText", R.string.SendGameToText));
                            args.putString("selectAlertStringGroup", LocaleController.getString("SendGameToGroupText", R.string.SendGameToGroupText));
                            DialogsActivity fragment = new DialogsActivity(args);
                            fragment.setDelegate((fragment1, dids, message1, param) -> {
                                long did = dids.get(0);
                                TLRPC.TL_inputMediaGame inputMediaGame = new TLRPC.TL_inputMediaGame();
                                inputMediaGame.id = new TLRPC.TL_inputGameShortName();
                                inputMediaGame.id.short_name = game;
                                inputMediaGame.id.bot_id = MessagesController.getInstance(intentAccount).getInputUser(res.users.get(0));
                                SendMessagesHelper.getInstance(intentAccount).sendGame(MessagesController.getInstance(intentAccount).getInputPeer((int) did), inputMediaGame, 0, 0);

                                Bundle args1 = new Bundle();
                                args1.putBoolean("scrollToTopOnResume", true);
                                int lower_part = (int) did;
                                int high_id = (int) (did >> 32);
                                if (lower_part != 0) {
                                    if (lower_part > 0) {
                                        args1.putInt("user_id", lower_part);
                                    } else if (lower_part < 0) {
                                        args1.putInt("chat_id", -lower_part);
                                    }
                                } else {
                                    args1.putInt("enc_id", high_id);
                                }
                                if (MessagesController.getInstance(intentAccount).checkCanOpenChat(args1, fragment1)) {
                                    NotificationCenter.getInstance(intentAccount).postNotificationName(NotificationCenter.closeChats);
                                    actionBarLayout.presentFragment(new ChatActivity(args1), true, false, true, false);
                                }
                            });
                            boolean removeLast;
                            if (AndroidUtilities.isTablet()) {
                                removeLast = layersActionBarLayout.fragmentsStack.size() > 0 && layersActionBarLayout.fragmentsStack.get(layersActionBarLayout.fragmentsStack.size() - 1) instanceof DialogsActivity;
                            } else {
                                removeLast = actionBarLayout.fragmentsStack.size() > 1 && actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1) instanceof DialogsActivity;
                            }
                            actionBarLayout.presentFragment(fragment, removeLast, true, true, false);
                            if (SecretMediaViewer.hasInstance() && SecretMediaViewer.getInstance().isVisible()) {
                                SecretMediaViewer.getInstance().closePhoto(false, false);
                            } else if (PhotoViewer.hasInstance() && PhotoViewer.getInstance().isVisible()) {
                                PhotoViewer.getInstance().closePhoto(false, true);
                            } else if (ArticleViewer.hasInstance() && ArticleViewer.getInstance().isVisible()) {
                                ArticleViewer.getInstance().close(false, true);
                            }
                            drawerLayoutContainer.setAllowOpenDrawer(false, false);
                            if (AndroidUtilities.isTablet()) {
                                actionBarLayout.showLastFragment();
                                rightActionBarLayout.showLastFragment();
                            } else {
                                drawerLayoutContainer.setAllowOpenDrawer(true, false);
                            }
                        } else if (botChat != null) {
                            final TLRPC.User user = !res.users.isEmpty() ? res.users.get(0) : null;
                            if (user == null || user.bot && user.bot_nochats) {
                                try {
                                    Toast.makeText(LaunchActivity.this, LocaleController.getString("BotCantJoinGroups", R.string.BotCantJoinGroups), Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    FileLog.e(e);
                                }
                                return;
                            }
                            Bundle args = new Bundle();
                            args.putBoolean("onlySelect", true);
                            args.putInt("dialogsType", 2);
                            args.putString("addToGroupAlertString", LocaleController.formatString("AddToTheGroupAlertText", R.string.AddToTheGroupAlertText, UserObject.getUserName(user), "%1$s"));
                            DialogsActivity fragment = new DialogsActivity(args);
                            fragment.setDelegate((fragment12, dids, message1, param) -> {
                                long did = dids.get(0);
                                Bundle args12 = new Bundle();
                                args12.putBoolean("scrollToTopOnResume", true);
                                args12.putInt("chat_id", -(int) did);
                                if (mainFragmentsStack.isEmpty() || MessagesController.getInstance(intentAccount).checkCanOpenChat(args12, mainFragmentsStack.get(mainFragmentsStack.size() - 1))) {
                                    NotificationCenter.getInstance(intentAccount).postNotificationName(NotificationCenter.closeChats);
                                    MessagesController.getInstance(intentAccount).addUserToChat(-(int) did, user, 0, botChat, null, null);
                                    actionBarLayout.presentFragment(new ChatActivity(args12), true, false, true, false);
                                }
                            });
                            presentFragment(fragment);
                        } else {
                            long dialog_id;
                            boolean isBot = false;
                            Bundle args = new Bundle();
                            if (!res.chats.isEmpty()) {
                                args.putInt("chat_id", res.chats.get(0).id);
                                dialog_id = -res.chats.get(0).id;
                            } else {
                                args.putInt("user_id", res.users.get(0).id);
                                dialog_id = res.users.get(0).id;
                            }
                            if (botUser != null && res.users.size() > 0 && res.users.get(0).bot) {
                                args.putString("botUser", botUser);
                                isBot = true;
                            }
                            if (messageId != null) {
                                args.putInt("message_id", messageId);
                            }
                            BaseFragment lastFragment = !mainFragmentsStack.isEmpty() ? mainFragmentsStack.get(mainFragmentsStack.size() - 1) : null;
                            if (lastFragment == null || MessagesController.getInstance(intentAccount).checkCanOpenChat(args, lastFragment)) {
                                if (isBot && lastFragment instanceof ChatActivity && ((ChatActivity) lastFragment).getDialogId() == dialog_id) {
                                    ((ChatActivity) lastFragment).setBotUser(botUser);
                                } else {
                                    MessagesController.getInstance(intentAccount).ensureMessagesLoaded(dialog_id, messageId == null ? 0 : messageId, new MessagesController.MessagesLoadedCallback() {
                                        @Override
                                        public void onMessagesLoaded(boolean fromCache) {
                                            try {
                                                progressDialog.dismiss();
                                            } catch (Exception e) {
                                                FileLog.e(e);
                                            }
                                            if (!LaunchActivity.this.isFinishing()) {
                                                ChatActivity fragment = new ChatActivity(args);
                                                actionBarLayout.presentFragment(fragment);
                                            }
                                        }

                                        @Override
                                        public void onError() {
                                            if (!LaunchActivity.this.isFinishing()) {
                                                BaseFragment fragment = mainFragmentsStack.get(mainFragmentsStack.size() - 1);
                                                AlertsCreator.showSimpleAlert(fragment, LocaleController.getString("JoinToGroupErrorNotExist", R.string.JoinToGroupErrorNotExist));
                                            }
                                            try {
                                                progressDialog.dismiss();
                                            } catch (Exception e) {
                                                FileLog.e(e);
                                            }
                                        }
                                    });
                                    hideProgressDialog = false;
                                }
                            }
                        }
                    } else {
                        try {
                            if (error != null && error.text != null && error.text.startsWith("FLOOD_WAIT")) {
                                Toast.makeText(LaunchActivity.this, LocaleController.getString("FloodWait", R.string.FloodWait), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LaunchActivity.this, LocaleController.getString("NoUsernameFound", R.string.NoUsernameFound), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }

                    if (hideProgressDialog) {
                        try {
                            progressDialog.dismiss();
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                }
            }, ConnectionsManager.RequestFlagFailOnServerErrors));
        } else if (group != null) {
            if (state == 0) {
                final TLRPC.TL_messages_checkChatInvite req = new TLRPC.TL_messages_checkChatInvite();
                req.hash = group;
                requestId[0] = ConnectionsManager.getInstance(intentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                    if (!LaunchActivity.this.isFinishing()) {
                        boolean hideProgressDialog = true;
                        if (error == null && actionBarLayout != null) {
                            TLRPC.ChatInvite invite = (TLRPC.ChatInvite) response;
                            if (invite.chat != null && (!ChatObject.isLeftFromChat(invite.chat) || !invite.chat.kicked && (!TextUtils.isEmpty(invite.chat.username) || invite instanceof TLRPC.TL_chatInvitePeek || invite.chat.has_geo))) {
                                MessagesController.getInstance(intentAccount).putChat(invite.chat, false);
                                ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                                chats.add(invite.chat);
                                MessagesStorage.getInstance(intentAccount).putUsersAndChats(null, chats, false, true);
                                Bundle args = new Bundle();
                                args.putInt("chat_id", invite.chat.id);
                                if (mainFragmentsStack.isEmpty() || MessagesController.getInstance(intentAccount).checkCanOpenChat(args, mainFragmentsStack.get(mainFragmentsStack.size() - 1))) {
                                    boolean[] canceled = new boolean[1];
                                    progressDialog.setOnCancelListener(dialog -> canceled[0] = true);

                                    MessagesController.getInstance(intentAccount).ensureMessagesLoaded(-invite.chat.id, 0, new MessagesController.MessagesLoadedCallback() {
                                        @Override
                                        public void onMessagesLoaded(boolean fromCache) {
                                            try {
                                                progressDialog.dismiss();
                                            } catch (Exception e) {
                                                FileLog.e(e);
                                            }
                                            if (canceled[0]) {
                                                return;
                                            }
                                            ChatActivity fragment = new ChatActivity(args);
                                            if (invite instanceof TLRPC.TL_chatInvitePeek) {
                                                fragment.setChatInvite(invite);
                                            }
                                            actionBarLayout.presentFragment(fragment);
                                        }

                                        @Override
                                        public void onError() {
                                            if (!LaunchActivity.this.isFinishing()) {
                                                BaseFragment fragment = mainFragmentsStack.get(mainFragmentsStack.size() - 1);
                                                AlertsCreator.showSimpleAlert(fragment, LocaleController.getString("JoinToGroupErrorNotExist", R.string.JoinToGroupErrorNotExist));
                                            }
                                            try {
                                                progressDialog.dismiss();
                                            } catch (Exception e) {
                                                FileLog.e(e);
                                            }
                                        }
                                    });
                                    hideProgressDialog = false;
                                }
                            } else {
                                BaseFragment fragment = mainFragmentsStack.get(mainFragmentsStack.size() - 1);
                                fragment.showDialog(new JoinGroupAlert(LaunchActivity.this, invite, group, fragment));
                            }
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(LaunchActivity.this);
                            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                            if (error.text.startsWith("FLOOD_WAIT")) {
                                builder.setMessage(LocaleController.getString("FloodWait", R.string.FloodWait));
                            } else {
                                builder.setMessage(LocaleController.getString("JoinToGroupErrorNotExist", R.string.JoinToGroupErrorNotExist));
                            }
                            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                            showAlertDialog(builder);
                        }

                        try {
                            if (hideProgressDialog) {
                                progressDialog.dismiss();
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                }), ConnectionsManager.RequestFlagFailOnServerErrors);
            } else if (state == 1) {
                TLRPC.TL_messages_importChatInvite req = new TLRPC.TL_messages_importChatInvite();
                req.hash = group;
                ConnectionsManager.getInstance(intentAccount).sendRequest(req, (response, error) -> {
                    if (error == null) {
                        TLRPC.Updates updates = (TLRPC.Updates) response;
                        MessagesController.getInstance(intentAccount).processUpdates(updates, false);
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        if (!LaunchActivity.this.isFinishing()) {
                            try {
                                progressDialog.dismiss();
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                            if (error == null) {
                                if (actionBarLayout != null) {
                                    TLRPC.Updates updates = (TLRPC.Updates) response;
                                    if (!updates.chats.isEmpty()) {
                                        TLRPC.Chat chat = updates.chats.get(0);
                                        chat.left = false;
                                        chat.kicked = false;
                                        MessagesController.getInstance(intentAccount).putUsers(updates.users, false);
                                        MessagesController.getInstance(intentAccount).putChats(updates.chats, false);
                                        Bundle args = new Bundle();
                                        args.putInt("chat_id", chat.id);
                                        if (mainFragmentsStack.isEmpty() || MessagesController.getInstance(intentAccount).checkCanOpenChat(args, mainFragmentsStack.get(mainFragmentsStack.size() - 1))) {
                                            ChatActivity fragment = new ChatActivity(args);
                                            NotificationCenter.getInstance(intentAccount).postNotificationName(NotificationCenter.closeChats);
                                            actionBarLayout.presentFragment(fragment, false, true, true, false);
                                        }
                                    }
                                }
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(LaunchActivity.this);
                                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                                if (error.text.startsWith("FLOOD_WAIT")) {
                                    builder.setMessage(LocaleController.getString("FloodWait", R.string.FloodWait));
                                } else if (error.text.equals("USERS_TOO_MUCH")) {
                                    builder.setMessage(LocaleController.getString("JoinToGroupErrorFull", R.string.JoinToGroupErrorFull));
                                } else {
                                    builder.setMessage(LocaleController.getString("JoinToGroupErrorNotExist", R.string.JoinToGroupErrorNotExist));
                                }
                                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                                showAlertDialog(builder);
                            }
                        }
                    });
                }, ConnectionsManager.RequestFlagFailOnServerErrors);
            }
        } else if (sticker != null) {
            if (!mainFragmentsStack.isEmpty()) {
                TLRPC.TL_inputStickerSetShortName stickerset = new TLRPC.TL_inputStickerSetShortName();
                stickerset.short_name = sticker;
                BaseFragment fragment = mainFragmentsStack.get(mainFragmentsStack.size() - 1);
                StickersAlert alert;
                if (fragment instanceof ChatActivity) {
                    ChatActivity chatActivity = (ChatActivity) fragment;
                    alert = new StickersAlert(LaunchActivity.this, fragment, stickerset, null, chatActivity.getChatActivityEnterView());
                    alert.setCalcMandatoryInsets(chatActivity.isKeyboardVisible());
                } else {
                    alert = new StickersAlert(LaunchActivity.this, fragment, stickerset, null, null);
                }
                fragment.showDialog(alert);
            }
            return;
        } else if (message != null) {
            Bundle args = new Bundle();
            args.putBoolean("onlySelect", true);
            args.putInt("dialogsType", 3);
            DialogsActivity fragment = new DialogsActivity(args);
            fragment.setDelegate((fragment13, dids, m, param) -> {
                long did = dids.get(0);
                Bundle args13 = new Bundle();
                args13.putBoolean("scrollToTopOnResume", true);
                args13.putBoolean("hasUrl", hasUrl);
                int lower_part = (int) did;
                int high_id = (int) (did >> 32);
                if (lower_part != 0) {
                    if (lower_part > 0) {
                        args13.putInt("user_id", lower_part);
                    } else if (lower_part < 0) {
                        args13.putInt("chat_id", -lower_part);
                    }
                } else {
                    args13.putInt("enc_id", high_id);
                }
                if (MessagesController.getInstance(intentAccount).checkCanOpenChat(args13, fragment13)) {
                    NotificationCenter.getInstance(intentAccount).postNotificationName(NotificationCenter.closeChats);
                    MediaDataController.getInstance(intentAccount).saveDraft(did, 0, message, null, null, false);
                    actionBarLayout.presentFragment(new ChatActivity(args13), true, false, true, false);
                }
            });
            presentFragment(fragment, false, true);
        } else if (auth != null) {
            final int bot_id = Utilities.parseInt(auth.get("bot_id"));
            if (bot_id == 0) {
                return;
            }
            final String payload = auth.get("payload");
            final String nonce = auth.get("nonce");
            final String callbackUrl = auth.get("callback_url");
            final TLRPC.TL_account_getAuthorizationForm req = new TLRPC.TL_account_getAuthorizationForm();
            req.bot_id = bot_id;
            req.scope = auth.get("scope");
            req.public_key = auth.get("public_key");
            requestId[0] = ConnectionsManager.getInstance(intentAccount).sendRequest(req, (response, error) -> {
                final TLRPC.TL_account_authorizationForm authorizationForm = (TLRPC.TL_account_authorizationForm) response;
                if (authorizationForm != null) {
                    TLRPC.TL_account_getPassword req2 = new TLRPC.TL_account_getPassword();
                    requestId[0] = ConnectionsManager.getInstance(intentAccount).sendRequest(req2, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                        try {
                            progressDialog.dismiss();
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                        if (response1 != null) {
                            TLRPC.TL_account_password accountPassword = (TLRPC.TL_account_password) response1;
                            MessagesController.getInstance(intentAccount).putUsers(authorizationForm.users, false);
                            presentFragment(new PassportActivity(PassportActivity.TYPE_PASSWORD, req.bot_id, req.scope, req.public_key, payload, nonce, callbackUrl, authorizationForm, accountPassword));
                        }
                    }));
                } else {
                    AndroidUtilities.runOnUIThread(() -> {
                        try {
                            progressDialog.dismiss();
                            if ("APP_VERSION_OUTDATED".equals(error.text)) {
                                AlertsCreator.showUpdateAppAlert(LaunchActivity.this, LocaleController.getString("UpdateAppAlert", R.string.UpdateAppAlert), true);
                            } else {
                                showAlertDialog(AlertsCreator.createSimpleAlert(LaunchActivity.this, LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + error.text));
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    });
                }
            });
        } else if (unsupportedUrl != null) {
            TLRPC.TL_help_getDeepLinkInfo req = new TLRPC.TL_help_getDeepLinkInfo();
            req.path = unsupportedUrl;
            requestId[0] = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                try {
                    progressDialog.dismiss();
                } catch (Exception e) {
                    FileLog.e(e);
                }

                if (response instanceof TLRPC.TL_help_deepLinkInfo) {
                    TLRPC.TL_help_deepLinkInfo res = (TLRPC.TL_help_deepLinkInfo) response;
                    AlertsCreator.showUpdateAppAlert(LaunchActivity.this, res.message, res.update_app);
                }
            }));
        } else if (lang != null) {
            TLRPC.TL_langpack_getLanguage req = new TLRPC.TL_langpack_getLanguage();
            req.lang_code = lang;
            req.lang_pack = "android";
            requestId[0] = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                try {
                    progressDialog.dismiss();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (response instanceof TLRPC.TL_langPackLanguage) {
                    TLRPC.TL_langPackLanguage res = (TLRPC.TL_langPackLanguage) response;
                    showAlertDialog(AlertsCreator.createLanguageAlert(LaunchActivity.this, res));
                } else if (error != null) {
                    if ("LANG_CODE_NOT_SUPPORTED".equals(error.text)) {
                        showAlertDialog(AlertsCreator.createSimpleAlert(LaunchActivity.this, LocaleController.getString("LanguageUnsupportedError", R.string.LanguageUnsupportedError)));
                    } else {
                        showAlertDialog(AlertsCreator.createSimpleAlert(LaunchActivity.this, LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + error.text));
                    }
                }
            }));
        } else if (wallPaper != null) {
            boolean ok = false;
            if (TextUtils.isEmpty(wallPaper.slug)) {
                try {
                    WallpapersListActivity.ColorWallpaper colorWallpaper = new WallpapersListActivity.ColorWallpaper(Theme.COLOR_BACKGROUND_SLUG, wallPaper.settings.background_color, wallPaper.settings.second_background_color, AndroidUtilities.getWallpaperRotation(wallPaper.settings.rotation, false));
                    ThemePreviewActivity wallpaperActivity = new ThemePreviewActivity(colorWallpaper, null);
                    AndroidUtilities.runOnUIThread(() -> presentFragment(wallpaperActivity));
                    ok = true;
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            if (!ok) {
                TLRPC.TL_account_getWallPaper req = new TLRPC.TL_account_getWallPaper();
                TLRPC.TL_inputWallPaperSlug inputWallPaperSlug = new TLRPC.TL_inputWallPaperSlug();
                inputWallPaperSlug.slug = wallPaper.slug;
                req.wallpaper = inputWallPaperSlug;
                requestId[0] = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                    try {
                        progressDialog.dismiss();
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    if (response instanceof TLRPC.TL_wallPaper) {
                        TLRPC.TL_wallPaper res = (TLRPC.TL_wallPaper) response;
                        Object object;
                        if (res.pattern) {
                            WallpapersListActivity.ColorWallpaper colorWallpaper = new WallpapersListActivity.ColorWallpaper(res.slug, wallPaper.settings.background_color, wallPaper.settings.second_background_color, AndroidUtilities.getWallpaperRotation(wallPaper.settings.rotation, false), wallPaper.settings.intensity / 100.0f, wallPaper.settings.motion, null);
                            colorWallpaper.pattern = res;
                            object = colorWallpaper;
                        } else {
                            object = res;
                        }
                        ThemePreviewActivity wallpaperActivity = new ThemePreviewActivity(object, null);
                        wallpaperActivity.setInitialModes(wallPaper.settings.blur, wallPaper.settings.motion);
                        presentFragment(wallpaperActivity);
                    } else {
                        showAlertDialog(AlertsCreator.createSimpleAlert(LaunchActivity.this, LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + error.text));
                    }
                }));
            }
        } else if (theme != null) {
            cancelRunnable = () -> {
                loadingThemeFileName = null;
                loadingThemeWallpaperName = null;
                loadingThemeWallpaper = null;
                loadingThemeInfo = null;
                loadingThemeProgressDialog = null;
                loadingTheme = null;
            };
            TLRPC.TL_account_getTheme req = new TLRPC.TL_account_getTheme();
            req.format = "android";
            TLRPC.TL_inputThemeSlug inputThemeSlug = new TLRPC.TL_inputThemeSlug();
            inputThemeSlug.slug = theme;
            req.theme = inputThemeSlug;
            requestId[0] = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                int notFound = 2;
                if (response instanceof TLRPC.TL_theme) {
                    TLRPC.TL_theme t = (TLRPC.TL_theme) response;
                    if (t.settings != null) {
                        String key = Theme.getBaseThemeKey(t.settings);
                        Theme.ThemeInfo info = Theme.getTheme(key);
                        if (info != null) {
                            TLRPC.TL_wallPaper object;
                            if (t.settings.wallpaper instanceof TLRPC.TL_wallPaper) {
                                object = (TLRPC.TL_wallPaper) t.settings.wallpaper;
                                File path = FileLoader.getPathToAttach(object.document, true);
                                if (!path.exists()) {
                                    loadingThemeProgressDialog = progressDialog;
                                    loadingThemeAccent = true;
                                    loadingThemeInfo = info;
                                    loadingTheme = t;
                                    loadingThemeWallpaper = object;
                                    loadingThemeWallpaperName = FileLoader.getAttachFileName(object.document);
                                    FileLoader.getInstance(currentAccount).loadFile(object.document, object, 1, 1);
                                    return;
                                }
                            } else {
                                object = null;
                            }
                            try {
                                progressDialog.dismiss();
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                            notFound = 0;
                            openThemeAccentPreview(t, object, info);
                        } else {
                            notFound = 1;
                        }
                    } else if (t.document != null) {
                        loadingThemeAccent = false;
                        loadingTheme = t;
                        loadingThemeFileName = FileLoader.getAttachFileName(loadingTheme.document);
                        loadingThemeProgressDialog = progressDialog;
                        FileLoader.getInstance(currentAccount).loadFile(loadingTheme.document, t, 1, 1);
                        notFound = 0;
                    } else {
                        notFound = 1;
                    }
                } else if (error != null && "THEME_FORMAT_INVALID".equals(error.text)) {
                    notFound = 1;
                }
                if (notFound != 0) {
                    try {
                        progressDialog.dismiss();
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    if (notFound == 1) {
                        showAlertDialog(AlertsCreator.createSimpleAlert(LaunchActivity.this, LocaleController.getString("Theme", R.string.Theme), LocaleController.getString("ThemeNotSupported", R.string.ThemeNotSupported)));
                    } else {
                        showAlertDialog(AlertsCreator.createSimpleAlert(LaunchActivity.this, LocaleController.getString("Theme", R.string.Theme), LocaleController.getString("ThemeNotFound", R.string.ThemeNotFound)));
                    }
                }
            }));
        } else if (channelId != null && messageId != null) {
            if (threadId != null) {
                TLRPC.Chat chat = MessagesController.getInstance(intentAccount).getChat(channelId);
                if (chat != null) {
                    requestId[0] = runCommentRequest(intentAccount, progressDialog, messageId, commentId, threadId, chat);
                } else {
                    TLRPC.TL_channels_getChannels req = new TLRPC.TL_channels_getChannels();
                    TLRPC.TL_inputChannel inputChannel = new TLRPC.TL_inputChannel();
                    inputChannel.channel_id = channelId;
                    req.id.add(inputChannel);
                    requestId[0] = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                        boolean notFound = true;
                        if (response instanceof TLRPC.TL_messages_chats) {
                            TLRPC.TL_messages_chats res = (TLRPC.TL_messages_chats) response;
                            if (!res.chats.isEmpty()) {
                                notFound = false;
                                MessagesController.getInstance(currentAccount).putChats(res.chats, false);
                                requestId[0] = runCommentRequest(intentAccount, progressDialog, messageId, commentId, threadId, res.chats.get(0));
                            }
                        }
                        if (notFound) {
                            try {
                                progressDialog.dismiss();
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                            showAlertDialog(AlertsCreator.createSimpleAlert(LaunchActivity.this, LocaleController.getString("LinkNotFound", R.string.LinkNotFound)));
                        }
                    }));
                }
            } else {
                Bundle args = new Bundle();
                args.putInt("chat_id", channelId);
                args.putInt("message_id", messageId);
                BaseFragment lastFragment = !mainFragmentsStack.isEmpty() ? mainFragmentsStack.get(mainFragmentsStack.size() - 1) : null;
                if (lastFragment == null || MessagesController.getInstance(intentAccount).checkCanOpenChat(args, lastFragment)) {
                    AndroidUtilities.runOnUIThread(() -> {
                        if (!actionBarLayout.presentFragment(new ChatActivity(args))) {
                            TLRPC.TL_channels_getChannels req = new TLRPC.TL_channels_getChannels();
                            TLRPC.TL_inputChannel inputChannel = new TLRPC.TL_inputChannel();
                            inputChannel.channel_id = channelId;
                            req.id.add(inputChannel);
                            requestId[0] = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                                try {
                                    progressDialog.dismiss();
                                } catch (Exception e) {
                                    FileLog.e(e);
                                }
                                boolean notFound = true;
                                if (response instanceof TLRPC.TL_messages_chats) {
                                    TLRPC.TL_messages_chats res = (TLRPC.TL_messages_chats) response;
                                    if (!res.chats.isEmpty()) {
                                        notFound = false;
                                        MessagesController.getInstance(currentAccount).putChats(res.chats, false);
                                        TLRPC.Chat chat = res.chats.get(0);
                                        if (lastFragment == null || MessagesController.getInstance(intentAccount).checkCanOpenChat(args, lastFragment)) {
                                            actionBarLayout.presentFragment(new ChatActivity(args));
                                        }
                                    }
                                }
                                if (notFound) {
                                    showAlertDialog(AlertsCreator.createSimpleAlert(LaunchActivity.this, LocaleController.getString("LinkNotFound", R.string.LinkNotFound)));
                                }
                            }));
                        }
                    });
                }
            }
        }

        if (requestId[0] != 0) {
            final Runnable cancelRunnableFinal = cancelRunnable;
            progressDialog.setOnCancelListener(dialog -> {
                ConnectionsManager.getInstance(intentAccount).cancelRequest(requestId[0], true);
                if (cancelRunnableFinal != null) {
                    cancelRunnableFinal.run();
                }
            });
            try {
                progressDialog.showDelayed(300);
            } catch (Exception ignore) {

            }
        }
    }

    private List<TLRPC.TL_contact> findContacts(String userName, String userPhone, boolean allowSelf) {
        final MessagesController messagesController = MessagesController.getInstance(currentAccount);
        final ContactsController contactsController = ContactsController.getInstance(currentAccount);
        final List<TLRPC.TL_contact> contacts = new ArrayList<>(contactsController.contacts);
        final List<TLRPC.TL_contact> foundContacts = new ArrayList<>();

        if (userPhone != null) {
            userPhone = PhoneFormat.stripExceptNumbers(userPhone);
            TLRPC.TL_contact contact = contactsController.contactsByPhone.get(userPhone);
            if (contact == null) {
                String shortUserPhone = userPhone.substring(Math.max(0, userPhone.length() - 7));
                contact = contactsController.contactsByShortPhone.get(shortUserPhone);
            }
            if (contact != null) {
                final TLRPC.User user = messagesController.getUser(contact.user_id);
                if (user != null && (!user.self || allowSelf)) {
                    foundContacts.add(contact);
                } else {
                    // disable search by name
                    userName = null;
                }
            }
        }

        if (foundContacts.isEmpty() && userName != null) {
            final String query1 = userName.trim().toLowerCase();
            if (!TextUtils.isEmpty(query1)) {
                String query2 = LocaleController.getInstance().getTranslitString(query1);
                if (query1.equals(query2) || query2.length() == 0) {
                    query2 = null;
                }
                final String[] queries = new String[]{query1, query2};
                for (int i = 0, size = contacts.size(); i < size; i++) {
                    final TLRPC.TL_contact contact = contacts.get(i);
                    if (contact != null) {
                        final TLRPC.User user = messagesController.getUser(contact.user_id);
                        if (user != null) {
                            if (user.self && !allowSelf) {
                                continue;
                            }

                            final String[] names = new String[3];
                            names[0] = ContactsController.formatName(user.first_name, user.last_name).toLowerCase();
                            names[1] = LocaleController.getInstance().getTranslitString(names[0]);
                            if (names[0].equals(names[1])) {
                                names[1] = null;
                            }
                            if (UserObject.isReplyUser(user)) {
                                names[2] = LocaleController.getString("RepliesTitle", R.string.RepliesTitle).toLowerCase();
                            } else if (user.self) {
                                names[2] = LocaleController.getString("SavedMessages", R.string.SavedMessages).toLowerCase();
                            }

                            boolean found = false;
                            for (String q : queries) {
                                if (q == null) {
                                    continue;
                                }
                                for (int j = 0; j < names.length; j++) {
                                    final String name = names[j];
                                    if (name != null && (name.startsWith(q) || name.contains(" " + q))) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found && user.username != null && user.username.startsWith(q)) {
                                    found = true;
                                }
                                if (found) {
                                    foundContacts.add(contact);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return foundContacts;
    }

    public void checkAppUpdate(boolean force) {
        if (!force && BuildVars.DEBUG_VERSION || !force && !BuildVars.CHECK_UPDATES) {
            return;
        }
        if (!force && Math.abs(System.currentTimeMillis() - UserConfig.getInstance(0).lastUpdateCheckTime) < 24 * 60 * 60 * 1000) {
            return;
        }
        TLRPC.TL_help_getAppUpdate req = new TLRPC.TL_help_getAppUpdate();
        try {
            req.source = ApplicationLoader.applicationContext.getPackageManager().getInstallerPackageName(ApplicationLoader.applicationContext.getPackageName());
        } catch (Exception ignore) {

        }
        if (req.source == null) {
            req.source = "";
        }
        final int accountNum = currentAccount;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            UserConfig.getInstance(0).lastUpdateCheckTime = System.currentTimeMillis();
            UserConfig.getInstance(0).saveConfig(false);
            if (response instanceof TLRPC.TL_help_appUpdate) {
                final TLRPC.TL_help_appUpdate res = (TLRPC.TL_help_appUpdate) response;
                AndroidUtilities.runOnUIThread(() -> {
                    if (res.can_not_skip) {
                        UserConfig.getInstance(0).pendingAppUpdate = res;
                        UserConfig.getInstance(0).pendingAppUpdateBuildVersion = BuildVars.BUILD_VERSION;
                        try {
                            PackageInfo packageInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
                            UserConfig.getInstance(0).pendingAppUpdateInstallTime = Math.max(packageInfo.lastUpdateTime, packageInfo.firstInstallTime);
                        } catch (Exception e) {
                            FileLog.e(e);
                            UserConfig.getInstance(0).pendingAppUpdateInstallTime = 0;
                        }
                        UserConfig.getInstance(0).saveConfig(false);
                        showUpdateActivity(accountNum, res, false);
                    } else {
                        (new UpdateAppAlertDialog(LaunchActivity.this, res, accountNum)).show();
                    }
                });
            }
        });
    }

    public AlertDialog showAlertDialog(AlertDialog.Builder builder) {
        try {
            if (visibleDialog != null) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        try {
            visibleDialog = builder.show();
            visibleDialog.setCanceledOnTouchOutside(true);
            visibleDialog.setOnDismissListener(dialog -> {
                if (visibleDialog != null) {
                    if (visibleDialog == localeDialog) {
                        try {
                            String shorname = LocaleController.getInstance().getCurrentLocaleInfo().shortName;
                            Toast.makeText(LaunchActivity.this, getStringForLanguageAlert(shorname.equals("en") ? englishLocaleStrings : systemLocaleStrings, "ChangeLanguageLater", R.string.ChangeLanguageLater), Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                        localeDialog = null;
                    } else if (visibleDialog == proxyErrorDialog) {
                        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
                        editor.putBoolean("proxy_enabled", false);
                        editor.putBoolean("proxy_enabled_calls", false);
                        editor.commit();
                        ConnectionsManager.setProxySettings(false, "", 1080, "", "", "");
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                        proxyErrorDialog = null;
                    }
                }
                visibleDialog = null;
            });
            return visibleDialog;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public void showBulletin(Function<BulletinFactory, Bulletin> createBulletin) {
        BaseFragment topFragment = null;
        if (!layerFragmentsStack.isEmpty()) {
             topFragment = layerFragmentsStack.get(layerFragmentsStack.size() - 1);
        } else if (!rightFragmentsStack.isEmpty()) {
            topFragment = rightFragmentsStack.get(rightFragmentsStack.size() - 1);
        } else if (!mainFragmentsStack.isEmpty()) {
            topFragment = mainFragmentsStack.get(mainFragmentsStack.size() - 1);
        }
        if (BulletinFactory.canShowBulletin(topFragment)) {
            createBulletin.apply(BulletinFactory.of(topFragment)).show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        sbdvContainer.onNewIntent(intent);
        handleIntent(intent, true, false, false);
    }

    @Override
    public void didSelectDialogs(DialogsActivity dialogsFragment, ArrayList<Long> dids, CharSequence message, boolean param) {
        int attachesCount = 0;
        if (contactsToSend != null) {
            attachesCount += contactsToSend.size();
        }
        if (videoPath != null) {
            attachesCount++;
        }
        if (photoPathsArray != null) {
            attachesCount += photoPathsArray.size();
        }
        if (documentsPathsArray != null) {
            attachesCount += documentsPathsArray.size();
        }
        if (documentsUrisArray != null) {
            attachesCount += documentsUrisArray.size();
        }
        if (videoPath == null && photoPathsArray == null && documentsPathsArray == null && documentsUrisArray == null && sendingText != null) {
            attachesCount++;
        }

        for (int i = 0; i < dids.size(); i++) {
            final long did = dids.get(i);
            if (AlertsCreator.checkSlowMode(this, currentAccount, did, attachesCount > 1)) {
                return;
            }
        }

        final int account = dialogsFragment != null ? dialogsFragment.getCurrentAccount() : currentAccount;
        final ChatActivity fragment;
        if (dids.size() <= 1) {
            final long did = dids.get(0);
            int lower_part = (int) did;
            int high_id = (int) (did >> 32);

            Bundle args = new Bundle();
            args.putBoolean("scrollToTopOnResume", true);
            if (!AndroidUtilities.isTablet()) {
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.closeChats);
            }
            if (lower_part != 0) {
                if (lower_part > 0) {
                    args.putInt("user_id", lower_part);
                } else if (lower_part < 0) {
                    args.putInt("chat_id", -lower_part);
                }
            } else {
                args.putInt("enc_id", high_id);
            }
            if (!MessagesController.getInstance(account).checkCanOpenChat(args, dialogsFragment)) {
                return;
            }
            fragment = new ChatActivity(args);
        } else {
            fragment = null;
        }

        if (contactsToSend != null && contactsToSend.size() == 1 && !mainFragmentsStack.isEmpty()) {
            PhonebookShareAlert alert = new PhonebookShareAlert(mainFragmentsStack.get(mainFragmentsStack.size() - 1), null, null, contactsToSendUri, null, null, null);
            alert.setDelegate((user, notify, scheduleDate) -> {
                if (fragment != null) {
                    actionBarLayout.presentFragment(fragment, true, false, true, false);
                }
                for (int i = 0; i < dids.size(); i++) {
                    SendMessagesHelper.getInstance(account).sendMessage(user, dids.get(i), null, null, null, null, notify, scheduleDate);
                }
            });
            mainFragmentsStack.get(mainFragmentsStack.size() - 1).showDialog(alert);
        } else {
            String captionToSend = null;
            for (int i = 0; i < dids.size(); i++) {
                final long did = dids.get(i);
                int lower_part = (int) did;
                int high_id = (int) (did >> 32);

                AccountInstance accountInstance = AccountInstance.getInstance(UserConfig.selectedAccount);
                if (fragment != null) {
                    actionBarLayout.presentFragment(fragment, dialogsFragment != null, dialogsFragment == null, true, false);
                    if (videoPath != null) {
                        fragment.openVideoEditor(videoPath, sendingText);
                        sendingText = null;
                    }
                } else {
                    if (videoPath != null) {
                        if (sendingText != null && sendingText.length() <= 1024) {
                            captionToSend = sendingText;
                            sendingText = null;
                        }
                        ArrayList<String> arrayList = new ArrayList<>();
                        arrayList.add(videoPath);
                        SendMessagesHelper.prepareSendingDocuments(accountInstance, arrayList, arrayList, null, captionToSend, null, did, null, null, null, null, true, 0);
                    }
                }
                if (photoPathsArray != null) {
                    if (sendingText != null && sendingText.length() <= 1024 && photoPathsArray.size() == 1) {
                        photoPathsArray.get(0).caption = sendingText;
                        sendingText = null;
                    }
                    SendMessagesHelper.prepareSendingMedia(accountInstance, photoPathsArray, did, null, null, null, false, false, null, true, 0);
                }
                if (documentsPathsArray != null || documentsUrisArray != null) {
                    if (sendingText != null && sendingText.length() <= 1024 && ((documentsPathsArray != null ? documentsPathsArray.size() : 0) + (documentsUrisArray != null ? documentsUrisArray.size() : 0)) == 1) {
                        captionToSend = sendingText;
                        sendingText = null;
                    }
                    SendMessagesHelper.prepareSendingDocuments(accountInstance, documentsPathsArray, documentsOriginalPathsArray, documentsUrisArray, captionToSend, documentsMimeType, did, null, null, null, null, true, 0);
                }
                if (sendingText != null) {
                    SendMessagesHelper.prepareSendingText(accountInstance, sendingText, did, true, 0);
                }
                if (contactsToSend != null && !contactsToSend.isEmpty()) {
                    for (int a = 0; a < contactsToSend.size(); a++) {
                        TLRPC.User user = contactsToSend.get(a);
                        SendMessagesHelper.getInstance(account).sendMessage(user, did, null, null, null, null, true, 0);
                    }
                }
                if (!TextUtils.isEmpty(message)) {
                    SendMessagesHelper.prepareSendingText(accountInstance, message.toString(), did, true, 0);
                }
            }
        }
        if (dialogsFragment != null && fragment == null) {
            dialogsFragment.finishFragment();
        }

        photoPathsArray = null;
        videoPath = null;
        sendingText = null;
        documentsPathsArray = null;
        documentsOriginalPathsArray = null;
        contactsToSend = null;
        contactsToSendUri = null;
    }

    private void onFinish() {
        if (lockRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(lockRunnable);
            lockRunnable = null;
        }
        if (finished) {
            return;
        }
        finished = true;
        if (currentAccount != -1) {
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.appDidLogout);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.mainUserInfoChanged);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didUpdateConnectionState);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.needShowAlert);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.wasUnableToFindCurrentLocation);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.openArticle);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.hasNewContactsToImport);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.needShowPlayServicesAlert);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileDidLoad);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileDidFailToLoad);
        }

        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.needShowAlert);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didSetNewWallpapper);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.suggestedLangpack);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.reloadInterface);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didSetNewTheme);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.needSetDayNightTheme);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.needCheckSystemBarColors);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.closeOtherAppActivities);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didSetPasscode);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.notificationsCountUpdated);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.screenStateChanged);
    }

    public void presentFragment(BaseFragment fragment) {
        actionBarLayout.presentFragment(fragment);
    }

    public boolean presentFragment(final BaseFragment fragment, final boolean removeLast, boolean forceWithoutAnimation) {
        return actionBarLayout.presentFragment(fragment, removeLast, forceWithoutAnimation, true, false);
    }

    public ActionBarLayout getActionBarLayout() {
        return actionBarLayout;
    }

    public ActionBarLayout getLayersActionBarLayout() {
        return layersActionBarLayout;
    }

    public ActionBarLayout getRightActionBarLayout() {
        return rightActionBarLayout;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (SharedConfig.passcodeHash.length() != 0 && SharedConfig.lastPauseTime != 0) {
            SharedConfig.lastPauseTime = 0;
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("reset lastPauseTime onActivityResult");
            }
            UserConfig.getInstance(currentAccount).saveConfig(false);
        }
        if (requestCode == 105) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ApplicationLoader.canDrawOverlays = Settings.canDrawOverlays(this)) {
                    if (GroupCallActivity.groupCallInstance != null) {
                        GroupCallActivity.groupCallInstance.dismissInternal();
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        GroupCallPip.clearForce();
                        GroupCallPip.updateVisibility(LaunchActivity.this);
                    }, 200);
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLAY_SERVICES_REQUEST_CHECK_SETTINGS) {
            LocationController.getInstance(currentAccount).startFusedLocationRequest(resultCode == Activity.RESULT_OK);
        } else {
            ThemeEditorView editorView = ThemeEditorView.getInstance();
            if (editorView != null) {
                editorView.onActivityResult(requestCode, resultCode, data);
            }
            if (actionBarLayout.fragmentsStack.size() != 0) {
                BaseFragment fragment = actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1);
                fragment.onActivityResultFragment(requestCode, resultCode, data);
            }
            if (AndroidUtilities.isTablet()) {
                if (rightActionBarLayout.fragmentsStack.size() != 0) {
                    BaseFragment fragment = rightActionBarLayout.fragmentsStack.get(rightActionBarLayout.fragmentsStack.size() - 1);
                    fragment.onActivityResultFragment(requestCode, resultCode, data);
                }
                if (layersActionBarLayout.fragmentsStack.size() != 0) {
                    BaseFragment fragment = layersActionBarLayout.fragmentsStack.get(layersActionBarLayout.fragmentsStack.size() - 1);
                    fragment.onActivityResultFragment(requestCode, resultCode, data);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults == null) {
            grantResults = new int[0];
        }
        if (permissions == null) {
            permissions = new String[0];
        }

        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (requestCode == 4) {
            if (!granted) {
                showPermissionErrorAlert(LocaleController.getString("PermissionStorage", R.string.PermissionStorage));
            } else {
                ImageLoader.getInstance().checkMediaPaths();
            }
        } else if (requestCode == 5) {
            if (!granted) {
                showPermissionErrorAlert(LocaleController.getString("PermissionContacts", R.string.PermissionContacts));
                return;
            } else {
                ContactsController.getInstance(currentAccount).forceImportContacts();
            }
        } else if (requestCode == 3) {
            boolean audioGranted = true;
            boolean cameraGranted = true;
            for (int i = 0, size = Math.min(permissions.length, grantResults.length); i < size; i++) {
                if (Manifest.permission.RECORD_AUDIO.equals(permissions[i])) {
                    audioGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                } else if (Manifest.permission.CAMERA.equals(permissions[i])) {
                    cameraGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                }
            }
            if (!audioGranted) {
                showPermissionErrorAlert(LocaleController.getString("PermissionNoAudio", R.string.PermissionNoAudio));
            } else if (!cameraGranted) {
                showPermissionErrorAlert(LocaleController.getString("PermissionNoCamera", R.string.PermissionNoCamera));
            } else {
                if (SharedConfig.inappCamera) {
                    CameraController.getInstance().initCamera(null);
                }
                return;
            }
        } else if (requestCode == 18 || requestCode == 19 || requestCode == 20 || requestCode == 22) {
            if (!granted) {
                showPermissionErrorAlert(LocaleController.getString("PermissionNoCamera", R.string.PermissionNoCamera));
            }
        } else if (requestCode == 2) {
            if (granted) {
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.locationPermissionGranted);
            }
        }
        if (actionBarLayout.fragmentsStack.size() != 0) {
            BaseFragment fragment = actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1);
            fragment.onRequestPermissionsResultFragment(requestCode, permissions, grantResults);
        }
        if (AndroidUtilities.isTablet()) {
            if (rightActionBarLayout.fragmentsStack.size() != 0) {
                BaseFragment fragment = rightActionBarLayout.fragmentsStack.get(rightActionBarLayout.fragmentsStack.size() - 1);
                fragment.onRequestPermissionsResultFragment(requestCode, permissions, grantResults);
            }
            if (layersActionBarLayout.fragmentsStack.size() != 0) {
                BaseFragment fragment = layersActionBarLayout.fragmentsStack.get(layersActionBarLayout.fragmentsStack.size() - 1);
                fragment.onRequestPermissionsResultFragment(requestCode, permissions, grantResults);
            }
        }

        VoIPFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showPermissionErrorAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
        builder.setMessage(message);
        builder.setNegativeButton(LocaleController.getString("PermissionOpenSettings", R.string.PermissionOpenSettings), (dialog, which) -> {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + ApplicationLoader.applicationContext.getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        builder.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.stopAllHeavyOperations, 4096);
        ApplicationLoader.mainInterfacePaused = true;
        int account = currentAccount;
        Utilities.stageQueue.postRunnable(() -> {
            ApplicationLoader.mainInterfacePausedStageQueue = true;
            ApplicationLoader.mainInterfacePausedStageQueueTime = 0;
            if (VoIPService.getSharedInstance() == null) {
                MessagesController.getInstance(account).ignoreSetOnline = false;
            }
        });
        onPasscodePause();
        actionBarLayout.onPause();
        if (AndroidUtilities.isTablet()) {
            rightActionBarLayout.onPause();
            layersActionBarLayout.onPause();
        }
        if (passcodeView != null) {
            passcodeView.onPause();
        }
        ConnectionsManager.getInstance(currentAccount).setAppPaused(true, false);
        if (PhotoViewer.hasInstance() && PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().onPause();
        }

        if (VoIPFragment.getInstance() != null) {
            VoIPFragment.onPause();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Browser.bindCustomTabsService(this);
        ApplicationLoader.mainInterfaceStopped = false;
        GroupCallPip.updateVisibility(this);

        sbdvContainer.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Browser.unbindCustomTabsService(this);
        ApplicationLoader.mainInterfaceStopped = true;
        GroupCallPip.updateVisibility(this);

        sbdvContainer.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        if (PhotoViewer.getPipInstance() != null) {
            PhotoViewer.getPipInstance().destroyPhotoViewer();
        }
        if (PhotoViewer.hasInstance()) {
            PhotoViewer.getInstance().destroyPhotoViewer();
        }
        if (SecretMediaViewer.hasInstance()) {
            SecretMediaViewer.getInstance().destroyPhotoViewer();
        }
        if (ArticleViewer.hasInstance()) {
            ArticleViewer.getInstance().destroyArticleViewer();
        }
        if (ContentPreviewViewer.hasInstance()) {
            ContentPreviewViewer.getInstance().destroy();
        }
        if (GroupCallActivity.groupCallInstance != null) {
            GroupCallActivity.groupCallInstance.dismissInternal();
        }
        PipRoundVideoView pipRoundVideoView = PipRoundVideoView.getInstance();
        MediaController.getInstance().setBaseActivity(this, false);
        MediaController.getInstance().setFeedbackView(actionBarLayout, false);
        if (pipRoundVideoView != null) {
            pipRoundVideoView.close(false);
        }
        Theme.destroyResources();
        EmbedBottomSheet embedBottomSheet = EmbedBottomSheet.getInstance();
        if (embedBottomSheet != null) {
            embedBottomSheet.destroy();
        }
        ThemeEditorView editorView = ThemeEditorView.getInstance();
        if (editorView != null) {
            editorView.destroy();
        }
        try {
            if (visibleDialog != null) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        try {
            if (onGlobalLayoutListener != null) {
                final View view = getWindow().getDecorView().getRootView();
                view.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        super.onDestroy();
        onFinish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //FileLog.d("UI resume time = " + (SystemClock.elapsedRealtime() - ApplicationLoader.startTime));
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.startAllHeavyOperations, 4096);
        MediaController.getInstance().setFeedbackView(actionBarLayout, true);
        ApplicationLoader.mainInterfacePaused = false;
        showLanguageAlert(false);
        Utilities.stageQueue.postRunnable(() -> {
            ApplicationLoader.mainInterfacePausedStageQueue = false;
            ApplicationLoader.mainInterfacePausedStageQueueTime = System.currentTimeMillis();
        });
        checkFreeDiscSpace();
        if (!Config.IS_SBERDEVICE) MediaController.checkGallery();
        if (!Config.IS_SBERDEVICE) onPasscodeResume();
        if (!Config.IS_SBERDEVICE && passcodeView.getVisibility() != View.VISIBLE) {
            actionBarLayout.onResume();
            if (AndroidUtilities.isTablet()) {
                rightActionBarLayout.onResume();
                layersActionBarLayout.onResume();
            }
        } else {
            actionBarLayout.dismissDialogs();
            if (AndroidUtilities.isTablet()) {
                rightActionBarLayout.dismissDialogs();
                layersActionBarLayout.dismissDialogs();
            }
            if (!Config.IS_SBERDEVICE) passcodeView.onResume();
        }
        ConnectionsManager.getInstance(currentAccount).setAppPaused(false, false);
        updateCurrentConnectionState(currentAccount);
        if (PhotoViewer.hasInstance() && PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().onResume();
        }
        PipRoundVideoView pipRoundVideoView = PipRoundVideoView.getInstance();
        if (pipRoundVideoView != null && MediaController.getInstance().isMessagePaused()) {
            MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
            if (messageObject != null) {
                MediaController.getInstance().seekToProgress(messageObject, messageObject.audioProgress);
            }
        }
        if (UserConfig.getInstance(UserConfig.selectedAccount).unacceptedTermsOfService != null) {
            showTosActivity(UserConfig.selectedAccount, UserConfig.getInstance(UserConfig.selectedAccount).unacceptedTermsOfService);
        } else if (!Config.IS_SBERDEVICE && UserConfig.getInstance(0).pendingAppUpdate != null) {
            showUpdateActivity(UserConfig.selectedAccount, UserConfig.getInstance(0).pendingAppUpdate, true);
        }
        if (!Config.IS_SBERDEVICE) checkAppUpdate(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ApplicationLoader.canDrawOverlays = Settings.canDrawOverlays(this);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        AndroidUtilities.checkDisplaySize(this, newConfig);
        super.onConfigurationChanged(newConfig);
        checkLayout();
        PipRoundVideoView pipRoundVideoView = PipRoundVideoView.getInstance();
        if (pipRoundVideoView != null) {
            pipRoundVideoView.onConfigurationChanged();
        }
        EmbedBottomSheet embedBottomSheet = EmbedBottomSheet.getInstance();
        if (embedBottomSheet != null) {
            embedBottomSheet.onConfigurationChanged(newConfig);
        }
        PhotoViewer photoViewer = PhotoViewer.getPipInstance();
        if (photoViewer != null) {
            photoViewer.onConfigurationChanged(newConfig);
        }
        ThemeEditorView editorView = ThemeEditorView.getInstance();
        if (editorView != null) {
            editorView.onConfigurationChanged();
        }
        if (Theme.selectedAutoNightType == Theme.AUTO_NIGHT_TYPE_SYSTEM) {
            Theme.checkAutoNightThemeConditions();
        }
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        AndroidUtilities.isInMultiwindow = isInMultiWindowMode;
        checkLayout();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void didReceivedNotification(int id, final int account, Object... args) {
        Log.d(TAG, "didReceivedNotification: " + id);

        if (id == NotificationCenter.appDidLogout) {
            switchToAvailableAccountOrLogout();
        } else if (id == NotificationCenter.closeOtherAppActivities) {
            if (args[0] != this) {
                onFinish();
                finish();
            }
        } else if (id == NotificationCenter.didUpdateConnectionState) {
            int state = ConnectionsManager.getInstance(account).getConnectionState();
            if (currentConnectionState != state) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("switch to state " + state);
                }
                currentConnectionState = state;
                updateCurrentConnectionState(account);
            }
        } else if (id == NotificationCenter.mainUserInfoChanged) {
            if (!Config.IS_SBERDEVICE) drawerLayoutAdapter.notifyDataSetChanged();
            if (sbdvContainer.getVisibility() != View.VISIBLE) {
                sbdvContainer.setVisibility(View.VISIBLE);
                analyticsCollector.onAppEvent(AppEvent.OPEN_MAIN_SCREEN);
            }
        } else if (id == NotificationCenter.needShowAlert) {
            final Integer reason = (Integer) args[0];
            if (reason == 6 || reason == 3 && proxyErrorDialog != null) {
                return;
            } else if (reason == 4) {
                showTosActivity(account, (TLRPC.TL_help_termsOfService) args[1]);
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
            if (reason != 2 && reason != 3 && reason != 6) {
                builder.setNegativeButton(LocaleController.getString("MoreInfo", R.string.MoreInfo), (dialogInterface, i) -> {
                    if (!mainFragmentsStack.isEmpty()) {
                        MessagesController.getInstance(account).openByUserName("spambot", mainFragmentsStack.get(mainFragmentsStack.size() - 1), 1);
                    }
                });
            }
            if (reason == 5) {
                builder.setMessage(LocaleController.getString("NobodyLikesSpam3", R.string.NobodyLikesSpam3));
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
            } else if (reason == 0) {
                builder.setMessage(LocaleController.getString("NobodyLikesSpam1", R.string.NobodyLikesSpam1));
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
            } else if (reason == 1) {
                builder.setMessage(LocaleController.getString("NobodyLikesSpam2", R.string.NobodyLikesSpam2));
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
            } else if (reason == 2) {
                builder.setMessage((String) args[1]);
                String type = (String) args[2];
                if (type.startsWith("AUTH_KEY_DROP_")) {
                    builder.setPositiveButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    builder.setNegativeButton(LocaleController.getString("LogOut", R.string.LogOut), (dialog, which) -> MessagesController.getInstance(currentAccount).performLogout(2));
                } else {
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                }
            } else if (reason == 3) {
                builder.setTitle(LocaleController.getString("Proxy", R.string.Proxy));
                builder.setMessage(LocaleController.getString("UseProxyTelegramError", R.string.UseProxyTelegramError));
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                proxyErrorDialog = showAlertDialog(builder);
                return;
            }
            if (!mainFragmentsStack.isEmpty()) {
                mainFragmentsStack.get(mainFragmentsStack.size() - 1).showDialog(builder.create());
            }
        } else if (id == NotificationCenter.wasUnableToFindCurrentLocation) {
            final HashMap<String, MessageObject> waitingForLocation = (HashMap<String, MessageObject>) args[0];
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
            builder.setNegativeButton(LocaleController.getString("ShareYouLocationUnableManually", R.string.ShareYouLocationUnableManually), (dialogInterface, i) -> {
                if (mainFragmentsStack.isEmpty()) {
                    return;
                }
                BaseFragment lastFragment = mainFragmentsStack.get(mainFragmentsStack.size() - 1);
                if (!AndroidUtilities.isGoogleMapsInstalled(lastFragment)) {
                    return;
                }
                LocationActivity fragment = new LocationActivity(0);
                fragment.setDelegate((location, live, notify, scheduleDate) -> {
                    for (HashMap.Entry<String, MessageObject> entry : waitingForLocation.entrySet()) {
                        MessageObject messageObject = entry.getValue();
                        SendMessagesHelper.getInstance(account).sendMessage(location, messageObject.getDialogId(), messageObject, null, null, null, notify, scheduleDate);
                    }
                });
                presentFragment(fragment);
            });
            builder.setMessage(LocaleController.getString("ShareYouLocationUnable", R.string.ShareYouLocationUnable));
            if (!mainFragmentsStack.isEmpty()) {
                mainFragmentsStack.get(mainFragmentsStack.size() - 1).showDialog(builder.create());
            }
        } else if (id == NotificationCenter.didSetNewWallpapper) {
            if (sideMenu != null) {
                View child = sideMenu.getChildAt(0);
                if (child != null) {
                    child.invalidate();
                }
            }
        } else if (id == NotificationCenter.didSetPasscode) {
            if (SharedConfig.passcodeHash.length() > 0 && !SharedConfig.allowScreenCapture) {
                try {
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            } else if (!AndroidUtilities.hasFlagSecureFragment()) {
                try {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        } else if (id == NotificationCenter.reloadInterface) {
            boolean last = mainFragmentsStack.size() > 1 && mainFragmentsStack.get(mainFragmentsStack.size() - 1) instanceof ProfileActivity;
            if (last) {
                ProfileActivity profileActivity = (ProfileActivity) mainFragmentsStack.get(mainFragmentsStack.size() - 1);
                if (!profileActivity.isSettings()) {
                    last = false;
                }
            }
            rebuildAllFragments(last);
        } else if (id == NotificationCenter.suggestedLangpack) {
            showLanguageAlert(false);
        } else if (id == NotificationCenter.openArticle) {
            if (mainFragmentsStack.isEmpty()) {
                return;
            }
            ArticleViewer.getInstance().setParentActivity(this, mainFragmentsStack.get(mainFragmentsStack.size() - 1));
            ArticleViewer.getInstance().open((TLRPC.TL_webPage) args[0], (String) args[1]);
        } else if (id == NotificationCenter.hasNewContactsToImport) {
            if (actionBarLayout == null || actionBarLayout.fragmentsStack.isEmpty()) {
                return;
            }
            final int type = (Integer) args[0];
            final HashMap<String, ContactsController.Contact> contactHashMap = (HashMap<String, ContactsController.Contact>) args[1];
            final boolean first = (Boolean) args[2];
            final boolean schedule = (Boolean) args[3];
            BaseFragment fragment = actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1);

            AlertDialog.Builder builder = new AlertDialog.Builder(LaunchActivity.this);
            builder.setTitle(LocaleController.getString("UpdateContactsTitle", R.string.UpdateContactsTitle));
            builder.setMessage(LocaleController.getString("UpdateContactsMessage", R.string.UpdateContactsMessage));
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> ContactsController.getInstance(account).syncPhoneBookByAlert(contactHashMap, first, schedule, false));
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialog, which) -> ContactsController.getInstance(account).syncPhoneBookByAlert(contactHashMap, first, schedule, true));
            builder.setOnBackButtonListener((dialogInterface, i) -> ContactsController.getInstance(account).syncPhoneBookByAlert(contactHashMap, first, schedule, true));
            AlertDialog dialog = builder.create();
            fragment.showDialog(dialog);
            dialog.setCanceledOnTouchOutside(false);
        } else if (id == NotificationCenter.didSetNewTheme) {
            Boolean nightTheme = (Boolean) args[0];
            if (!nightTheme) {
                if (sideMenu != null) {
                    sideMenu.setBackgroundColor(Theme.getColor(Theme.key_chats_menuBackground));
                    sideMenu.setGlowColor(Theme.getColor(Theme.key_chats_menuBackground));
                    sideMenu.setListSelectorColor(Theme.getColor(Theme.key_listSelector));
                    sideMenu.getAdapter().notifyDataSetChanged();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        setTaskDescription(new ActivityManager.TaskDescription(null, null, Theme.getColor(Theme.key_actionBarDefault) | 0xff000000));
                    } catch (Exception ignore) {

                    }
                }
            }
            drawerLayoutContainer.setBehindKeyboardColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            checkSystemBarColors();
        } else if (!Config.IS_SBERDEVICE && id == NotificationCenter.needSetDayNightTheme) {
            boolean instant = false;
            if (Build.VERSION.SDK_INT >= 21 && args[2] != null) {
                if (themeSwitchImageView.getVisibility() == View.VISIBLE) {
                    return;
                }
                try {
                    int[] pos = (int[]) args[2];
                    boolean toDark = (Boolean) args[4];
                    RLottieImageView darkThemeView = (RLottieImageView) args[5];
                    int w = drawerLayoutContainer.getMeasuredWidth();
                    int h = drawerLayoutContainer.getMeasuredHeight();
                    if (!toDark) {
                        darkThemeView.setVisibility(View.INVISIBLE);
                    }
                    Bitmap bitmap = Bitmap.createBitmap(drawerLayoutContainer.getMeasuredWidth(), drawerLayoutContainer.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    drawerLayoutContainer.draw(canvas);
                    frameLayout.removeView(themeSwitchImageView);
                    if (toDark) {
                        frameLayout.addView(themeSwitchImageView, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
                        themeSwitchSunView.setVisibility(View.GONE);
                    } else {
                        frameLayout.addView(themeSwitchImageView, 1, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
                        themeSwitchSunView.setTranslationX(pos[0] - AndroidUtilities.dp(14));
                        themeSwitchSunView.setTranslationY(pos[1] - AndroidUtilities.dp(14));
                        themeSwitchSunView.setVisibility(View.VISIBLE);
                        themeSwitchSunView.invalidate();
                    }
                    themeSwitchImageView.setImageBitmap(bitmap);
                    themeSwitchImageView.setVisibility(View.VISIBLE);
                    themeSwitchSunDrawable = darkThemeView.getAnimatedDrawable();
                    float finalRadius = (float) Math.max(Math.sqrt((w - pos[0]) * (w - pos[0]) + (h - pos[1]) * (h - pos[1])), Math.sqrt(pos[0] * pos[0] + (h - pos[1]) * (h - pos[1])));
                    Animator anim = ViewAnimationUtils.createCircularReveal(toDark ? drawerLayoutContainer : themeSwitchImageView, pos[0], pos[1], toDark ? 0 : finalRadius, toDark ? finalRadius : 0);
                    anim.setDuration(400);
                    anim.setInterpolator(Easings.easeInOutQuad);
                    anim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            themeSwitchImageView.setImageDrawable(null);
                            themeSwitchImageView.setVisibility(View.GONE);
                            themeSwitchSunView.setVisibility(View.GONE);
                            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.themeAccentListUpdated);
                            if (!toDark) {
                                darkThemeView.setVisibility(View.VISIBLE);
                            }
                            DrawerProfileCell.switchingTheme = false;
                        }
                    });
                    anim.start();
                    instant = true;
                } catch (Throwable e) {
                    FileLog.e(e);
                    try {
                        themeSwitchImageView.setImageDrawable(null);
                        frameLayout.removeView(themeSwitchImageView);
                        DrawerProfileCell.switchingTheme = false;
                    } catch (Exception e2) {
                        FileLog.e(e2);
                    }
                }
            }
            Theme.ThemeInfo theme = (Theme.ThemeInfo) args[0];
            boolean nigthTheme = (Boolean) args[1];
            int accentId = (Integer) args[3];
            actionBarLayout.animateThemedValues(theme, accentId, nigthTheme, instant);
            if (AndroidUtilities.isTablet()) {
                layersActionBarLayout.animateThemedValues(theme, accentId, nigthTheme, instant);
                rightActionBarLayout.animateThemedValues(theme, accentId, nigthTheme, instant);
            }
        } else if (!Config.IS_SBERDEVICE && id == NotificationCenter.notificationsCountUpdated) {
            if (sideMenu != null) {
                Integer accountNum = (Integer) args[0];
                int count = sideMenu.getChildCount();
                for (int a = 0; a < count; a++) {
                    View child = sideMenu.getChildAt(a);
                    if (child instanceof DrawerUserCell) {
                        if (((DrawerUserCell) child).getAccountNumber() == accountNum) {
                            child.invalidate();
                            break;
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.needShowPlayServicesAlert) {
            try {
                final Status status = (Status) args[0];
                status.startResolutionForResult(this, PLAY_SERVICES_REQUEST_CHECK_SETTINGS);
            } catch (Throwable ignore) {

            }
        } else if (id == NotificationCenter.fileDidLoad) {
            if (loadingThemeFileName != null) {
                String path = (String) args[0];
                if (loadingThemeFileName.equals(path)) {
                    loadingThemeFileName = null;
                    File locFile = new File(ApplicationLoader.getFilesDirFixed(), "remote" + loadingTheme.id + ".attheme");
                    Theme.ThemeInfo themeInfo = Theme.fillThemeValues(locFile, loadingTheme.title, loadingTheme);
                    if (themeInfo != null) {
                        if (themeInfo.pathToWallpaper != null) {
                            File file = new File(themeInfo.pathToWallpaper);
                            if (!file.exists()) {
                                TLRPC.TL_account_getWallPaper req = new TLRPC.TL_account_getWallPaper();
                                TLRPC.TL_inputWallPaperSlug inputWallPaperSlug = new TLRPC.TL_inputWallPaperSlug();
                                inputWallPaperSlug.slug = themeInfo.slug;
                                req.wallpaper = inputWallPaperSlug;
                                ConnectionsManager.getInstance(themeInfo.account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                                    if (response instanceof TLRPC.TL_wallPaper) {
                                        TLRPC.TL_wallPaper wallPaper = (TLRPC.TL_wallPaper) response;
                                        loadingThemeInfo = themeInfo;
                                        loadingThemeWallpaperName = FileLoader.getAttachFileName(wallPaper.document);
                                        loadingThemeWallpaper = wallPaper;
                                        FileLoader.getInstance(themeInfo.account).loadFile(wallPaper.document, wallPaper, 1, 1);
                                    } else {
                                        onThemeLoadFinish();
                                    }
                                }));
                                return;
                            }
                        }
                        Theme.ThemeInfo finalThemeInfo = Theme.applyThemeFile(locFile, loadingTheme.title, loadingTheme, true);
                        if (finalThemeInfo != null) {
                            presentFragment(new ThemePreviewActivity(finalThemeInfo, true, ThemePreviewActivity.SCREEN_TYPE_PREVIEW, false, false));
                        }
                    }
                    onThemeLoadFinish();
                }
            } else if (loadingThemeWallpaperName != null) {
                String path = (String) args[0];
                if (loadingThemeWallpaperName.equals(path)) {
                    loadingThemeWallpaperName = null;
                    File file = (File) args[1];
                    if (loadingThemeAccent) {
                        openThemeAccentPreview(loadingTheme, loadingThemeWallpaper, loadingThemeInfo);
                        onThemeLoadFinish();
                    } else {
                        Theme.ThemeInfo info = loadingThemeInfo;
                        Utilities.globalQueue.postRunnable(() -> {
                            info.createBackground(file, info.pathToWallpaper);
                            AndroidUtilities.runOnUIThread(() -> {
                                if (loadingTheme == null) {
                                    return;
                                }
                                File locFile = new File(ApplicationLoader.getFilesDirFixed(), "remote" + loadingTheme.id + ".attheme");
                                Theme.ThemeInfo finalThemeInfo = Theme.applyThemeFile(locFile, loadingTheme.title, loadingTheme, true);
                                if (finalThemeInfo != null) {
                                    presentFragment(new ThemePreviewActivity(finalThemeInfo, true, ThemePreviewActivity.SCREEN_TYPE_PREVIEW, false, false));
                                }
                                onThemeLoadFinish();
                            });
                        });
                    }
                }
            }
        } else if (id == NotificationCenter.fileDidFailToLoad) {
            String path = (String) args[0];
            if (path.equals(loadingThemeFileName) || path.equals(loadingThemeWallpaperName)) {
                onThemeLoadFinish();
            }
        } else if (id == NotificationCenter.screenStateChanged) {
            if (ApplicationLoader.mainInterfacePaused) {
                return;
            }
            if (ApplicationLoader.isScreenOn) {
                onPasscodeResume();
            } else {
                onPasscodePause();
            }
        } else if (id == NotificationCenter.needCheckSystemBarColors) {
            checkSystemBarColors();
        }
    }

    private String getStringForLanguageAlert(HashMap<String, String> map, String key, int intKey) {
        String value = map.get(key);
        if (value == null) {
            return LocaleController.getString(key, intKey);
        }
        return value;
    }

    private void openThemeAccentPreview(TLRPC.TL_theme t, TLRPC.TL_wallPaper wallPaper, Theme.ThemeInfo info) {
        int lastId = info.lastAccentId;
        Theme.ThemeAccent accent = info.createNewAccent(t, currentAccount);
        info.prevAccentId = info.currentAccentId;
        info.setCurrentAccentId(accent.id);
        accent.pattern = wallPaper;
        presentFragment(new ThemePreviewActivity(info, lastId != info.lastAccentId, ThemePreviewActivity.SCREEN_TYPE_PREVIEW, false, false));
    }

    private void onThemeLoadFinish() {
        if (loadingThemeProgressDialog != null) {
            try {
                loadingThemeProgressDialog.dismiss();
            } finally {
                loadingThemeProgressDialog = null;
            }
        }
        loadingThemeWallpaperName = null;
        loadingThemeWallpaper = null;
        loadingThemeInfo = null;
        loadingThemeFileName = null;
        loadingTheme = null;
    }

    private void checkFreeDiscSpace() {
        SharedConfig.checkKeepMedia();
        if (Build.VERSION.SDK_INT >= 26) {
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            if (!UserConfig.getInstance(currentAccount).isClientActivated()) {
                return;
            }
            try {
                SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                if (Math.abs(preferences.getLong("last_space_check", 0) - System.currentTimeMillis()) >= 3 * 24 * 3600 * 1000) {
                    File path = FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE);
                    if (path == null) {
                        return;
                    }
                    long freeSpace;
                    StatFs statFs = new StatFs(path.getAbsolutePath());
                    if (Build.VERSION.SDK_INT < 18) {
                        freeSpace = Math.abs(statFs.getAvailableBlocks() * statFs.getBlockSize());
                    } else {
                        freeSpace = statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
                    }
                    if (freeSpace < 1024 * 1024 * 100) {
                        preferences.edit().putLong("last_space_check", System.currentTimeMillis()).commit();
                        AndroidUtilities.runOnUIThread(() -> {
                            try {
                                AlertsCreator.createFreeSpaceDialog(LaunchActivity.this).show();
                            } catch (Throwable ignore) {

                            }
                        });
                    }
                }
            } catch (Throwable ignore) {

            }
        }, 2000);
    }

    private void setPreferredLanguage(@NonNull LocaleController.LocaleInfo systemInfo, @NonNull LocaleController.LocaleInfo englishInfo) {
        Log.d(TAG, "setPreferredLanguage(systemInfo=".concat(systemInfo.nameEnglish).concat(")"));
        LocaleController.LocaleInfo selectedLanguage;
        if ("Russian".toUpperCase().equals(systemInfo.nameEnglish.toUpperCase())) {
            selectedLanguage = systemInfo;
        } else {
            selectedLanguage = englishInfo;
        }
        Log.d(TAG, "selectedLanguage = " + selectedLanguage.nameEnglish);
        LocaleController.getInstance().applyLanguage(selectedLanguage, true, false, currentAccount);
        rebuildAllFragments(true);
    }

    /*
     * Not used. Replaced by setPreferredLanguage(systemInfo, englishInfo)
     */
    private void showLanguageAlertInternal(LocaleController.LocaleInfo systemInfo, LocaleController.LocaleInfo englishInfo, String systemLang) {
        try {
            loadingLocaleDialog = false;
            boolean firstSystem = systemInfo.builtIn || LocaleController.getInstance().isCurrentLocalLocale();
            AlertDialog.Builder builder = new AlertDialog.Builder(LaunchActivity.this);
            builder.setTitle(getStringForLanguageAlert(systemLocaleStrings, "ChooseYourLanguage", R.string.ChooseYourLanguage));
            builder.setSubtitle(getStringForLanguageAlert(englishLocaleStrings, "ChooseYourLanguage", R.string.ChooseYourLanguage));
            LinearLayout linearLayout = new LinearLayout(LaunchActivity.this);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            final LanguageCell[] cells = new LanguageCell[2];
            final LocaleController.LocaleInfo[] selectedLanguage = new LocaleController.LocaleInfo[1];
            final LocaleController.LocaleInfo[] locales = new LocaleController.LocaleInfo[2];
            final String englishName = getStringForLanguageAlert(systemLocaleStrings, "English", R.string.English);
            locales[0] = firstSystem ? systemInfo : englishInfo;
            locales[1] = firstSystem ? englishInfo : systemInfo;
            selectedLanguage[0] = firstSystem ? systemInfo : englishInfo;

            for (int a = 0; a < 2; a++) {
                cells[a] = new LanguageCell(LaunchActivity.this, true);
                cells[a].setLanguage(locales[a], locales[a] == englishInfo ? englishName : null, true);
                cells[a].setTag(a);
                cells[a].setBackgroundDrawable(Theme.createSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), 2));
                cells[a].setLanguageSelected(a == 0);
                linearLayout.addView(cells[a], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));
                cells[a].setOnClickListener(v -> {
                    Integer tag = (Integer) v.getTag();
                    selectedLanguage[0] = ((LanguageCell) v).getCurrentLocale();
                    for (int a1 = 0; a1 < cells.length; a1++) {
                        cells[a1].setLanguageSelected(a1 == tag);
                    }
                });
            }
            LanguageCell cell = new LanguageCell(LaunchActivity.this, true);
            cell.setValue(getStringForLanguageAlert(systemLocaleStrings, "ChooseYourLanguageOther", R.string.ChooseYourLanguageOther), getStringForLanguageAlert(englishLocaleStrings, "ChooseYourLanguageOther", R.string.ChooseYourLanguageOther));
            cell.setOnClickListener(v -> {
                localeDialog = null;
                drawerLayoutContainer.closeDrawer(true);
                presentFragment(new LanguageSelectActivity());
                if (visibleDialog != null) {
                    visibleDialog.dismiss();
                    visibleDialog = null;
                }
            });
            linearLayout.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));
            builder.setView(linearLayout);
            builder.setNegativeButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
                LocaleController.getInstance().applyLanguage(selectedLanguage[0], true, false, currentAccount);
                rebuildAllFragments(true);
            });
            localeDialog = showAlertDialog(builder);
            SharedPreferences preferences = MessagesController.getGlobalMainSettings();
            preferences.edit().putString("language_showed2", systemLang).commit();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void showLanguageAlert(boolean force) {
        try {
            if (loadingLocaleDialog || ApplicationLoader.mainInterfacePaused) {
                return;
            }
            SharedPreferences preferences = MessagesController.getGlobalMainSettings();
            String showedLang = preferences.getString("language_showed2", "");
            final String systemLang = MessagesController.getInstance(currentAccount).suggestedLangCode;
            if (!force && showedLang.equals(systemLang)) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("alert already showed for " + showedLang);
                }
                return;
            }

            final LocaleController.LocaleInfo[] infos = new LocaleController.LocaleInfo[2];
            String arg = systemLang.contains("-") ? systemLang.split("-")[0] : systemLang;
            String alias;
            if ("in".equals(arg)) {
                alias = "id";
            } else if ("iw".equals(arg)) {
                alias = "he";
            } else if ("jw".equals(arg)) {
                alias = "jv";
            } else {
                alias = null;
            }
            for (int a = 0; a < LocaleController.getInstance().languages.size(); a++) {
                LocaleController.LocaleInfo info = LocaleController.getInstance().languages.get(a);
                if (info.shortName.equals("en")) {
                    infos[0] = info;
                }
                if (info.shortName.replace("_", "-").equals(systemLang) || info.shortName.equals(arg) || info.shortName.equals(alias)) {
                    infos[1] = info;
                }
                if (infos[0] != null && infos[1] != null) {
                    break;
                }
            }
            if (infos[0] == null || infos[1] == null || infos[0] == infos[1]) {
                return;
            }
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("show lang alert for " + infos[0].getKey() + " and " + infos[1].getKey());
            }

            systemLocaleStrings = null;
            englishLocaleStrings = null;
            loadingLocaleDialog = true;

            TLRPC.TL_langpack_getStrings req = new TLRPC.TL_langpack_getStrings();
            req.lang_code = infos[1].getLangCode();
            req.keys.add("English");
            req.keys.add("ChooseYourLanguage");
            req.keys.add("ChooseYourLanguageOther");
            req.keys.add("ChangeLanguageLater");
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                final HashMap<String, String> keys = new HashMap<>();
                if (response != null) {
                    TLRPC.Vector vector = (TLRPC.Vector) response;
                    for (int a = 0; a < vector.objects.size(); a++) {
                        final TLRPC.LangPackString string = (TLRPC.LangPackString) vector.objects.get(a);
                        keys.put(string.key, string.value);
                    }
                }
                AndroidUtilities.runOnUIThread(() -> {
                    systemLocaleStrings = keys;
                    if (englishLocaleStrings != null && systemLocaleStrings != null) {
                        // showLanguageAlertInternal(infos[1], infos[0], systemLang);
                        setPreferredLanguage(infos[1], infos[0]);
                    }
                });
            }, ConnectionsManager.RequestFlagWithoutLogin);

            req = new TLRPC.TL_langpack_getStrings();
            req.lang_code = infos[0].getLangCode();
            req.keys.add("English");
            req.keys.add("ChooseYourLanguage");
            req.keys.add("ChooseYourLanguageOther");
            req.keys.add("ChangeLanguageLater");
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                final HashMap<String, String> keys = new HashMap<>();
                if (response != null) {
                    TLRPC.Vector vector = (TLRPC.Vector) response;
                    for (int a = 0; a < vector.objects.size(); a++) {
                        final TLRPC.LangPackString string = (TLRPC.LangPackString) vector.objects.get(a);
                        keys.put(string.key, string.value);
                    }
                }
                AndroidUtilities.runOnUIThread(() -> {
                    englishLocaleStrings = keys;
                    if (englishLocaleStrings != null && systemLocaleStrings != null) {
                        // showLanguageAlertInternal(infos[1], infos[0], systemLang);
                        setPreferredLanguage(infos[1], infos[0]);
                    }
                });
            }, ConnectionsManager.RequestFlagWithoutLogin);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void onPasscodePause() {
        Log.d(TAG, "onPasscodePause");

        if (lockRunnable != null) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("cancel lockRunnable onPasscodePause");
            }
            AndroidUtilities.cancelRunOnUIThread(lockRunnable);
            lockRunnable = null;
        }
        if (SharedConfig.passcodeHash.length() != 0) {
            SharedConfig.lastPauseTime = (int) (SystemClock.elapsedRealtime() / 1000);
            lockRunnable = new Runnable() {
                @Override
                public void run() {
                    if (lockRunnable == this) {
                        if (AndroidUtilities.needShowPasscode(true)) {
                            if (BuildVars.LOGS_ENABLED) {
                                FileLog.d("lock app");
                            }
                            showPasscodeActivity();
                        } else {
                            if (BuildVars.LOGS_ENABLED) {
                                FileLog.d("didn't pass lock check");
                            }
                        }
                        lockRunnable = null;
                    }
                }
            };
            if (SharedConfig.appLocked) {
                AndroidUtilities.runOnUIThread(lockRunnable, 1000);
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("schedule app lock in " + 1000);
                }
            } else if (SharedConfig.autoLockIn != 0) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("schedule app lock in " + (((long) SharedConfig.autoLockIn) * 1000 + 1000));
                }
                AndroidUtilities.runOnUIThread(lockRunnable, ((long) SharedConfig.autoLockIn) * 1000 + 1000);
            }
        } else {
            SharedConfig.lastPauseTime = 0;
        }
        SharedConfig.saveConfig();
    }

    private void onPasscodeResume() {
        Log.d(TAG, "onPasscodeResume");

        if (lockRunnable != null) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("cancel lockRunnable onPasscodeResume");
            }
            AndroidUtilities.cancelRunOnUIThread(lockRunnable);
            lockRunnable = null;
        }
        if (AndroidUtilities.needShowPasscode(true)) {
            showPasscodeActivity();
        }
        if (SharedConfig.lastPauseTime != 0) {
            SharedConfig.lastPauseTime = 0;
            SharedConfig.saveConfig();
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("reset lastPauseTime onPasscodeResume");
            }
        }
    }

    private void updateCurrentConnectionState(int account) {
        if (actionBarLayout == null) {
            return;
        }
        String title = null;
        int titleId = 0;
        Runnable action = null;
        currentConnectionState = ConnectionsManager.getInstance(currentAccount).getConnectionState();
        if (currentConnectionState == ConnectionsManager.ConnectionStateWaitingForNetwork) {
            title = "WaitingForNetwork";
            titleId = R.string.WaitingForNetwork;
        } else if (currentConnectionState == ConnectionsManager.ConnectionStateUpdating) {
            title = "Updating";
            titleId = R.string.Updating;
        } else if (currentConnectionState == ConnectionsManager.ConnectionStateConnectingToProxy) {
            title = "ConnectingToProxy";
            titleId = R.string.ConnectingToProxy;
        } else if (currentConnectionState == ConnectionsManager.ConnectionStateConnecting) {
            title = "Connecting";
            titleId = R.string.Connecting;
        }
        if (currentConnectionState == ConnectionsManager.ConnectionStateConnecting || currentConnectionState == ConnectionsManager.ConnectionStateConnectingToProxy) {
            action = () -> {
                BaseFragment lastFragment = null;
                if (AndroidUtilities.isTablet()) {
                    if (!layerFragmentsStack.isEmpty()) {
                        lastFragment = layerFragmentsStack.get(layerFragmentsStack.size() - 1);
                    }
                } else {
                    if (!mainFragmentsStack.isEmpty()) {
                        lastFragment = mainFragmentsStack.get(mainFragmentsStack.size() - 1);
                    }
                }
                if (lastFragment instanceof ProxyListActivity || lastFragment instanceof ProxySettingsActivity) {
                    return;
                }
                presentFragment(new ProxyListActivity());
            };
        }
        actionBarLayout.setTitleOverlayText(title, titleId, action);
    }

    public void hideVisibleActionMode() {
        if (visibleActionMode == null) {
            return;
        }
        visibleActionMode.finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        try {
            super.onSaveInstanceState(outState);
            BaseFragment lastFragment = null;
            if (AndroidUtilities.isTablet()) {
                if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                    lastFragment = layersActionBarLayout.fragmentsStack.get(layersActionBarLayout.fragmentsStack.size() - 1);
                } else if (!rightActionBarLayout.fragmentsStack.isEmpty()) {
                    lastFragment = rightActionBarLayout.fragmentsStack.get(rightActionBarLayout.fragmentsStack.size() - 1);
                } else if (!actionBarLayout.fragmentsStack.isEmpty()) {
                    lastFragment = actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1);
                }
            } else {
                if (!actionBarLayout.fragmentsStack.isEmpty()) {
                    lastFragment = actionBarLayout.fragmentsStack.get(actionBarLayout.fragmentsStack.size() - 1);
                }
            }

            if (lastFragment != null) {
                Bundle args = lastFragment.getArguments();
                if (lastFragment instanceof ChatActivity && args != null) {
                    outState.putBundle("args", args);
                    outState.putString("fragment", "chat");
                } else if (lastFragment instanceof GroupCreateFinalActivity && args != null) {
                    outState.putBundle("args", args);
                    outState.putString("fragment", "group");
                } else if (lastFragment instanceof WallpapersListActivity) {
                    outState.putString("fragment", "wallpapers");
                } else if (lastFragment instanceof ProfileActivity) {
                    ProfileActivity profileActivity = (ProfileActivity) lastFragment;
                    if (profileActivity.isSettings()) {
                        outState.putString("fragment", "settings");
                    } else if (profileActivity.isChat() && args != null) {
                        outState.putBundle("args", args);
                        outState.putString("fragment", "chat_profile");
                    }
                } else if (lastFragment instanceof ChannelCreateActivity && args != null && args.getInt("step") == 0) {
                    outState.putBundle("args", args);
                    outState.putString("fragment", "channel");
                }
                lastFragment.saveSelfArgs(outState);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");

        if (!Config.IS_SBERDEVICE && passcodeView.getVisibility() == View.VISIBLE) {
            finish();
            return;
        }
        if (SecretMediaViewer.hasInstance() && SecretMediaViewer.getInstance().isVisible()) {
            SecretMediaViewer.getInstance().closePhoto(true, false);
        } else if (PhotoViewer.hasInstance() && PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().closePhoto(true, false);
        } else if (ArticleViewer.hasInstance() && ArticleViewer.getInstance().isVisible()) {
            ArticleViewer.getInstance().close(true, false);
        } else if (drawerLayoutContainer.isDrawerOpened()) {
            drawerLayoutContainer.closeDrawer(false);
        } else if (AndroidUtilities.isTablet()) {
            if (layersActionBarLayout.getVisibility() == View.VISIBLE) {
                layersActionBarLayout.onBackPressed();
            } else {
                boolean cancel = false;
                if (rightActionBarLayout.getVisibility() == View.VISIBLE && !rightActionBarLayout.fragmentsStack.isEmpty()) {
                    BaseFragment lastFragment = rightActionBarLayout.fragmentsStack.get(rightActionBarLayout.fragmentsStack.size() - 1);
                    cancel = !lastFragment.onBackPressed();
                }
                if (!cancel) {
                    if (!sbdvContainer.onBackPressed()) {
                        actionBarLayout.onBackPressed();
                        if (actionBarLayout.fragmentsStack.isEmpty()){
                            Log.d(TAG, "Empty fragment stack. Close activity..");
                            onFinish();
                            finish();
                        }
                    }
                }
            }
        } else {
            actionBarLayout.onBackPressed();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (actionBarLayout != null) {
            actionBarLayout.onLowMemory();
            if (AndroidUtilities.isTablet()) {
                rightActionBarLayout.onLowMemory();
                layersActionBarLayout.onLowMemory();
            }
        }
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        visibleActionMode = mode;
        try {
            Menu menu = mode.getMenu();
            if (menu != null) {
                boolean extended = actionBarLayout.extendActionMode(menu);
                if (!extended && AndroidUtilities.isTablet()) {
                    extended = rightActionBarLayout.extendActionMode(menu);
                    if (!extended) {
                        layersActionBarLayout.extendActionMode(menu);
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (Build.VERSION.SDK_INT >= 23 && mode.getType() == ActionMode.TYPE_FLOATING) {
            return;
        }
        actionBarLayout.onActionModeStarted(mode);
        if (AndroidUtilities.isTablet()) {
            rightActionBarLayout.onActionModeStarted(mode);
            layersActionBarLayout.onActionModeStarted(mode);
        }
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        if (visibleActionMode == mode) {
            visibleActionMode = null;
        }
        if (Build.VERSION.SDK_INT >= 23 && mode.getType() == ActionMode.TYPE_FLOATING) {
            return;
        }
        actionBarLayout.onActionModeFinished(mode);
        if (AndroidUtilities.isTablet()) {
            rightActionBarLayout.onActionModeFinished(mode);
            layersActionBarLayout.onActionModeFinished(mode);
        }
    }

    @Override
    public boolean onPreIme() {
        if (SecretMediaViewer.hasInstance() && SecretMediaViewer.getInstance().isVisible()) {
            SecretMediaViewer.getInstance().closePhoto(true, false);
            return true;
        } else if (PhotoViewer.hasInstance() && PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().closePhoto(true, false);
            return true;
        } else if (ArticleViewer.hasInstance() && ArticleViewer.getInstance().isVisible()) {
            ArticleViewer.getInstance().close(true, false);
            return true;
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (VoIPService.getSharedInstance() == null && !mainFragmentsStack.isEmpty() && (!PhotoViewer.hasInstance() || !PhotoViewer.getInstance().isVisible()) && event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN && (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            BaseFragment fragment = mainFragmentsStack.get(mainFragmentsStack.size() - 1);
            if (fragment instanceof ChatActivity) {
                if (((ChatActivity) fragment).maybePlayVisibleVideo()) {
                    return true;
                }
            }
            if (AndroidUtilities.isTablet() && !rightFragmentsStack.isEmpty()) {
                fragment = rightFragmentsStack.get(rightFragmentsStack.size() - 1);
                if (fragment instanceof ChatActivity) {
                    if (((ChatActivity) fragment).maybePlayVisibleVideo()) {
                        return true;
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && !SharedConfig.isWaitingForPasscodeEnter) {
            if (PhotoViewer.hasInstance() && PhotoViewer.getInstance().isVisible()) {
                return super.onKeyUp(keyCode, event);
            } else if (ArticleViewer.hasInstance() && ArticleViewer.getInstance().isVisible()) {
                return super.onKeyUp(keyCode, event);
            }
            if (AndroidUtilities.isTablet()) {
                if (layersActionBarLayout.getVisibility() == View.VISIBLE && !layersActionBarLayout.fragmentsStack.isEmpty()) {
                    layersActionBarLayout.onKeyUp(keyCode, event);
                } else if (rightActionBarLayout.getVisibility() == View.VISIBLE && !rightActionBarLayout.fragmentsStack.isEmpty()) {
                    rightActionBarLayout.onKeyUp(keyCode, event);
                } else {
                    actionBarLayout.onKeyUp(keyCode, event);
                }
            } else {
                if (actionBarLayout.fragmentsStack.size() == 1) {
                    if (!drawerLayoutContainer.isDrawerOpened()) {
                        if (getCurrentFocus() != null) {
                            AndroidUtilities.hideKeyboard(getCurrentFocus());
                        }
                        drawerLayoutContainer.openDrawer(false);
                    } else {
                        drawerLayoutContainer.closeDrawer(false);
                    }
                } else {
                    actionBarLayout.onKeyUp(keyCode, event);
                }
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean needPresentFragment(BaseFragment fragment, boolean removeLast, boolean forceWithoutAnimation, ActionBarLayout layout) {
        if (ArticleViewer.hasInstance() && ArticleViewer.getInstance().isVisible()) {
            ArticleViewer.getInstance().close(false, true);
        }
        if (AndroidUtilities.isTablet()) {
            drawerLayoutContainer.setAllowOpenDrawer(!(fragment instanceof LoginActivity || fragment instanceof CountrySelectActivity) && layersActionBarLayout.getVisibility() != View.VISIBLE, true);
            if (fragment instanceof DialogsActivity) {
                DialogsActivity dialogsActivity = (DialogsActivity) fragment;
                if (dialogsActivity.isMainDialogList() && layout != actionBarLayout) {
                    actionBarLayout.removeAllFragments();
                    actionBarLayout.presentFragment(fragment, removeLast, forceWithoutAnimation, false, false);
                    layersActionBarLayout.removeAllFragments();
                    layersActionBarLayout.setVisibility(View.GONE);
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                    if (!Config.IS_SBERDEVICE && !tabletFullSize) {
                        shadowTabletSide.setVisibility(View.VISIBLE);
                        if (rightActionBarLayout.fragmentsStack.isEmpty()) {
                            backgroundTablet.setVisibility(View.VISIBLE);
                        }
                    }
                    return false;
                }
            }
            if (fragment instanceof ChatActivity && !((ChatActivity) fragment).isInScheduleMode()) {
                if (!tabletFullSize && layout == rightActionBarLayout || tabletFullSize && layout == actionBarLayout) {
                    boolean result = !(tabletFullSize && layout == actionBarLayout && actionBarLayout.fragmentsStack.size() == 1);
                    if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                        for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                            layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                            a--;
                        }
                        layersActionBarLayout.closeLastFragment(!forceWithoutAnimation);
                    }
                    if (!result) {
                        actionBarLayout.presentFragment(fragment, false, forceWithoutAnimation, false, false);
                    }
                    return result;
                } else if (!tabletFullSize && layout != rightActionBarLayout) {
                    rightActionBarLayout.setVisibility(View.VISIBLE);
                    backgroundTablet.setVisibility(View.GONE);
                    rightActionBarLayout.removeAllFragments();
                    rightActionBarLayout.presentFragment(fragment, removeLast, true, false, false);
                    if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                        for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                            layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                            a--;
                        }
                        layersActionBarLayout.closeLastFragment(!forceWithoutAnimation);
                    }
                    return false;
                } else if (tabletFullSize && layout != actionBarLayout) {
                    actionBarLayout.presentFragment(fragment, actionBarLayout.fragmentsStack.size() > 1, forceWithoutAnimation, false, false);
                    if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                        for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                            layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                            a--;
                        }
                        layersActionBarLayout.closeLastFragment(!forceWithoutAnimation);
                    }
                    return false;
                } else {
                    if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                        for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                            layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                            a--;
                        }
                        layersActionBarLayout.closeLastFragment(!forceWithoutAnimation);
                    }
                    actionBarLayout.presentFragment(fragment, actionBarLayout.fragmentsStack.size() > 1, forceWithoutAnimation, false, false);
                    return false;
                }
            } else if (layout != layersActionBarLayout) {
                layersActionBarLayout.setVisibility(View.VISIBLE);
                drawerLayoutContainer.setAllowOpenDrawer(false, true);
                if (fragment instanceof LoginActivity) {
                    backgroundTablet.setVisibility(View.VISIBLE);
                    if (!Config.IS_SBERDEVICE) {
                    shadowTabletSide.setVisibility(View.GONE);
                    shadowTablet.setBackgroundColor(0x00000000);
                    }
                } else {
                    if (!Config.IS_SBERDEVICE) shadowTablet.setBackgroundColor(0x7f000000);
                }
                layersActionBarLayout.presentFragment(fragment, removeLast, forceWithoutAnimation, false, false);
                return false;
            }
        } else {
            boolean allow = true;
            if (fragment instanceof LoginActivity) {
                if (mainFragmentsStack.size() == 0) {
                    allow = false;
                }
            } else if (fragment instanceof CountrySelectActivity) {
                if (mainFragmentsStack.size() == 1) {
                    allow = false;
                }
            }
            drawerLayoutContainer.setAllowOpenDrawer(allow, false);
        }
        return true;
    }

    @Override
    public boolean needAddFragmentToStack(BaseFragment fragment, ActionBarLayout layout) {
        if (AndroidUtilities.isTablet()) {
            drawerLayoutContainer.setAllowOpenDrawer(!(fragment instanceof LoginActivity || fragment instanceof CountrySelectActivity) && layersActionBarLayout.getVisibility() != View.VISIBLE, true);
            if (fragment instanceof DialogsActivity) {
                DialogsActivity dialogsActivity = (DialogsActivity) fragment;
                if (dialogsActivity.isMainDialogList() && layout != actionBarLayout) {
                    actionBarLayout.removeAllFragments();
                    actionBarLayout.addFragmentToStack(fragment);
                    layersActionBarLayout.removeAllFragments();
                    layersActionBarLayout.setVisibility(View.GONE);
                    drawerLayoutContainer.setAllowOpenDrawer(true, false);
                    if (!tabletFullSize) {
                        if (!Config.IS_SBERDEVICE) shadowTabletSide.setVisibility(View.VISIBLE);
                        if (rightActionBarLayout.fragmentsStack.isEmpty()) {
                            backgroundTablet.setVisibility(View.VISIBLE);
                        }
                    }
                    return false;
                }
            } else if (fragment instanceof ChatActivity && !((ChatActivity) fragment).isInScheduleMode()) {
                if (!tabletFullSize && layout != rightActionBarLayout) {
                    rightActionBarLayout.setVisibility(View.VISIBLE);
                    backgroundTablet.setVisibility(View.GONE);
                    rightActionBarLayout.removeAllFragments();
                    rightActionBarLayout.addFragmentToStack(fragment);
                    if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                        for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                            layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                            a--;
                        }
                        layersActionBarLayout.closeLastFragment(true);
                    }
                    return false;
                } else if (tabletFullSize && layout != actionBarLayout) {
                    actionBarLayout.addFragmentToStack(fragment);
                    if (!layersActionBarLayout.fragmentsStack.isEmpty()) {
                        for (int a = 0; a < layersActionBarLayout.fragmentsStack.size() - 1; a++) {
                            layersActionBarLayout.removeFragmentFromStack(layersActionBarLayout.fragmentsStack.get(0));
                            a--;
                        }
                        layersActionBarLayout.closeLastFragment(true);
                    }
                    return false;
                }
            } else if (layout != layersActionBarLayout) {
                layersActionBarLayout.setVisibility(View.VISIBLE);
                drawerLayoutContainer.setAllowOpenDrawer(false, true);
                if (fragment instanceof LoginActivity) {
                    backgroundTablet.setVisibility(View.VISIBLE);
                    if (!Config.IS_SBERDEVICE){
                    shadowTabletSide.setVisibility(View.GONE);
                    shadowTablet.setBackgroundColor(0x00000000);
                    }
                } else {
                    if (!Config.IS_SBERDEVICE) shadowTablet.setBackgroundColor(0x7f000000);
                }
                layersActionBarLayout.addFragmentToStack(fragment);
                return false;
            }
        } else {
            boolean allow = true;
            if (fragment instanceof LoginActivity) {
                if (mainFragmentsStack.size() == 0) {
                    allow = false;
                }
            } else if (fragment instanceof CountrySelectActivity) {
                if (mainFragmentsStack.size() == 1) {
                    allow = false;
                }
            }
            drawerLayoutContainer.setAllowOpenDrawer(allow, false);
        }
        return true;
    }

    @Override
    public boolean needCloseLastFragment(ActionBarLayout layout) {
        if (AndroidUtilities.isTablet()) {
            if (layout == actionBarLayout && layout.fragmentsStack.size() <= 1) {
                onFinish();
                finish();
                return false;
            } else if (layout == rightActionBarLayout) {
                if (!tabletFullSize) {
                    backgroundTablet.setVisibility(View.VISIBLE);
                }
            } else if (layout == layersActionBarLayout && actionBarLayout.fragmentsStack.isEmpty() && layersActionBarLayout.fragmentsStack.size() == 1) {
                onFinish();
                finish();
                return false;
            }
        } else {
            if (layout.fragmentsStack.size() <= 1) {
                onFinish();
                finish();
                return false;
            }
            if (layout.fragmentsStack.size() >= 2 && !(layout.fragmentsStack.get(0) instanceof LoginActivity)) {
                drawerLayoutContainer.setAllowOpenDrawer(true, false);
            }
        }
        return true;
    }

    public void rebuildAllFragments(boolean last) {
        if (layersActionBarLayout != null) {
            layersActionBarLayout.rebuildAllFragmentViews(last, last);
        } else {
            actionBarLayout.rebuildAllFragmentViews(last, last);
        }
    }

    @Override
    public void onRebuildAllFragments(ActionBarLayout layout, boolean last) {
        if (AndroidUtilities.isTablet()) {
            if (layout == layersActionBarLayout) {
                rightActionBarLayout.rebuildAllFragmentViews(last, last);
                actionBarLayout.rebuildAllFragmentViews(last, last);
            }
        }
        if (!Config.IS_SBERDEVICE) drawerLayoutAdapter.notifyDataSetChanged();
    }
}
