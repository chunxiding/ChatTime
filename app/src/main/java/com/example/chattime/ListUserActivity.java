package com.example.chattime;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.chattime.Adapter.ListUserAdapter;
import com.example.chattime.Common.Common;
import com.example.chattime.Holder.QBUsersHolder;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.QBSystemMessagesManager;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.chat.request.QBDialogRequestBuilder;
import com.quickblox.chat.utils.DialogUtils;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import org.jivesoftware.smack.SmackException;

import java.util.ArrayList;
import java.util.List;

public class ListUserActivity extends AppCompatActivity {
     ListView listUsers;
     Button buttonCreateChat;

     String mode;
     QBChatDialog qbChatDialog;
     List<QBUser> userAdd = new ArrayList<>();
     
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_user);

        mode = getIntent().getStringExtra(Common.UPDATE_MODE);
        // Log.e("A", mode);
        qbChatDialog = (QBChatDialog) getIntent().getSerializableExtra(Common.UPDATE_DIALOG_EXTRA);

        listUsers = (ListView) findViewById(R.id.list_users);
        listUsers.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        buttonCreateChat = (Button) findViewById(R.id.button_create_chat);
        buttonCreateChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mode == null) {
                    if (listUsers.getCheckedItemPositions().size() == 1) {
                        createPrivateChat(listUsers.getCheckedItemPositions());
                    } else if (listUsers.getCheckedItemPositions().size() > 1) {
                        createGroupChat(listUsers.getCheckedItemPositions());
                    } else {
                        Toast.makeText(ListUserActivity.this, "Please select one or more friends to start a chat!", Toast.LENGTH_SHORT).show();
                    }
                } else if (mode.equals(Common.UPDATE_ADD_MODE) && qbChatDialog != null) {
                    if (userAdd.size() > 0) {
                        QBDialogRequestBuilder requestBuilder = new QBDialogRequestBuilder();
                        int countChoice = listUsers.getCount();
                        SparseBooleanArray checkItemPositions = listUsers.getCheckedItemPositions();
                        for (int i = 0; i<countChoice; i++) {
                            if (checkItemPositions.get(i)) {
                                QBUser user = (QBUser) listUsers.getItemAtPosition(i);
                                requestBuilder.addUsers(user);
                            }
                        }

                        QBRestChatService.updateGroupChatDialog(qbChatDialog, requestBuilder).performAsync(new QBEntityCallback<QBChatDialog>() {
                            @Override
                            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                Toast.makeText(getBaseContext(), "Add user successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onError(QBResponseException e) {
                                Toast.makeText(ListUserActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                } else if (mode.equals(Common.UPDATE_REMOVE_MODE) && qbChatDialog != null) {
                    if (userAdd.size() > 0) {
                        QBDialogRequestBuilder requestBuilder = new QBDialogRequestBuilder();
                        int countChoice = listUsers.getCount();
                        SparseBooleanArray checkItemPositions = listUsers.getCheckedItemPositions();
                        for (int i = 0; i<countChoice; i++) {
                            if (checkItemPositions.get(i)) {
                                QBUser user = (QBUser) listUsers.getItemAtPosition(i);
                                requestBuilder.removeUsers(user);
                            }
                        }

                        QBRestChatService.updateGroupChatDialog(qbChatDialog, requestBuilder).performAsync(new QBEntityCallback<QBChatDialog>() {
                            @Override
                            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                Toast.makeText(getBaseContext(), "Remove user successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onError(QBResponseException e) {
                                Toast.makeText(ListUserActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

            }
        });

        if (mode == null && qbChatDialog == null) {
            retrieveAllUsers();
        } else if (mode.equals(Common.UPDATE_ADD_MODE)) {
            loadListAvailableUsers();
        } else if (mode.equals(Common.UPDATE_REMOVE_MODE)){
            loadListGroupMembers();
        }
    }

    private void loadListGroupMembers() {
        buttonCreateChat.setText("Remove User");
        QBRestChatService.getChatDialogById(qbChatDialog.getDialogId()).performAsync(new QBEntityCallback<QBChatDialog>() {
            @Override
            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                List<Integer> occupentIds = qbChatDialog.getOccupants();
                List<QBUser> listGroupMember = QBUsersHolder.getInstance().getUsersByIds(occupentIds);
                ArrayList<QBUser> users = new ArrayList<>();
                users.addAll(listGroupMember);
                ListUserAdapter adapter = new ListUserAdapter(getBaseContext(), users);
                listUsers.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                userAdd = users;

            }

            @Override
            public void onError(QBResponseException e) {
                Toast.makeText(ListUserActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadListAvailableUsers() {
        buttonCreateChat.setText("Add User");

        QBRestChatService.getChatDialogById(qbChatDialog.getDialogId()).performAsync(new QBEntityCallback<QBChatDialog>() {
            @Override
            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                ArrayList<QBUser> allUsers = QBUsersHolder.getInstance().getAllUsers();
                List<Integer> occpantIds = qbChatDialog.getOccupants();
                List<QBUser> groupMembers = QBUsersHolder.getInstance().getUsersByIds(occpantIds);

                for (QBUser user:groupMembers) {
                    allUsers.remove(user);
                }

                if(allUsers.size() > 0) {
                    ListUserAdapter adapter = new ListUserAdapter(getBaseContext(), allUsers);
                    listUsers.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    userAdd = allUsers;
                }
            }

            @Override
            public void onError(QBResponseException e) {
                Toast.makeText(ListUserActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void createPrivateChat(SparseBooleanArray checkedItemPositions) {
        final ProgressDialog mDialog = new ProgressDialog(ListUserActivity.this);
        mDialog.setMessage("Please wait...");
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();

        int count = listUsers.getCount();
        for (int i = 0; i < count; i++) {
            if (checkedItemPositions.get(i)) {
                final QBUser user = (QBUser) listUsers.getItemAtPosition(i);
                QBChatDialog dialog = DialogUtils.buildPrivateDialog(user.getId());
                QBRestChatService.createChatDialog(dialog).performAsync(new QBEntityCallback<QBChatDialog>() {
                    @Override
                    public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                        mDialog.dismiss();
                        Toast.makeText(getBaseContext(), "Private dialog created", Toast.LENGTH_SHORT).show();

                        QBSystemMessagesManager qbSystemMessagesManager = QBChatService.getInstance().getSystemMessagesManager();
                        QBChatMessage qbChatMessage = new QBChatMessage();
                        qbChatMessage.setRecipientId(user.getId());
                        qbChatMessage.setBody(qbChatDialog.getDialogId());
                        try {
                            qbSystemMessagesManager.sendSystemMessage(qbChatMessage);
                        } catch (SmackException.NotConnectedException e) {
                            e.printStackTrace();
                        }
                        finish();
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Log.e("ERROR", e.getMessage());
                    }
                });
            }
        }


    }

    private void createGroupChat(SparseBooleanArray checkedItemPositions) {
        final ProgressDialog mDialog = new ProgressDialog(ListUserActivity.this);
        mDialog.setMessage("Please wait...");
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();

        int count = listUsers.getCount();
        ArrayList<Integer> occupantIdsList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            if (checkedItemPositions.get(i)) {
                QBUser user = (QBUser) listUsers.getItemAtPosition(i);
                occupantIdsList.add(user.getId());
            }
        }

        QBChatDialog dialog = new QBChatDialog();
        dialog.setName(Common.createChatDialogName(occupantIdsList));
        dialog.setType(QBDialogType.GROUP);
        dialog.setOccupantsIds(occupantIdsList);

        QBRestChatService.createChatDialog(dialog).performAsync(new QBEntityCallback<QBChatDialog>() {
            @Override
            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                mDialog.dismiss();
                Toast.makeText(getBaseContext(), "Group dialog created", Toast.LENGTH_SHORT).show();
                QBSystemMessagesManager qbSystemMessagesManager = QBChatService.getInstance().getSystemMessagesManager();
                QBChatMessage qbChatMessage = new QBChatMessage();
                for (int i = 0; i<qbChatDialog.getOccupants().size();i++) {
                    qbChatMessage.setRecipientId(qbChatDialog.getOccupants().get(i));
                    try {
                        qbSystemMessagesManager.sendSystemMessage(qbChatMessage);
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                    }
                }
                finish();
            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("ERROR", e.getMessage());
            }
        });
    }

    private void retrieveAllUsers() {
        QBUsers.getUsers(null).performAsync(new QBEntityCallback<ArrayList<QBUser>>() {
            @Override
            public void onSuccess(ArrayList<QBUser> qbUsers, Bundle bundle) {
                QBUsersHolder.getInstance().putUsers(qbUsers);

                ArrayList<QBUser> qbUserWithoutCurrent = new ArrayList<QBUser>();
                for (QBUser user: qbUsers) {
                    if (!user.getLogin().equals(QBChatService.getInstance().getUser().getLogin())) {
                        qbUserWithoutCurrent.add(user);
                    }
                }
                ListUserAdapter adapter = new ListUserAdapter(getBaseContext(), qbUserWithoutCurrent);
                listUsers.setAdapter(adapter);
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("ERROR", e.getMessage());
            }
        });
    }
}
