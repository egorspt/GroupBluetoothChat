package com.example.android.bluetoothchat.ChatKit;

import com.stfalcon.chatkit.commons.models.IUser;

public class User implements IUser {
    public String id, name, avatar;
    private boolean online;

    public User(String id, String name, String avatar, boolean online) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.online = online;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAvatar() {
        return avatar;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean bool){
        online = bool;
    }
}
