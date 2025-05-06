package com.example.randomwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


public class WidgetSettingsActivity extends Activity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private RadioGroup modeRadioGroup;
    private RadioButton radioNumber;
    private RadioButton radioCoin;
    private RadioButton radioDice;
    private EditText minValueEditText;
    private EditText maxValueEditText;
    private View rangeContainer;
    private SeekBar animationSpeedSeekBar;
    private TextView animationSpeedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_settings_activity);


        setResult(RESULT_CANCELED);

        modeRadioGroup = findViewById(R.id.mode_radio_group);
        radioNumber = findViewById(R.id.radio_number);
        radioCoin = findViewById(R.id.radio_coin);
        radioDice = findViewById(R.id.radio_dice);
        minValueEditText = findViewById(R.id.min_value);
        maxValueEditText = findViewById(R.id.max_value);
        rangeContainer = findViewById(R.id.range_container);
        animationSpeedSeekBar = findViewById(R.id.animation_speed_seekbar);
        animationSpeedText = findViewById(R.id.animation_speed_text);
        Button applyButton = findViewById(R.id.button_apply);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        loadSettings();

        modeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_number) {
                rangeContainer.setVisibility(View.VISIBLE);
            } else {
                rangeContainer.setVisibility(View.GONE);
            }
        });

        animationSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateAnimationSpeedText(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        applyButton.setOnClickListener(v -> saveSettingsAndFinish());
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(
                RandomGeneratorWidget.PREFS_NAME, MODE_PRIVATE);

        int mode = prefs.getInt("mode_" + appWidgetId, RandomGeneratorWidget.MODE_NUMBER);
        switch (mode) {
            case RandomGeneratorWidget.MODE_COIN:
                radioCoin.setChecked(true);
                rangeContainer.setVisibility(View.GONE);
                break;
            case RandomGeneratorWidget.MODE_DICE:
                radioDice.setChecked(true);
                rangeContainer.setVisibility(View.GONE);
                break;
            case RandomGeneratorWidget.MODE_NUMBER:
            default:
                radioNumber.setChecked(true);
                rangeContainer.setVisibility(View.VISIBLE);
                break;
        }

        int minValue = prefs.getInt("min_" + appWidgetId, 1);
        int maxValue = prefs.getInt("max_" + appWidgetId, 100);
        minValueEditText.setText(String.valueOf(minValue));
        maxValueEditText.setText(String.valueOf(maxValue));

        int animationSpeed = prefs.getInt("animation_speed_" + appWidgetId, 5);
        animationSpeedSeekBar.setProgress(animationSpeed);
        updateAnimationSpeedText(animationSpeed);
    }

    private void updateAnimationSpeedText(int progress) {
        String speedText;
        if (progress < 3) {
            speedText = "Повільно";
        } else if (progress < 7) {
            speedText = "Середньо";
        } else {
            speedText = "Швидко";
        }
        animationSpeedText.setText(speedText);
    }

    private void saveSettingsAndFinish() {
        int selectedMode;
        int checkedRadioButtonId = modeRadioGroup.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.radio_coin) {
            selectedMode = RandomGeneratorWidget.MODE_COIN;
        } else if (checkedRadioButtonId == R.id.radio_dice) {
            selectedMode = RandomGeneratorWidget.MODE_DICE;
        } else {
            selectedMode = RandomGeneratorWidget.MODE_NUMBER;

            try {
                int minValue = Integer.parseInt(minValueEditText.getText().toString());
                int maxValue = Integer.parseInt(maxValueEditText.getText().toString());

                if (minValue >= maxValue) {
                    Toast.makeText(this, "Мінімальне значення повинно бути менше максимального",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Введіть правильні числові значення",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        SharedPreferences.Editor prefs = getSharedPreferences(
                RandomGeneratorWidget.PREFS_NAME, MODE_PRIVATE).edit();

        prefs.putInt("mode_" + appWidgetId, selectedMode);

        if (selectedMode == RandomGeneratorWidget.MODE_NUMBER) {
            int minValue = Integer.parseInt(minValueEditText.getText().toString());
            int maxValue = Integer.parseInt(maxValueEditText.getText().toString());
            prefs.putInt("min_" + appWidgetId, minValue);
            prefs.putInt("max_" + appWidgetId, maxValue);
        }

        prefs.putInt("animation_speed_" + appWidgetId, animationSpeedSeekBar.getProgress());

        prefs.apply();

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        RandomGeneratorWidget.updateAppWidget(this, appWidgetManager, appWidgetId, false);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}