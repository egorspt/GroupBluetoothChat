package com.example.android.bluetoothchat.Database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.example.android.bluetoothchat.ChatKit.MessageChat;
import com.example.android.bluetoothchat.ChatKit.PersonDialog;

@Database(entities = {PersonDialog.class, MessageChat.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PersonDialogDao getPersonDao();

    public abstract MessageDao getMessageDao();
}
