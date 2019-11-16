package com.example.chattime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.example.chattime.Adapter.ChatMessageAdapter;
import com.example.chattime.Common.Common;
import com.example.chattime.Holder.QBChatDialogHolder;
import com.example.chattime.Holder.QBChatMessageHolder;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBIncomingMessagesManager;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.listeners.QBChatDialogParticipantListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.chat.model.QBPresence;
import com.quickblox.chat.request.QBDialogRequestBuilder;
import com.quickblox.chat.request.QBMessageGetBuilder;
import com.quickblox.chat.request.QBMessageUpdateBuilder;
import com.quickblox.content.QBContent;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBRequestUpdateBuilder;
import com.squareup.picasso.Picasso;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.DiscussionHistory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

public class ChatMessageActivity extends AppCompatActivity implements  QBChatDialogMessageListener{
    QBChatDialog qbChatDialog;
    ListView listChatMessages;
    ImageButton submitButton;
    EditText editContent;

    ChatMessageAdapter adapter;

    ImageView image_online_count;
    ImageView dialog_avatar;
    TextView text_online_count;

    int contextMenuIndexClicked = -1;
    boolean isEditMode = false;
    QBChatMessage editMessage;

    Toolbar toolbar;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (qbChatDialog.getType() == QBDialogType.PUBLIC_GROUP || qbChatDialog.getType() == QBDialogType.GROUP) {
            getMenuInflater().inflate(R.menu.chat_message_group_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.chat_group_add_user:
                addUser();
                break;
            case R.id.chat_group_remove_user:
                removeUser();
                break;
            case R.id.chat_group_edit_name:
                editName();
                break;
                default:
                    break;
        }
        return true;
    }

    private void removeUser() {
        Intent intent = new Intent(this, ListUserActivity.class);
        intent.putExtra(Common.UPDATE_DIALOG_EXTRA, qbChatDialog);
        intent.putExtra(Common.UPDATE_MODE, Common.UPDATE_REMOVE_MODE);
        startActivity(intent);
    }


    private void addUser() {
        // Log.e("A", "here");
        Intent intent = new Intent(this, ListUserActivity.class);
        intent.putExtra(Common.UPDATE_DIALOG_EXTRA, qbChatDialog);
        intent.putExtra(Common.UPDATE_MODE, Common.UPDATE_ADD_MODE);
        startActivity(intent);
    }

    private void editName() {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view =inflater.inflate(R.layout.dialog_edit_group_name_layout, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(view);
        final EditText newName = (EditText) view.findViewById(R.id.edit_new_group_name);

        alertDialogBuilder.setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                qbChatDialog.setName(newName.getText().toString());

                QBDialogRequestBuilder requestBuilder = new QBDialogRequestBuilder();
                QBRestChatService.updateGroupChatDialog(qbChatDialog, requestBuilder).performAsync(new QBEntityCallback<QBChatDialog>() {
                    @Override
                    public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                        Toast.makeText(ChatMessageActivity.this, "Group name updated", Toast.LENGTH_SHORT).show();
                        toolbar.setTitle(qbChatDialog.getName());
                    }

                    @Override
                    public void onError(QBResponseException e) {

                    }
                });
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.chat_message_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        contextMenuIndexClicked = info.position;

        switch (item.getItemId()) {
            case R.id.chat_message_update_message:
                updateMessage();
                break;
            case R.id.chat_message_delete_message:
                deleteMessage();
                break;
        }
        return true;
    }

