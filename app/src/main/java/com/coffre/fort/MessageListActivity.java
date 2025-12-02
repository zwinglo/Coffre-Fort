package com.coffre.fort;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageListActivity extends AppCompatActivity implements MessageAdapter.OnMessageClickListener {

    private static final int REQUEST_MESSAGE_PERMISSIONS = 2001;

    private RecyclerView messagesRecyclerView;
    private TextView emptyTextView;
    private View progressBar;
    private Button syncButton;

    private DatabaseHelper databaseHelper;
    private MessageAdapter adapter;
    private ExecutorService executorService;
    private BroadcastReceiver messagesUpdatedReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_list);
        setTitle(R.string.messages_title);

        databaseHelper = new DatabaseHelper(this);
        adapter = new MessageAdapter(this, this);
        executorService = Executors.newSingleThreadExecutor();

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        emptyTextView = findViewById(R.id.messagesEmptyTextView);
        progressBar = findViewById(R.id.messagesProgressBar);
        syncButton = findViewById(R.id.messagesSyncButton);

        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(adapter);

        syncButton.setOnClickListener(v -> synchronizeMessages());

        messagesUpdatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadMessages();
                hideProgress();
            }
        };

        requestMessagePermissionsIfNeeded();
        loadMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(MessageSyncManager.ACTION_MESSAGES_UPDATED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(messagesUpdatedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(messagesUpdatedReceiver, filter);
        }
        loadMessages();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(messagesUpdatedReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Override
    public void onMessageClick(VaultMessage message) {
        Intent intent = new Intent(this, MessageDetailActivity.class);
        intent.putExtra("message_id", message.getLocalId());
        startActivity(intent);
    }

    private void loadMessages() {
        List<VaultMessage> messages = databaseHelper.getAllMessages();
        adapter.setMessages(messages);
        boolean hasMessages = messages != null && !messages.isEmpty();
        emptyTextView.setVisibility(hasMessages ? View.GONE : View.VISIBLE);
        messagesRecyclerView.setVisibility(hasMessages ? View.VISIBLE : View.GONE);
    }

    private void synchronizeMessages() {
        if (!hasMessagePermissions()) {
            requestMessagePermissionsIfNeeded();
            return;
        }
        showProgress();
        executorService.execute(() -> {
            new MessageSyncManager(getApplicationContext()).synchronizeMessages();
            runOnUiThread(() -> {
                loadMessages();
                hideProgress();
            });
        });
    }

    private boolean hasMessagePermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMessagePermissionsIfNeeded() {
        if (!hasMessagePermissions()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_STATE},
                    REQUEST_MESSAGE_PERMISSIONS);
        }
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        syncButton.setEnabled(false);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
        syncButton.setEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MESSAGE_PERMISSIONS) {
            boolean granted = grantResults.length > 0;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                synchronizeMessages();
            } else {
                Toast.makeText(this, R.string.messages_permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }
}
