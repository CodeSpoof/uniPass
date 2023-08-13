package io.codespoof.univpassm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DBManager {

    private DatabaseHelper dbHelper;

    private final Context context;

    private SQLiteDatabase database;

    public DBManager(Context c) {
        context = c;
    }

    public void open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long insert(String content) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.CONTENT, content);
        long ret = database.insert(DatabaseHelper.TABLE_NAME, null, contentValue);
        clean();
        return ret;
    }

    public Cursor fetch() {
        String[] columns = new String[] {DatabaseHelper._ID, DatabaseHelper.DATETIME, DatabaseHelper.CONTENT };
        Cursor cursor = database.query(DatabaseHelper.TABLE_NAME, columns, null, null, null, null, DatabaseHelper.DATETIME + " DESC");
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public void clean() {
        database.delete(DatabaseHelper.TABLE_NAME, DatabaseHelper.DATETIME + " < datetime('now', '-1 year')", null);
    }

    public void delete(long _id) {
        database.delete(DatabaseHelper.TABLE_NAME, DatabaseHelper._ID + "=" + _id, null);
    }

}
