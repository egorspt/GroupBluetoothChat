package com.example.android.bluetoothchat.Database;

import android.view.View;

import com.example.android.bluetoothchat.ChatKit.PersonDialog;
import com.example.android.bluetoothchat.R;
import com.stfalcon.chatkit.dialogs.DialogsListAdapter;

public class DialogViewHolder extends DialogsListAdapter.DialogViewHolder<PersonDialog> {
    private View onlineIndicator;

    public DialogViewHolder(View itemView) {
        super(itemView);
        onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
    }

    @Override
    public void onBind(PersonDialog dialog) {
        super.onBind(dialog);

        if (dialog.getUsers().size() != 0 ) {
            boolean isOnline = dialog.getUsers().get(0).isOnline();
            if (dialog.getAdmin() != null || !isOnline) {
                onlineIndicator.setVisibility(View.GONE);
            } else {
                onlineIndicator.setVisibility(View.VISIBLE);
            }
        }
    }
}
