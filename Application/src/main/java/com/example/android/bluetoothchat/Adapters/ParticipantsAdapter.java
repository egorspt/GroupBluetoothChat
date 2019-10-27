package com.example.android.bluetoothchat.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.android.bluetoothchat.Constants;
import com.example.android.bluetoothchat.R;

import java.util.ArrayList;

public class ParticipantsAdapter extends ArrayAdapter {
    private Context context;
    private ArrayList<Participant> arrayList;
    public ParticipantsAdapter(@NonNull Context context, ArrayList<Participant> arrayList) {
        super(context, 0, arrayList);
        this.context = context;
        this.arrayList = arrayList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.device_name,parent,false);
        TextView textView = (TextView) view.findViewById(R.id.text_participant);
        textView.setText(arrayList.get(position).getField());
        textView.setTextColor(Color.parseColor("#C5C5C5"));
        textView.setPadding(15,5,20,5);
        textView.setTextSize(18);
        if(arrayList.get(position).isOnline() == Constants.ONLINE)
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0 , R.drawable.ic_participants_online, 0);
        else if(arrayList.get(position).isOnline() == Constants.OFFLINE)
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0 , R.drawable.ic_participants_offline, 0);
        else
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0 , 0, 0);

        return view;
    }

    public static class Participant {
        private String field;
        private int online;
        public Participant(String field, int online){
            this.field = field;
            this.online = online;
        }

        public String getField() {
            return field;
        }

        public int isOnline() {
            return online;
        }
    }
}
