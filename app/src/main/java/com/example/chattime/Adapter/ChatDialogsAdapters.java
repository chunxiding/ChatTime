package com.example.chattime.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.example.chattime.Holder.QBUnreadMessageHolder;
import com.example.chattime.R;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.content.QBContent;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class ChatDialogsAdapters extends BaseAdapter {

    private Context context;
    private ArrayList<QBChatDialog> qbChatDialogs;

    public ChatDialogsAdapters(Context context, ArrayList<QBChatDialog> qbChatDialogs) {
        this.context = context;
        this.qbChatDialogs = qbChatDialogs;
    }


    @Override
    public int getCount() {
        return qbChatDialogs.size();
    }

    @Override
    public Object getItem(int i) {
        return qbChatDialogs.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_chat_dialogs, null);

            TextView textTitle, textMessage;
            final ImageView imageView, imageUnread;

            textMessage = (TextView) view.findViewById(R.id.list_chat_dialog_message);
            textTitle = (TextView) view.findViewById(R.id.list_chat_dialog_title);
            imageView = (ImageView) view.findViewById(R.id.image_chatDialog);
            imageUnread = (ImageView) view.findViewById(R.id.image_unread);

            textMessage.setText(qbChatDialogs.get(i).getLastMessage());
            textTitle.setText(qbChatDialogs.get(i).getName());

            ColorGenerator generator = ColorGenerator.MATERIAL;
            int randomColor = generator.getRandomColor();

            if (qbChatDialogs.get(i).getPhoto().equals("null")) {
                TextDrawable.IBuilder builder = TextDrawable.builder().beginConfig().withBorder(4).endConfig().round();
                TextDrawable drawable = builder.build(textTitle.getText().toString().substring(0,1).toUpperCase(), randomColor);
                imageView.setImageDrawable(drawable);
            } else {
                QBContent.getFile(Integer.parseInt(qbChatDialogs.get(i).getPhoto())).performAsync(new QBEntityCallback<QBFile>() {
                    @Override
                    public void onSuccess(QBFile qbFile, Bundle bundle) {
                        String fileUrl = qbFile.getPublicUrl();
                        Picasso.with(context).load(fileUrl).resize(50,50).centerCrop().into(imageView);
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Log.e("ERROR", ""+e.getMessage());
                    }
                });
            }

            TextDrawable.IBuilder unreadBuilder = TextDrawable.builder().beginConfig().withBorder(4).endConfig().round();
            int unreadCount = QBUnreadMessageHolder.getInstance().getBundle().getInt(qbChatDialogs.get(i).getDialogId());
            if (unreadCount > 0) {
                TextDrawable unreadDrawable = unreadBuilder.build(""+unreadCount, Color.RED);
                imageUnread.setImageDrawable(unreadDrawable);
            }

        }
        return view;
    }
}
