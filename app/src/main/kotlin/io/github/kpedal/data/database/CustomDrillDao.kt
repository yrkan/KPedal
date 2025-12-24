package io.github.kpedal.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for custom drills.
 */
@Dao
interface CustomDrillDao {

    @Query("SELECT * FROM custom_drills ORDER BY createdAt DESC")
    fun getAllDrills(): Flow<List<CustomDrillEntity>>

    @Query("SELECT * FROM custom_drills WHERE id = :id")
    suspend fun getDrill(id: Long): CustomDrillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(drill: CustomDrillEntity): Long

    @Update
    suspend fun update(drill: CustomDrillEntity)

    @Query("DELETE FROM custom_drills WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM custom_drills")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM custom_drills")
    suspend fun getCount(): Int
}
