package com.example.chattime.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.chattime.Holder.QBChatMessageHolder;
import com.example.chattime.Holder.QBUsersHolder;
import com.example.chattime.R;
import com.github.library.bubbleview.BubbleTextView;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.model.QBChatMessage;

import java.util.ArrayList;

public class ChatMessageAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<QBChatMessage> qbChatMessages;

    public ChatMessageAdapter(Context context, ArrayList<QBChatMessage> qbChatMessages) {
        this.context = context;
        this.qbChatMessages = qbChatMessages;
    }

    @Override
    public int getCount() {
        return qbChatMessages.size();
    }

    @Override
    public Object getItem(int i) {
        return qbChatMessages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        // Log.e("B", Boolean.valueOf(view == null) + String.valueOf(i) + qbChatMessages.get(i).getBody());
        View newView = view;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (qbChatMessages.get(i).getSenderId().equals(QBChatService.getInstance().getUser().getId())) {
                newView = inflater.inflate(R.layout.list_send_messages, null);
                BubbleTextView bubbleTextView = (BubbleTextView) newView.findViewById(R.id.message_content);
                bubbleTextView.setText(qbChatMessages.get(i).getBody());
            } else {
                newView = inflater.inflate(R.layout.list_recv_message, null);
                BubbleTextView bubbleTextView = (BubbleTextView) newView.findViewById(R.id.message_content);
                bubbleTextView.setText(qbChatMessages.get(i).getBody());
                TextView textName = (TextView) newView.findViewById(R.id.message_user);
                textName.setText(QBUsersHolder.getInstance().getUserById(qbChatMessages.get(i).getSenderId()).getFullName());
            }
        }
        return newView;
    }
}
