package com.example.chattime.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.quickblox.users.model.QBUser;

import java.util.ArrayList;

public class ListUserAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<QBUser> qbUsers;

    public ListUserAdapter(Context context, ArrayList<QBUser> qbUsers) {
        this.context = context;
        this.qbUsers = qbUsers;
    }

    @Override
    public int getCount() {
        return qbUsers.size();
    }

    @Override
    public Object getItem(int i) {
        return qbUsers.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(android.R.layout.simple_list_item_multiple_choice, null);
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(qbUsers.get(i).getLogin());
        }
        return view;
    }
}
