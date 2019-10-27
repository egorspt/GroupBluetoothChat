package com.example.android.bluetoothchat.Database;

import android.arch.persistence.room.TypeConverter;

import com.example.android.bluetoothchat.ChatKit.MessageChat;
import com.example.android.bluetoothchat.ChatKit.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PersonDialogConverter {
    @TypeConverter
    public String fromUsers(ArrayList<User> users) {
        if (users == null) {
            return (null);
        }
        Gson gson = new Gson();
        Type type = new TypeToken<List<User>>() {
        }.getType();
        String json = gson.toJson(users, type);
        return json;
    }

    @TypeConverter
    public ArrayList<User> toUsers(String usersString) {
        if (usersString == null) {
            return (null);
        }
        Gson gson = new Gson();
        Type type = new TypeToken<List<User>>() {
        }.getType();
        ArrayList<User> productCategoriesList = gson.fromJson(usersString, type);
        return productCategoriesList;
    }
    @TypeConverter
    public String fromUser(User user) {
        if (user == null) {
            return (null);
        }
        Gson gson = new Gson();
        Type type = new TypeToken<User>() {
        }.getType();
        String json = gson.toJson(user, type);
        return json;
    }

    @TypeConverter
    public User toUser(String userString) {
        if (userString == null) {
            return (null);
        }
        Gson gson = new Gson();
        Type type = new TypeToken<User>() {
        }.getType();
        User productCategoriesList = gson.fromJson(userString, type);
        return productCategoriesList;
    }

    @TypeConverter
    public String fromLastMessage(MessageChat users) {
        if (users == null) {
            return (null);
        }
        Gson gson = new Gson();
        Type type = new TypeToken<MessageChat>() {
        }.getType();
        String json = gson.toJson(users, type);
        return json;
    }

    @TypeConverter
    public MessageChat toLastMessage(String usersString) {
        if (usersString == null) {
            return (null);
        }
        Gson gson = new Gson();
        Type type = new TypeToken<MessageChat>() {
        }.getType();
        MessageChat productCategoriesList = gson.fromJson(usersString, type);
        return productCategoriesList;
    }

    @TypeConverter
    public static Date fromDate(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long toDate(Date date) {
        return date == null ? null : date.getTime();
    }
}
