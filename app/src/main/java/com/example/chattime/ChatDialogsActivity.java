package com.example.chattime;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.chattime.Adapter.ChatDialogsAdapters;
import com.example.chattime.Common.Common;
import com.example.chattime.Holder.QBChatDialogHolder;
import com.example.chattime.Holder.QBUnreadMessageHolder;
import com.example.chattime.Holder.QBUsersHolder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.quickblox.auth.QBAuth;
import com.quickblox.auth.session.BaseService;
import com.quickblox.auth.session.QBSession;
import com.quickblox.chat.QBChat;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBIncomingMessagesManager;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.QBSystemMessagesManager;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.listeners.QBSystemMessageListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.BaseServiceException;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ChatDialogsActivity extends AppCompatActivity implements QBSystemMessageListener, QBChatDialogMessageListener {

    FloatingActionButton floatingActionButton;
    ListView listChatDialogs;


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.chat_dialog_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        
        switch (item.getItemId()) {
            case R.id.dialog_context_delete_dialog:
                deleteDialog(info.position);
                break;
        }

        return true;
    }

    private void deleteDialog(int position) {
        final QBChatDialog chatDialog = (QBChatDialog) listChatDialogs.getAdapter().getItem(position);
        QBRestChatService.deleteDialog(chatDialog.getDialogId(), false).performAsync(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                QBChatDialogHolder.getInstance().removeDialog(chatDialog.getDialogId());
                ChatDialogsAdapters adapters = new ChatDialogsAdapters(getBaseContext(), QBChatDialogHolder.getInstance().getAllChatDialogs());
                listChatDialogs.setAdapter(adapters);
                adapters.notifyDataSetChanged();
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChatDialogs();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_dialog_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.chat_dialog_menu_user: {
                showUserProfile();
                break;
            }
            default:
                break;
                    
        }
        return true;
    }

    private void showUserProfile() {
        Intent intent = new Intent(ChatDialogsActivity.this, UserProfileActivity.class);
        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_dialogs);

        Toolbar toolbar = (Toolbar) findViewById(R.id.chat_dialog_toolbar);
        toolbar.setTitle("ChatTime");
        setSupportActionBar(toolbar);

        createSessionForChat();
        listChatDialogs = (ListView) findViewById(R.id.listChatDialogs);
        listChatDialogs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                QBChatDialog qbChatDialog = (QBChatDialog) listChatDialogs.getAdapter().getItem(i);
                Intent intent = new Intent(ChatDialogsActivity.this, ChatMessageActivity.class);
                intent.putExtra(Common.DIALOG_EXTRA, qbChatDialog);
                startActivity(intent);
            }
        });

        registerForContextMenu(listChatDialogs);
        loadChatDialogs();

        // go to add new user list user activity
        floatingActionButton = (FloatingActionButton) findViewById(R.id.chatDialog_addUser);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChatDialogsActivity.this, ListUserActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadChatDialogs() {
        QBRequestGetBuilder requestGetBuilder = new QBRequestGetBuilder();
        requestGetBuilder.setLimit(100);

        QBRestChatService.getChatDialogs(null, requestGetBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatDialog>>() {
            @Override
            public void onSuccess(final ArrayList<QBChatDialog> qbChatDialogs, Bundle bundle) {
                QBChatDialogHolder.getInstance().putDialogs(qbChatDialogs);

                Set<String> setIds = new HashSet<>();
                for (QBChatDialog chatDialog:qbChatDialogs) {
                    ((HashSet) setIds).add(chatDialog.getDialogId());
                }

                QBRestChatService.getTotalUnreadMessagesCount(setIds, QBUnreadMessageHolder.getInstance().getBundle()).performAsync(new QBEntityCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer integer, Bundle bundle) {
                        QBUnreadMessageHolder.getInstance().setBundle(bundle);
                        ChatDialogsAdapters adapters = new ChatDialogsAdapters(getBaseContext(), QBChatDialogHolder.getInstance().getAllChatDialogs());
                        listChatDialogs.setAdapter(adapters);
                        adapters.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(QBResponseException e) {

                    }
                });
            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("ERROR", e.getMessage());
            }
        });
    }

    private void createSessionForChat() {
        // show loading message
        final ProgressDialog mDialog = new ProgressDialog(ChatDialogsActivity.this);
        mDialog.setMessage("Please wait...");
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();

        // get user and password from intent extra
        String user, password;
        user = getIntent().getStringExtra("user");
        password = getIntent().getStringExtra("password");

        QBUsers.getUsers(null).performAsync(new QBEntityCallback<ArrayList<QBUser>>() {
            @Override
            public void onSuccess(ArrayList<QBUser> qbUsers, Bundle bundle) {
                QBUsersHolder.getInstance().putUsers(qbUsers);
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });

        final QBUser qbUser = new QBUser(user, password);
        QBAuth.createSession(qbUser).performAsync(new QBEntityCallback<QBSession>() {
            @Override
            public void onSuccess(QBSession qbSession, Bundle bundle) {
                qbUser.setId(qbSession.getUserId());
                try {
                    qbUser.setPassword(BaseService.getBaseService().getToken());
                } catch (BaseServiceException e) {
                    e.printStackTrace();
                }

                QBChatService.getInstance().login(qbUser, new QBEntityCallback() {
                    @Override
                    public void onSuccess(Object o, Bundle bundle) {

                        mDialog.dismiss();
                        QBSystemMessagesManager qbSystemMessagesManager = QBChatService.getInstance().getSystemMessagesManager();
                        qbSystemMessagesManager.addSystemMessageListener(ChatDialogsActivity.this);

                        QBIncomingMessagesManager incomingMessagesManager = QBChatService.getInstance().getIncomingMessagesManager();
                        incomingMessagesManager.addDialogMessageListener(ChatDialogsActivity.this);
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Log.e("ERROR", ""+e.getMessage());
                    }
                });
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    @Override
    public void processMessage(QBChatMessage qbChatMessage) {
        QBRestChatService.getChatDialogById(qbChatMessage.getBody()).performAsync(new QBEntityCallback<QBChatDialog>() {
            @Override
            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                QBChatDialogHolder.getInstance().putDialog(qbChatDialog);
                ArrayList<QBChatDialog> adapterSource = QBChatDialogHolder.getInstance().getAllChatDialogs();
                ChatDialogsAdapters adapters = new ChatDialogsAdapters(getBaseContext(), adapterSource);
                listChatDialogs.setAdapter(adapters);
                adapters.notifyDataSetChanged();
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    @Override
    public void processError(QBChatException e, QBChatMessage qbChatMessage) {
        Log.e("ERROR", e.getMessage());
    }

    @Override
    public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {
        loadChatDialogs();
    }

    @Override
    public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {

    }
}
