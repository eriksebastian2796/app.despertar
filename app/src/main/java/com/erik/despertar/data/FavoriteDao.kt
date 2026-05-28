package com.erik.despertar.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt ASC")
    fun getAllFavorites(): Flow<List<FavoriteApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: FavoriteApp)

    @Delete
    suspend fun delete(app: FavoriteApp)
}
