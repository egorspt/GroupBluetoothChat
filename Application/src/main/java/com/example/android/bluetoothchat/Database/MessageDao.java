package com.example.android.bluetoothchat.Database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.example.android.bluetoothchat.ChatKit.MessageChat;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface MessageDao {

    @Query("SELECT * FROM messagechat")
    Flowable<List<MessageChat>> getAll();

    @Query("SELECT * FROM messagechat")
    List<MessageChat> getA();

    @Query("SELECT * FROM messagechat ORDER BY rowid DESC LIMIT 1")
    MessageChat getLastMessage();

    @Query("SELECT * FROM messagechat WHERE id = :id")
    MessageChat getById(String id);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(MessageChat employee);

    @Update
    void update(MessageChat employee);

    @Delete
    void delete(MessageChat employee);

    @Query("DELETE FROM messagechat")
    void deleteAll();
}
