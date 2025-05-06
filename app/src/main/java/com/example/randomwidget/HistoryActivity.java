package com.example.randomwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;


public class HistoryActivity extends AppCompatActivity {

    private ListView historyListView;
    private TextView emptyTextView;
    private Button clearHistoryButton;
    private int currentAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Історія генерації");
        }

        historyListView = findViewById(R.id.history_list);
        emptyTextView = findViewById(R.id.empty_text);
        clearHistoryButton = findViewById(R.id.clear_history_button);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, RandomGeneratorWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        if (appWidgetIds != null && appWidgetIds.length > 0) {
            currentAppWidgetId = appWidgetIds[0];
            loadHistoryData(currentAppWidgetId);

            clearHistoryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showClearHistoryConfirmation();
                }
            });
        } else {
            emptyTextView.setText("Немає активних віджетів");
            historyListView.setEmptyView(emptyTextView);
            clearHistoryButton.setEnabled(false);
        }
    }

    private void loadHistoryData(int appWidgetId) {
        List<String> historyItems = RandomGeneratorWidget.getHistory(this, appWidgetId);

        if (historyItems.isEmpty()) {
            emptyTextView.setText("Історія порожня");
            historyListView.setEmptyView(emptyTextView);
            clearHistoryButton.setEnabled(false);
        } else {
            adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_list_item_1, historyItems);
            historyListView.setAdapter(adapter);
            clearHistoryButton.setEnabled(true);
            historyListView.setEmptyView(emptyTextView);
        }
    }

    private void showClearHistoryConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Підтвердження");
        builder.setMessage("Ви впевнені, що хочете очистити всю історію?");
        builder.setPositiveButton("Так", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clearHistory();
            }
        });
        builder.setNegativeButton("Ні", null);
        builder.show();
    }

    private void clearHistory() {
        if (currentAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            RandomGeneratorWidget.clearHistory(this, currentAppWidgetId);

            List<String> emptyList = RandomGeneratorWidget.getHistory(this, currentAppWidgetId);
            adapter.clear();
            adapter.addAll(emptyList);
            adapter.notifyDataSetChanged();

            emptyTextView.setText("Історія порожня");
            historyListView.setEmptyView(emptyTextView);
            clearHistoryButton.setEnabled(false);

            Toast.makeText(this, "Історію очищено", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}