package com.example.android.bluetoothchat.ChatKit;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.android.bluetoothchat.Database.PersonDialogConverter;
import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.MessageContentType;

import java.util.Calendar;
import java.util.Date;

@Entity
@TypeConverters(PersonDialogConverter.class)
public class MessageChat implements IMessage,
        MessageContentType.Image {
    @PrimaryKey
    @NonNull
    public String id = "EGOR";
    public String text, image;
    public User user;
    public Date date = Calendar.getInstance().getTime();

    @Ignore
    MessageChat(String text) {
        this.text = text;
    }

    public MessageChat(String id, User user, String text) {
        this(id, user, text, new Date(), null);
    }

    public MessageChat(String id, User user, int image) {
        this(id, user, "image", new Date(), id);
    }

    @Ignore
    public MessageChat(String id, User user, String text, Date createdAt, String image) {
        this.id = id;
        this.text = text;
        this.user = user;
        this.date= createdAt;
        this.image = image;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public User getUser() {
        return this.user;
    }

    @Override
    public Date getCreatedAt() {
        return date;
    }

    @Nullable
    @Override
    public String getImageUrl() {
        return image;
    }
}
