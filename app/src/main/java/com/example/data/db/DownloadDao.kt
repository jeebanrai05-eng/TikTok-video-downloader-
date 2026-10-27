package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadedAtMillis DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE isFavorite = 1 ORDER BY downloadedAtMillis DESC")
    fun getFavoriteDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE quality = :quality ORDER BY downloadedAtMillis DESC")
    fun getDownloadsByQuality(quality: String): Flow<List<DownloadItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(item: DownloadItem): Long

    @Delete
    suspend fun deleteDownload(item: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE downloads SET isFavorite = CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}
