package com.example.randomwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class RandomGeneratorWidget extends AppWidgetProvider {

    public static final String ACTION_GENERATE = "com.example.randomwidget.ACTION_GENERATE";
    public static final String ACTION_CHANGE_MODE = "com.example.randomwidget.ACTION_CHANGE_MODE";
    public static final String ACTION_SHOW_HISTORY = "com.example.randomwidget.ACTION_SHOW_HISTORY";
    public static final String ACTION_OPEN_SETTINGS = "com.example.randomwidget.ACTION_OPEN_SETTINGS";

    public static final int MODE_NUMBER = 0;
    public static final int MODE_COIN = 1;
    public static final int MODE_DICE = 2;

    public static final String PREFS_NAME = "com.example.randomwidget.RandomGeneratorWidget";
    private static final String PREF_MODE_KEY = "mode_";
    private static final String PREF_MIN_KEY = "min_";
    private static final String PREF_MAX_KEY = "max_";
    private static final String PREF_HISTORY_PREFIX = "history_";
    private static final String PREF_HISTORY_COUNT = "history_count";
    private static final String PREF_ANIMATION_SPEED = "animation_speed_";
    private static final int MAX_HISTORY_SIZE = 10;

    private static final int ANIMATION_DELAY_BASE = 100;
    private static final int ANIMATION_FRAMES = 10;

    private static final int[] DICE_RESOURCES = {
            R.drawable.dice_1,
            R.drawable.dice_2,
            R.drawable.dice_3,
            R.drawable.dice_4,
            R.drawable.dice_5,
            R.drawable.dice_6
    };

    private static final int[] COIN_RESOURCES = {
            R.drawable.coin_heads,
            R.drawable.coin_tails
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, false);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, RandomGeneratorWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        if (appWidgetIds != null && appWidgetIds.length > 0) {
            int appWidgetId = appWidgetIds[0];

            if (ACTION_GENERATE.equals(intent.getAction())) {
                // Generate new random value with animation
                generateRandomValue(context, appWidgetManager, appWidgetId);
            } else if (ACTION_CHANGE_MODE.equals(intent.getAction())) {
                // Cycle through modes
                cycleMode(context, appWidgetManager, appWidgetId);
            } else if (ACTION_SHOW_HISTORY.equals(intent.getAction())) {
                // Show history activity
                Intent historyIntent = new Intent(context, HistoryActivity.class);
                historyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(historyIntent);
            } else if (ACTION_OPEN_SETTINGS.equals(intent.getAction())) {
                // Open settings activity
                Intent settingsIntent = new Intent(context, WidgetSettingsActivity.class);
                settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(settingsIntent);
            }
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        for (int appWidgetId : appWidgetIds) {
            prefs.remove(PREF_MODE_KEY + appWidgetId);
            prefs.remove(PREF_MIN_KEY + appWidgetId);
            prefs.remove(PREF_MAX_KEY + appWidgetId);
            prefs.remove(PREF_ANIMATION_SPEED + appWidgetId);

            SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, 0);
            int historyCount = sharedPreferences.getInt(PREF_HISTORY_COUNT + appWidgetId, 0);
            for (int i = 0; i < historyCount; i++) {
                prefs.remove(PREF_HISTORY_PREFIX + appWidgetId + "_" + i);
            }
            prefs.remove(PREF_HISTORY_COUNT + appWidgetId);
        }
        prefs.apply();
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, boolean animate) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.random_generator_widget);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        int mode = prefs.getInt(PREF_MODE_KEY + appWidgetId, MODE_NUMBER);
        int min = prefs.getInt(PREF_MIN_KEY + appWidgetId, 1);
        int max = prefs.getInt(PREF_MAX_KEY + appWidgetId, 100);

        String modeText = getModeText(mode);
        views.setTextViewText(R.id.widget_mode, modeText);

        setUpButtons(context, views, appWidgetId);

        configureWidgetDisplay(views, mode);

        if (animate) {
            startAnimation(context, appWidgetManager, views, appWidgetId, mode, min, max);
        } else {
            String result = prefs.getString(PREF_HISTORY_PREFIX + appWidgetId + "_0", getDefaultResult(mode));
            updateWidgetWithResult(views, mode, result);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private static void configureWidgetDisplay(RemoteViews views, int mode) {
        if (mode == MODE_NUMBER) {
            views.setViewVisibility(R.id.widget_result, View.VISIBLE);
            views.setViewVisibility(R.id.dice_image, View.GONE);
            views.setViewVisibility(R.id.coin_image, View.GONE);
        } else if (mode == MODE_DICE) {
            views.setViewVisibility(R.id.widget_result, View.GONE);
            views.setViewVisibility(R.id.dice_image, View.VISIBLE);
            views.setViewVisibility(R.id.coin_image, View.GONE);
        } else if (mode == MODE_COIN) {
            views.setViewVisibility(R.id.widget_result, View.GONE);
            views.setViewVisibility(R.id.dice_image, View.GONE);
            views.setViewVisibility(R.id.coin_image, View.VISIBLE);
        }
    }

    private static void startAnimation(final Context context, final AppWidgetManager appWidgetManager,
                                       final RemoteViews views, final int appWidgetId,
                                       final int mode, final int min, final int max) {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Random random = new Random();
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);

        int animationSpeed = prefs.getInt(PREF_ANIMATION_SPEED + appWidgetId, 5);
        int animationDelay = ANIMATION_DELAY_BASE * (11 - animationSpeed);

        final String finalResult;
        final int finalDiceValue;
        final boolean finalCoinIsHeads;

        switch (mode) {
            case MODE_COIN:
                finalCoinIsHeads = random.nextBoolean();
                finalResult = finalCoinIsHeads ? "Орел" : "Решка";
                finalDiceValue = 1;
                break;
            case MODE_DICE:
                finalDiceValue = random.nextInt(6) + 1;
                finalResult = String.valueOf(finalDiceValue);
                finalCoinIsHeads = true;
                break;
            case MODE_NUMBER:
            default:
                finalResult = String.valueOf(min + random.nextInt(max - min + 1));
                finalDiceValue = 1;
                finalCoinIsHeads = true;
                break;
        }

        for (int i = 0; i < ANIMATION_FRAMES; i++) {
            final int frameIndex = i;
            handler.postDelayed(() -> {
                if (frameIndex < ANIMATION_FRAMES - 1) {
                    switch (mode) {
                        case MODE_COIN:
                            int coinFrame = random.nextInt(2);
                            views.setImageViewResource(R.id.coin_image, COIN_RESOURCES[coinFrame]);
                            break;
                        case MODE_DICE:
                            int diceFrame = random.nextInt(6);
                            views.setImageViewResource(R.id.dice_image, DICE_RESOURCES[diceFrame]);
                            break;
                        case MODE_NUMBER:
                        default:
                            String animValue = String.valueOf(min + random.nextInt(max - min + 1));
                            views.setTextViewText(R.id.widget_result, animValue);
                            break;
                    }
                } else {
                    updateWidgetWithResult(views, mode, finalResult);

                    if (mode == MODE_DICE) {
                        views.setImageViewResource(R.id.dice_image, DICE_RESOURCES[finalDiceValue - 1]);
                    } else if (mode == MODE_COIN) {
                        views.setImageViewResource(R.id.coin_image, COIN_RESOURCES[finalCoinIsHeads ? 0 : 1]);
                    }

                    saveToHistory(context, appWidgetId, finalResult);
                }

                appWidgetManager.updateAppWidget(appWidgetId, views);
            }, i * animationDelay);
        }
    }

    private static void updateWidgetWithResult(RemoteViews views, int mode, String result) {
        switch (mode) {
            case MODE_COIN:
                boolean isHeads = "Орел".equals(result);
                views.setImageViewResource(R.id.coin_image, COIN_RESOURCES[isHeads ? 0 : 1]);
                break;
            case MODE_DICE:
                try {
                    int diceValue = Integer.parseInt(result);
                    if (diceValue >= 1 && diceValue <= 6) {
                        views.setImageViewResource(R.id.dice_image, DICE_RESOURCES[diceValue - 1]);
                    }
                } catch (NumberFormatException e) {
                    views.setImageViewResource(R.id.dice_image, DICE_RESOURCES[0]);
                }
                break;
            case MODE_NUMBER:
            default:
                views.setTextViewText(R.id.widget_result, result);
                break;
        }
    }

    private static void saveToHistory(Context context, int appWidgetId, String result) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();

        int historyCount = prefs.getInt(PREF_HISTORY_COUNT + appWidgetId, 0);

        for (int i = Math.min(historyCount, MAX_HISTORY_SIZE - 1); i > 0; i--) {
            String historyItem = prefs.getString(PREF_HISTORY_PREFIX + appWidgetId + "_" + (i - 1), "");
            editor.putString(PREF_HISTORY_PREFIX + appWidgetId + "_" + i, historyItem);
        }

        editor.putString(PREF_HISTORY_PREFIX + appWidgetId + "_0", result);

        if (historyCount < MAX_HISTORY_SIZE) {
            editor.putInt(PREF_HISTORY_COUNT + appWidgetId, historyCount + 1);
        }

        editor.apply();
    }

    private static void setUpButtons(Context context, RemoteViews views, int appWidgetId) {
        Intent generateIntent = new Intent(context, RandomGeneratorWidget.class);
        generateIntent.setAction(ACTION_GENERATE);
        generateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        generateIntent.setData(Uri.parse("random://widget/generate/" + appWidgetId));
        PendingIntent generatePendingIntent = PendingIntent.getBroadcast(
                context, 0, generateIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.button_generate, generatePendingIntent);

        Intent modeIntent = new Intent(context, RandomGeneratorWidget.class);
        modeIntent.setAction(ACTION_CHANGE_MODE);
        modeIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        modeIntent.setData(Uri.parse("random://widget/mode/" + appWidgetId));
        PendingIntent modePendingIntent = PendingIntent.getBroadcast(
                context, 0, modeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.button_mode, modePendingIntent);

        Intent historyIntent = new Intent(context, RandomGeneratorWidget.class);
        historyIntent.setAction(ACTION_SHOW_HISTORY);
        historyIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        historyIntent.setData(Uri.parse("random://widget/history/" + appWidgetId));
        PendingIntent historyPendingIntent = PendingIntent.getBroadcast(
                context, 0, historyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.button_history, historyPendingIntent);

        Intent settingsIntent = new Intent(context, RandomGeneratorWidget.class);
        settingsIntent.setAction(ACTION_OPEN_SETTINGS);
        settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        settingsIntent.setData(Uri.parse("random://widget/settings/" + appWidgetId));
        PendingIntent settingsPendingIntent = PendingIntent.getBroadcast(
                context, 0, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.button_settings, settingsPendingIntent);
    }

    private static String getModeText(int mode) {
        switch (mode) {
            case MODE_COIN:
                return "Монета";
            case MODE_DICE:
                return "Кубик";
            case MODE_NUMBER:
            default:
                return "Число";
        }
    }

    private static String getDefaultResult(int mode) {
        switch (mode) {
            case MODE_COIN:
                return "?";
            case MODE_DICE:
                return "?";
            case MODE_NUMBER:
            default:
                return "?";
        }
    }

    private static void generateRandomValue(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        updateAppWidget(context, appWidgetManager, appWidgetId, true);
    }

    private static void cycleMode(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        int currentMode = prefs.getInt(PREF_MODE_KEY + appWidgetId, MODE_NUMBER);

        int newMode = (currentMode + 1) % 3;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_MODE_KEY + appWidgetId, newMode);
        editor.apply();

        updateAppWidget(context, appWidgetManager, appWidgetId, false);
    }

    public static List<String> getHistory(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        int historyCount = prefs.getInt(PREF_HISTORY_COUNT + appWidgetId, 0);
        List<String> history = new ArrayList<>();

        for (int i = 0; i < historyCount; i++) {
            String historyItem = prefs.getString(PREF_HISTORY_PREFIX + appWidgetId + "_" + i, "");
            history.add(historyItem);
        }

        return history;
    }

    public static void clearHistory(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();

        int historyCount = prefs.getInt(PREF_HISTORY_COUNT + appWidgetId, 0);

        for (int i = 0; i < historyCount; i++) {
            editor.remove(PREF_HISTORY_PREFIX + appWidgetId + "_" + i);
        }

        editor.putInt(PREF_HISTORY_COUNT + appWidgetId, 0);
        editor.apply();
    }
}