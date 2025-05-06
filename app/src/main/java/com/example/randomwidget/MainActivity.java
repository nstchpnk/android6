package com.example.randomwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView infoTextView = findViewById(R.id.widget_info_text);
        Button refreshWidgetsButton = findViewById(R.id.refresh_widgets_button);

        infoTextView.setText(
                "\n\nЦе додаток з віджетом \"Генератор випадкових чисел\".\n\n" +
                        "Щоб додати віджет на головний екран:\n" +
                        "1. Натисніть і утримуйте на пустому місці головного екрану\n" +
                        "2. Виберіть \"Віджети\"\n" +
                        "3. Знайдіть \"Генератор випадкових чисел\" і перетягніть його на головний екран\n" +
                        "4. Налаштуйте параметри віджета\n\n" +
                        "Віджет має три режими:\n" +
                        "- Випадкове число: генерує число у вказаному діапазоні\n" +
                        "- Підкидання монети: випадково вибирає \"Орел\" або \"Решка\"\n" +
                        "- Кидок кубика: імітує кидок шестигранного кубика\n\n" +
                        "Ви можете переглянути історію генерацій, натиснувши кнопку історії у віджеті."
        );

        refreshWidgetsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(MainActivity.this);
                ComponentName thisWidget = new ComponentName(MainActivity.this, RandomGeneratorWidget.class);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

                for (int appWidgetId : appWidgetIds) {
                    RandomGeneratorWidget.updateAppWidget(MainActivity.this, appWidgetManager, appWidgetId, false);
                }
            }
        });
    }
}