package com.example.chattime.Holder;

import com.quickblox.chat.model.QBChatDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QBChatDialogHolder {
    private static  QBChatDialogHolder instance;
    private HashMap<String, QBChatDialog> qbChatDialogHashMap;

    public static synchronized QBChatDialogHolder getInstance() {
        QBChatDialogHolder qbChatDialogHolder;
        synchronized (QBChatDialogHolder.class) {
            if (instance == null) {
                instance = new QBChatDialogHolder();
            }
            qbChatDialogHolder = instance;
            return qbChatDialogHolder;
        }
    }

    public QBChatDialogHolder() {
        this.qbChatDialogHashMap = new HashMap<>();
    }

    public void putDialogs(List<QBChatDialog> dialogs) {
        for (QBChatDialog qbChatDialog:dialogs) {
            putDialog(qbChatDialog);
        }
    }

    public void putDialog(QBChatDialog qbChatDialog) {
        this.qbChatDialogHashMap.put(qbChatDialog.getDialogId(), qbChatDialog);
    }

    public QBChatDialog getChatDialogById(String dialogId) {
        return (QBChatDialog) qbChatDialogHashMap.get(dialogId);
    }

    public List<QBChatDialog> getChatDialogsByIds(List<String> dialogIds) {
        List<QBChatDialog> chatDialogs = new ArrayList<>();
        for (String id: dialogIds) {
            QBChatDialog chatDialog = getChatDialogById(id);
            if (chatDialog != null) {
                chatDialogs.add(chatDialog);

            }
        }
        return  chatDialogs;
    }

    public ArrayList<QBChatDialog> getAllChatDialogs() {
        ArrayList<QBChatDialog> chatDialogs = new ArrayList<>();
        for (String id: qbChatDialogHashMap.keySet()) {
            chatDialogs.add(qbChatDialogHashMap.get(id));
        }
        return  chatDialogs;
    }

    public void removeDialog(String id) {
        qbChatDialogHashMap.remove(id);
    }

}