    private void deleteMessage() {
        final ProgressDialog deleteDialog = new ProgressDialog(ChatMessageActivity.this);
        deleteDialog.setMessage("Please Wait...");
        deleteDialog.show();

        editMessage = QBChatMessageHolder.getInstance().getChatMessageByDialogId(qbChatDialog.getDialogId()).get(contextMenuIndexClicked);
        QBRestChatService.deleteMessage(editMessage.getId(), true).performAsync(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                retrieveMessage();
                deleteDialog.dismiss();
                Toast.makeText(getBaseContext(), "You have recalled a message", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    private void updateMessage() {
        editMessage = QBChatMessageHolder.getInstance().getChatMessageByDialogId(qbChatDialog.getDialogId()).get(contextMenuIndexClicked);
        editContent.setText(editMessage.getBody().toString());
        isEditMode = true;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        qbChatDialog.removeMessageListrener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        qbChatDialog.removeMessageListrener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_message);
        initView();
        initChatDialogs();
        retrieveMessage();

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!editContent.getText().toString().isEmpty()) {
                    if (!isEditMode) {
                        QBChatMessage chatMessage = new QBChatMessage();
                        chatMessage.setBody(editContent.getText().toString());
                        chatMessage.setSenderId(QBChatService.getInstance().getUser().getId());
                        chatMessage.setSaveToHistory(true);

                        try {
                            qbChatDialog.sendMessage(chatMessage);
                        } catch (SmackException.NotConnectedException e) {
                            e.printStackTrace();
                        }


                        QBChatMessageHolder.getInstance().putMessage(qbChatDialog.getDialogId(), chatMessage);
                        ArrayList<QBChatMessage> messages = QBChatMessageHolder.getInstance().getChatMessageByDialogId(qbChatDialog.getDialogId());
//                for (QBChatMessage msg:messages) {
//                    Log.e("A", msg.getBody());
//                }
                        adapter = new ChatMessageAdapter(getBaseContext(), messages);
                        listChatMessages.setAdapter(adapter);
                        adapter.notifyDataSetChanged();


                        editContent.setText("");
                        editContent.setFocusable(true);
                    } else {
                        final ProgressDialog updateDialog = new ProgressDialog(ChatMessageActivity.this);
                        updateDialog.setMessage("Please Wait...");
                        updateDialog.show();

                        QBMessageUpdateBuilder messageUpdateBuilder = new QBMessageUpdateBuilder();
                        messageUpdateBuilder.updateText(editContent.getText().toString()).markDelivered().markRead();

                        QBRestChatService.updateMessage(editMessage.getId(), qbChatDialog.getDialogId(), messageUpdateBuilder).performAsync(new QBEntityCallback<Void>() {
                            @Override
                            public void onSuccess(Void aVoid, Bundle bundle) {
                                retrieveMessage();
                                isEditMode = false;
                                updateDialog.dismiss();

                                editContent.setText("");
                                editContent.setFocusable(true);
                            }

                            @Override
                            public void onError(QBResponseException e) {
                                Toast.makeText(getBaseContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });
    }

    private void retrieveMessage() {
        // Log.e("A", "here1");
        QBMessageGetBuilder messageGetBuilder = new QBMessageGetBuilder();
        messageGetBuilder.setLimit(500);

        if (qbChatDialog != null) {
            QBRestChatService.getDialogMessages(qbChatDialog, messageGetBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatMessage>>() {
                @Override
                public void onSuccess(ArrayList<QBChatMessage> qbChatMessages, Bundle bundle) {
                    Log.e("B", String.valueOf(qbChatMessages.size()));
                    // ArrayList<QBChatMessage> newChatMessages = new ArrayList<>();
                    // QBChatMessageHolder.getInstance().putMessages(qbChatDialog.getDialogId(), newChatMessages);
                    QBChatMessageHolder.getInstance().putMessages(qbChatDialog.getDialogId(), qbChatMessages);
                    adapter = new ChatMessageAdapter(getBaseContext(), qbChatMessages);
                    listChatMessages.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onError(QBResponseException e) {

                }
            });
        }
    }

    private void initChatDialogs() {
        // Log.e("A", "here2");
        qbChatDialog = (QBChatDialog) getIntent().getSerializableExtra(Common.DIALOG_EXTRA);
        qbChatDialog.initForChat(QBChatService.getInstance());

        if (qbChatDialog != null && !qbChatDialog.getPhoto().equals("null")) {
            QBContent.getFile(Integer.parseInt(qbChatDialog.getPhoto())).performAsync(new QBEntityCallback<QBFile>() {
                @Override
                public void onSuccess(QBFile qbFile, Bundle bundle) {
                    String fileUrl = qbFile.getPublicUrl();
                    Picasso.with(getBaseContext()).load(fileUrl).resize(50, 50).centerCrop().into(dialog_avatar);
                }

                @Override
                public void onError(QBResponseException e) {
                    Log.e("ERROR", "" + e.getMessage());
                }
            });
        }

        dialog_avatar.setVisibility((qbChatDialog.getType() == QBDialogType.PUBLIC_GROUP || qbChatDialog.getType() == QBDialogType.GROUP) ? View.VISIBLE : View.INVISIBLE);

        QBIncomingMessagesManager incomingMessagesManager = QBChatService.getInstance().getIncomingMessagesManager();
        Collection<QBChatDialogMessageListener> existingListener = incomingMessagesManager.getDialogMessageListeners();
        if (existingListener.size() == 0) {
            incomingMessagesManager.addDialogMessageListener(new QBChatDialogMessageListener() {
                @Override
                public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {
                    QBChatMessageHolder.getInstance().putMessage(qbChatMessage.getDialogId(), qbChatMessage);
                    ArrayList<QBChatMessage> messages = QBChatMessageHolder.getInstance().getChatMessageByDialogId(qbChatMessage.getDialogId());
                }

                @Override
                public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {

                }
            });
        }

        if (qbChatDialog.getType() == QBDialogType.PUBLIC_GROUP || qbChatDialog.getType() == QBDialogType.GROUP) {
            DiscussionHistory discussionHistory = new DiscussionHistory();
            discussionHistory.setMaxStanzas(0);
            qbChatDialog.join(discussionHistory, new QBEntityCallback() {
                @Override
                public void onSuccess(Object o, Bundle bundle) {

                }

                @Override
                public void onError(QBResponseException e) {
                    Log.e("ERROR", e.getMessage());
                }
            });

            QBChatDialogParticipantListener participantListener = new QBChatDialogParticipantListener() {
                @Override
                public void processPresence(String s, QBPresence qbPresence) {
                    if (s == qbChatDialog.getDialogId()) {
                        QBRestChatService.getChatDialogById(s).performAsync(new QBEntityCallback<QBChatDialog>() {
                            @Override
                            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                try {
                                    // Log.e("A", "online");
                                    Collection<Integer> onlineList  =  qbChatDialog.getOnlineUsers();
                                    TextDrawable.IBuilder builder = TextDrawable.builder().beginConfig().withBorder(4).endConfig().round();
                                    TextDrawable online = builder.build("", Color.RED);
                                    image_online_count.setImageDrawable(online);
                                    text_online_count.setText(String.format("%d/%d online", onlineList.size(), qbChatDialog.getOccupants().size()));
                                } catch (XMPPException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onError(QBResponseException e) {

                            }
                        });
                    }
                }
            };

            qbChatDialog.addParticipantListener(participantListener);

        }

        qbChatDialog.addMessageListener(this);

        toolbar.setTitle(qbChatDialog.getName());
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == Common.SELECT_IMAGE) {
                Uri selectImageUrl = data.getData();
                final ProgressDialog mDialog = new ProgressDialog(ChatMessageActivity.this);
                mDialog.setMessage("Please wait...");
                mDialog.setCancelable(false);
                mDialog.show();

                try {
                    InputStream in = (InputStream) getContentResolver().openInputStream(selectImageUrl);
                    final Bitmap bitmap = BitmapFactory.decodeStream(in);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                    File file = new File(Environment.getExternalStorageDirectory() + "/image.png");
                    FileOutputStream fileOut = new FileOutputStream(file);
                    fileOut.write(bos.toByteArray());
                    fileOut.flush();
                    fileOut.close();

                    int imageSizeKb = (int) file.length() / 1024;
                    if (imageSizeKb >= (1024 * 100)) {
                        Toast.makeText(this, "Image too large", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    QBContent.uploadFileTask(file, true, null).performAsync(new QBEntityCallback<QBFile>() {
                        @Override
                        public void onSuccess(QBFile qbFile, Bundle bundle) {
                            qbChatDialog.setPhoto(qbFile.getId().toString());

                            QBRequestUpdateBuilder requestUpdateBuilder = new QBRequestUpdateBuilder();
                            QBRestChatService.updateGroupChatDialog(qbChatDialog, requestUpdateBuilder).performAsync(new QBEntityCallback<QBChatDialog>() {
                                @Override
                                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                    mDialog.dismiss();
                                    dialog_avatar.setImageBitmap(bitmap);
                                }

                                @Override
                                public void onError(QBResponseException e) {
                                    Toast.makeText(ChatMessageActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(QBResponseException e) {

                        }
                    });
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }
    }

    private void initView() {
        listChatMessages = (ListView) findViewById(R.id.list_messages);
        submitButton = (ImageButton) findViewById(R.id.send_button);
        editContent = (EditText) findViewById(R.id.edit_content);

        registerForContextMenu(listChatMessages);

        toolbar = (Toolbar) findViewById(R.id.chat_message_toolbar);

        image_online_count = (ImageView) findViewById(R.id.image_online_count);
        text_online_count = (TextView) findViewById(R.id.text_online_count);
        dialog_avatar = (ImageView) findViewById(R.id.dialog_avatar);

        dialog_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Log.e("A", "herepic2");
                Intent selectImage = new Intent();
                selectImage.setType("image/*");
                selectImage.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(selectImage, "Select Image"), Common.SELECT_IMAGE);
            }
        });





    }

    @Override
    public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {
        Log.e("A", "here3");
        QBChatMessageHolder.getInstance().putMessage(qbChatMessage.getDialogId(), qbChatMessage);
        ArrayList<QBChatMessage> messages = QBChatMessageHolder.getInstance().getChatMessageByDialogId(qbChatMessage.getDialogId());
        QBMessageUpdateBuilder messageUpdateBuilder = new QBMessageUpdateBuilder();
//        for (QBChatMessage msg:messages) {
//            messageUpdateBuilder.markRead();
//        }
        // Log.e("A", String.valueOf(messages.size()));
        adapter = new ChatMessageAdapter(getBaseContext(), messages);
        listChatMessages.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        retrieveMessage();
    }

    @Override
    public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {
        Log.e("ERROR", e.getMessage());
    }
}
