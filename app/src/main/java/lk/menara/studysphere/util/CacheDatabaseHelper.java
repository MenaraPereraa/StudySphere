package lk.menara.studysphere.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CacheDatabaseHelper extends SQLiteOpenHelper {

    private static CacheDatabaseHelper instance;

    private static final String DATABASE_NAME = "StudySphereCache.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_CACHE = "cache";
    private static final String COLUMN_KEY = "cache_key";
    private static final String COLUMN_PAYLOAD = "payload";
    private static final String COLUMN_UPDATED_AT = "updated_at";

    private static final String TABLE_SYNC_QUEUE = "sync_queue";
    private static final String COLUMN_SYNC_ID = "id";
    private static final String COLUMN_COLLECTION = "collection_name";
    private static final String COLUMN_DATA_JSON = "data_json";

    public static final String KEY_HOME_PRODUCTS = "home_products";
    public static final String KEY_DISCOVER_PRODUCTS = "discover_products";
    public static final String KEY_SELLER_PRODUCTS = "seller_products";

    private static final long CACHE_VALIDITY_MS = 24L * 60 * 60 * 1000;

    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_CACHE + " (" +
            COLUMN_KEY + " TEXT PRIMARY KEY, " +
            COLUMN_PAYLOAD + " TEXT NOT NULL, " +
            COLUMN_UPDATED_AT + " INTEGER NOT NULL)";

    private static final String CREATE_TABLE_SYNC_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_SYNC_QUEUE + " (" +
            COLUMN_SYNC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_COLLECTION + " TEXT NOT NULL, " +
            COLUMN_DATA_JSON + " TEXT NOT NULL)";

    private final Gson gson = new Gson();

    public static synchronized CacheDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new CacheDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private CacheDatabaseHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SQL);
        db.execSQL(CREATE_TABLE_SYNC_SQL);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CACHE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SYNC_QUEUE);
        onCreate(db);
    }

    public void queueForSync(String collection, Map<String, Object> data) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_COLLECTION, collection);
            values.put(COLUMN_DATA_JSON, gson.toJson(data));
            db.insert(TABLE_SYNC_QUEUE, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<SyncItem> getPendingSyncItems() {
        List<SyncItem> items = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SYNC_QUEUE, null)) {
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SYNC_ID));
                    String collection = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COLLECTION));
                    String json = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATA_JSON));
                    items.add(new SyncItem(id, collection, json));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    public void deleteSyncItem(int id) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(TABLE_SYNC_QUEUE, COLUMN_SYNC_ID + " = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class SyncItem {
        public int id;
        public String collection;
        public String dataJson;

        public SyncItem(int id, String collection, String dataJson) {
            this.id = id;
            this.collection = collection;
            this.dataJson = dataJson;
        }
    }

    public void put(String key, Object data) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("INSERT OR REPLACE INTO " + TABLE_CACHE + " (" +
                            COLUMN_KEY + ", " + COLUMN_PAYLOAD + ", " + COLUMN_UPDATED_AT + ") VALUES (?, ?, ?)",
                    new Object[]{key, gson.toJson(data), System.currentTimeMillis()});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String get(String key) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            try (Cursor cursor = db.rawQuery(
                    "SELECT " + COLUMN_PAYLOAD + " FROM " + TABLE_CACHE + " WHERE " + COLUMN_KEY + " = ?",
                    new String[]{key})) {
                return cursor.moveToFirst() ? cursor.getString(0) : null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isFresh(String key) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            try (Cursor cursor = db.rawQuery(
                    "SELECT " + COLUMN_UPDATED_AT + " FROM " + TABLE_CACHE + " WHERE " + COLUMN_KEY + " = ?",
                    new String[]{key})) {
                if (cursor.moveToFirst()) {
                    return (System.currentTimeMillis() - cursor.getLong(0)) < CACHE_VALIDITY_MS;
                }
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void clear(String key) {
        try {
            getWritableDatabase().execSQL(
                    "DELETE FROM " + TABLE_CACHE + " WHERE " + COLUMN_KEY + " = ?", new Object[]{key});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearAll() {
        try {
            getWritableDatabase().execSQL("DELETE FROM " + TABLE_CACHE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}