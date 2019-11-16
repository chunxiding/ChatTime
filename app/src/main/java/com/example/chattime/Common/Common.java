package com.example.chattime.Common;

import com.example.chattime.Holder.QBUsersHolder;
import com.quickblox.users.model.QBUser;

import java.util.List;

public class Common {
    public static final String DIALOG_EXTRA = "Dialogs";

    public static final String UPDATE_DIALOG_EXTRA = "ChatDialogs";
    public static final String UPDATE_MODE = "Mode";
    public static final String UPDATE_ADD_MODE = "add";
    public static final String UPDATE_REMOVE_MODE = "remove";

    public static final int SELECT_IMAGE = 7171;

    public static String createChatDialogName(List<Integer> qbUser) {
        List<QBUser> qbUsers = QBUsersHolder.getInstance().getUsersByIds(qbUser);
        StringBuilder name = new StringBuilder();
        for (QBUser user:qbUsers) {
            name.append(user.getFullName()).append(" ");
        }
        if (name.length() > 30) {
            name = name.replace(30, name.length() - 1, "...");
        }
        return name.toString();
    }

    public static boolean isNullOrEmptyString(String content) {
        return  (content != null && !content.trim().isEmpty() ? false : true);
    }
}
