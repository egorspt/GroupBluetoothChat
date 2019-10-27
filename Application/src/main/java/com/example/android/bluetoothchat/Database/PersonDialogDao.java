package com.example.android.bluetoothchat.Database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.example.android.bluetoothchat.ChatKit.PersonDialog;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface PersonDialogDao {

    @Query("SELECT * FROM persondialog")
    Flowable<List<PersonDialog>> getAll();

    @Query("SELECT * FROM persondialog")
    List<PersonDialog> getA();

    @Query("SELECT * FROM persondialog WHERE id = :id")
    PersonDialog getById(String id);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(PersonDialog employee);

    @Update
    void update(PersonDialog employee);

    @Delete
    void delete(PersonDialog employee);

    @Query("DELETE FROM persondialog")
    void deleteAll();
}
