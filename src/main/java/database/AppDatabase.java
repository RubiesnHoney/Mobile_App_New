package database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.content.Context;

@Database(entities = {AssetEntity.class}, version = 2, exportSchema = false)  // CHANGED: version = 2
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract AssetDao assetDao();

    // NEW: Migration from version 1 to 2
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add new columns for sync functionality
            database.execSQL("ALTER TABLE assets ADD COLUMN verified INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE assets ADD COLUMN serverAssetId TEXT");
            database.execSQL("ALTER TABLE assets ADD COLUMN lastSyncAttempt INTEGER");
            database.execSQL("ALTER TABLE assets ADD COLUMN syncError TEXT");
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "campus_asset_database")
                    .addMigrations(MIGRATION_1_2)  // NEW: Add migration
                    .fallbackToDestructiveMigration()  // For development only
                    .allowMainThreadQueries() // For simplicity - use background threads in production
                    .build();
        }
        return instance;
    }
}
