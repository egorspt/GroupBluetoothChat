package com.example.android.bluetoothchat.ChatKit;


import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.example.android.bluetoothchat.Database.PersonDialogConverter;
import com.stfalcon.chatkit.commons.models.IDialog;

import java.util.ArrayList;

@Entity
@TypeConverters(PersonDialogConverter.class)
public class PersonDialog implements IDialog<MessageChat> {
    @PrimaryKey
    @NonNull
    public String id;
    public String admin, dialogPhoto, dialogName;
    public MessageChat lastMessage = new MessageChat("2", new User("1", "EGOR", null, true), "Conversation created");
    public int unreadCount = 0;
    public ArrayList<User> users = new ArrayList<>();

    public PersonDialog(String dialogName, String id,
                        ArrayList<User> users, String admin) {
        this.dialogName = dialogName;
        this.id = id;
        this.users = users;
        this.admin = admin;
        this.dialogPhoto = dialogName;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getAdmin() {
        return admin;
    }
    @Override
    public String getDialogPhoto() {
        return dialogPhoto;
    }

    @Override
    public String getDialogName() {
        return dialogName;
    }

    public void addUser(User user){
        users.add(user);
    }

    @Override
    public ArrayList<User> getUsers() {
        return users;
    }

    @Override
    public MessageChat getLastMessage() {
        return lastMessage;
    }

    @Override
    public void setLastMessage(MessageChat lastMessage) {
        this.lastMessage = lastMessage;
    }

    @Override
    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int count){
        unreadCount = count;
    }

    public void addUnreadCount(){
        unreadCount++;
    }
}
