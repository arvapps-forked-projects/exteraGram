/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2022.

*/

package com.exteragram.messenger;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.exteragram.messenger.updater.UpdaterUtils;

import org.json.JSONArray;
import org.json.JSONTokener;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CombinedDrawable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ExteraUtils {

    public static final DispatchQueue translateQueue = new DispatchQueue("translateQueue", false);

    public static Drawable drawFab() {
        return drawFab(false);
    }

    public static Drawable drawFab(boolean altColor) {
        int r = AndroidUtilities.dp(ExteraConfig.squareFab ? 16 : 100);
        int c = Theme.getColor(altColor ? Theme.key_dialogFloatingButton : Theme.key_chats_actionBackground);
        int pc = Theme.getColor(altColor ? Theme.key_dialogFloatingButtonPressed : Theme.key_chats_actionPressedBackground);
        return Theme.createSimpleSelectorRoundRectDrawable(r, c, pc);
    }

    public static String getDC(TLRPC.User user) {
        return getDC(user, null);
    }

    public static String getDC(TLRPC.Chat chat) {
        return getDC(null, chat);
    }

    // thx to @Owlgram for idea
    public static String getDC(TLRPC.User user, TLRPC.Chat chat) {
        int DC = 0;
        int myDC = AccountInstance.getInstance(UserConfig.selectedAccount).getConnectionsManager().getCurrentDatacenterId();
        if (user != null) {
            if (UserObject.isUserSelf(user) && myDC != -1) {
                DC = myDC;
            } else {
                DC = user.photo != null ? user.photo.dc_id : -1;
            }
        } else if (chat != null) {
            DC = chat.photo != null ? chat.photo.dc_id : -1;
        }
        if (DC == -1 || DC == 0) {
            return getDCName(DC);
        } else {
            return String.format(Locale.ROOT, "DC%d, %s", DC, getDCName(DC));
        }
    }

    public static String getDCName(int dc) {
        switch (dc) {
            case 1:
            case 3:
                return "Miami FL, USA";
            case 2:
            case 4:
                return "Amsterdam, NL";
            case 5:
                return "Singapore, SG";
            default:
                return LocaleController.getString("NumberUnknown", R.string.NumberUnknown);
        }
    }

    public static String getAppName() {
        String beta = BuildVars.isBetaApp() ? " β" : "";
        return LocaleController.getString("exteraAppName", R.string.exteraAppName) + beta;
    }

    public static boolean notSubbedTo(long id) {
        TLRPC.Chat chat = MessagesController.getInstance(UserConfig.selectedAccount).getChat(id);
        return chat == null || chat.left || chat.kicked;
    }

    public static int[] getDrawerIconPack() {
        switch (Theme.getEventType()) {
            case 0:
                return new int[]{
                        R.drawable.msg_groups_ny,
                        R.drawable.msg_secret_ny,
                        R.drawable.msg_channel_ny,
                        R.drawable.msg_contacts_ny,
                        R.drawable.msg_calls_ny,
                        R.drawable.msg_saved_ny,
                        R.drawable.msg_invite_ny,
                        R.drawable.msg_help_ny,
                        R.drawable.msg_nearby_ny
                };
            case 1:
                return new int[]{
                        R.drawable.msg_groups_14,
                        R.drawable.msg_secret_14,
                        R.drawable.msg_channel_14,
                        R.drawable.msg_contacts_14,
                        R.drawable.msg_calls_14,
                        R.drawable.msg_saved_14,
                        R.drawable.msg_invite_14,
                        R.drawable.msg_help_14,
                        R.drawable.msg_nearby_14
                };
            case 2:
                return new int[]{
                        R.drawable.msg_groups_hw,
                        R.drawable.msg_secret_hw,
                        R.drawable.msg_channel_hw,
                        R.drawable.msg_contacts_hw,
                        R.drawable.msg_calls_hw,
                        R.drawable.msg_saved_hw,
                        R.drawable.msg_invite_hw,
                        R.drawable.msg_help_hw,
                        R.drawable.msg_nearby_hw
                };
            default:
                return new int[]{
                        R.drawable.msg_groups,
                        R.drawable.msg_secret,
                        R.drawable.msg_channel,
                        R.drawable.msg_contacts,
                        R.drawable.msg_calls,
                        R.drawable.msg_saved,
                        R.drawable.msg_invite,
                        R.drawable.msg_help,
                        R.drawable.msg_nearby
                };
        }
    }

    public static int getNotificationIconColor() {
        return BuildVars.isBetaApp() ? 0xff747f9f : 0xfff54142;
    }

    public interface OnTranslationSuccess {
        void run(CharSequence translated);
    }

    public interface OnTranslationFail {
        void run();
    }

    public static void translate(CharSequence text, String target, OnTranslationSuccess onSuccess, OnTranslationFail onFail) {
        if (!translateQueue.isAlive()) {
            translateQueue.start();
        }
        translateQueue.postRunnable(() -> {
            String uri;
            HttpURLConnection connection;
            try {
                uri = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=";
                uri += Uri.encode(target);
                uri += "&dt=t&ie=UTF-8&oe=UTF-8&otf=1&ssel=0&tsel=0&kc=7&dt=at&dt=bd&dt=ex&dt=ld&dt=md&dt=qca&dt=rw&dt=rm&dt=ss&q=";
                uri += Uri.encode(text.toString());
                connection = (HttpURLConnection) new URI(uri).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", UpdaterUtils.getRandomUserAgent());
                connection.setRequestProperty("Content-Type", "application/json");

                StringBuilder textBuilder = new StringBuilder();
                try (Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    int c;
                    while ((c = reader.read()) != -1) textBuilder.append((char) c);
                }
                JSONTokener tokener = new JSONTokener(textBuilder.toString());
                JSONArray array = new JSONArray(tokener);
                JSONArray array1 = array.getJSONArray(0);
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < array1.length(); ++i) {
                    String blockText = array1.getJSONArray(i).getString(0);
                    if (blockText != null && !blockText.equals("null"))
                        result.append(blockText);
                }
                if (text.length() > 0 && text.charAt(0) == '\n') result.insert(0, "\n");
                if (onSuccess != null)
                    AndroidUtilities.runOnUIThread(() -> onSuccess.run(result.toString()));
            } catch (Exception e) {
                e.printStackTrace();
                if (onFail != null)
                    AndroidUtilities.runOnUIThread(onFail::run);
            }
        });
    }

    public static String getActionBarTitle() {
        return getActionBarTitle(ExteraConfig.actionBarTitle);
    }
    public static String getActionBarTitle(int num) {
        TLRPC.User user = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
        String title;
        if (num == 0) {
            title = LocaleController.getString("exteraAppName", R.string.exteraAppName);
        } else if (num == 1) {
            title = LocaleController.getString("SearchAllChatsShort", R.string.SearchAllChatsShort);
        } else if (num == 2) {
            if (!TextUtils.isEmpty(UserObject.getPublicUsername(user))) {
                title = UserObject.getPublicUsername(user);
            } else {
                title = UserObject.getFirstName(user);
            }
        } else {
            title = UserObject.getFirstName(user);
        }
        return title;
    }

    public static boolean hasGps() {
        boolean hasGps;
        try {
            hasGps = ApplicationLoader.applicationContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        } catch (Throwable e) {
            hasGps = false;
        }
        return hasGps;
    }

    public static CombinedDrawable createCircleDrawableWithIcon(Context context, int iconRes, int size) {
        Drawable drawable = iconRes != 0 ? ContextCompat.getDrawable(context, iconRes).mutate() : null;
        OvalShape ovalShape = new OvalShape();
        ovalShape.resize(size, size);
        ShapeDrawable defaultDrawable = new ShapeDrawable(ovalShape);
        Paint paint = defaultDrawable.getPaint();
        paint.setColor(0xffffffff);
        CombinedDrawable combinedDrawable = new CombinedDrawable(defaultDrawable, drawable);
        combinedDrawable.setCustomSize(size, size);
        return combinedDrawable;
    }
}