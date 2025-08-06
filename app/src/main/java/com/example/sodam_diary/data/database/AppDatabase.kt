package com.example.sodam_diary.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.sodam_diary.data.entity.PhotoEntity

/**
 * 앱의 메인 데이터베이스
 */
@Database(
    entities = [PhotoEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun photoDao(): PhotoDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 버전 1 → 2: userDescription을 nullable로 변경
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // userDescription 컬럼을 nullable로 변경하기 위해 테이블 재생성
                database.execSQL("""
                    CREATE TABLE photos_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        photoPath TEXT NOT NULL,
                        captureDate INTEGER NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        locationName TEXT,
                        imageDescription TEXT,
                        userDescription TEXT
                    )
                """)
                
                // 기존 데이터 복사 (userDescription이 빈 문자열인 경우 NULL로 변환)
                database.execSQL("""
                    INSERT INTO photos_new (id, photoPath, captureDate, latitude, longitude, locationName, imageDescription, userDescription)
                    SELECT id, photoPath, captureDate, latitude, longitude, locationName, imageDescription, 
                           CASE WHEN userDescription = '' THEN NULL ELSE userDescription END
                    FROM photos
                """)
                
                // 기존 테이블 삭제하고 새 테이블로 이름 변경
                database.execSQL("DROP TABLE photos")
                database.execSQL("ALTER TABLE photos_new RENAME TO photos")
            }
        }
        
        /**
         * 데이터베이스 인스턴스 반환 (싱글톤)
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sodam_diary_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}