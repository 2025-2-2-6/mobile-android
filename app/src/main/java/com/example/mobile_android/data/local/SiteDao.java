package com.example.mobile_android.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.mobile_android.model.Site;

import java.util.List;

@Dao
public abstract class SiteDao {

    @Query("SELECT * FROM site ORDER BY updatedAt DESC")
    public abstract LiveData<List<Site>> observeAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract void insertAll(List<Site> sites);

    @Query("DELETE FROM site")
    protected abstract void deleteAll();

    @Transaction
    public void replaceAll(List<Site> sites) {
        deleteAll();
        if (sites != null && !sites.isEmpty()) {
            insertAll(sites);
        }
    }
}
