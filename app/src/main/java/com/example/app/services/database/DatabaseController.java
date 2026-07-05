package com.example.app.services.example;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.app.services.RequestMapping;
import com.example.app.services.RequestContext;
import com.example.app.services.ResponseContext;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class DatabaseController {
    private static final String TAG = "DatabaseController";

    public DatabaseController() {}

    @RequestMapping(path = "/api/database/query", method = "POST")
    public ResponseContext queryDatabase(RequestContext request) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            byte[] bodyBytes = request.getBody();
            String rawBodyText = (bodyBytes != null && bodyBytes.length > 0) ? new String(bodyBytes, StandardCharsets.UTF_8) : "{}";
            
            JSONObject bodyJson = new JSONObject(rawBodyText);
            String dbPath = bodyJson.optString("path", "");
            String sql = bodyJson.optString("sql", "");

            if (dbPath.trim().isEmpty() || sql.trim().isEmpty()) {
                return buildErrorResponse(400, "Required properties missing from body payload: path, sql");
            }

            File dbFile = new File(dbPath);
            if (!dbFile.exists()) {
                return buildErrorResponse(404, "Database file does not exist at path: " + dbPath);
            }

            db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            cursor = db.rawQuery(sql, null);
            
            JSONArray rowsArray = new JSONArray();
            if (cursor != null) {
                String[] columnNames = cursor.getColumnNames();
                while (cursor.moveToNext()) {
                    JSONObject row = new JSONObject();
                    for (int i = 0; i < columnNames.length; i++) {
                        int type = cursor.getType(i);
                        switch (type) {
                            case Cursor.FIELD_TYPE_INTEGER: row.put(columnNames[i], cursor.getLong(i)); break;
                            case Cursor.FIELD_TYPE_FLOAT: row.put(columnNames[i], cursor.getDouble(i)); break;
                            case Cursor.FIELD_TYPE_STRING: row.put(columnNames[i], cursor.getString(i)); break;
                            case Cursor.FIELD_TYPE_BLOB: row.put(columnNames[i], "[Raw Binary Blob]"); break;
                            case Cursor.FIELD_TYPE_NULL: default: row.put(columnNames[i], JSONObject.NULL); break;
                        }
                    }
                    rowsArray.put(row);
                }
            }

            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("rows", rowsArray);
            result.put("row_count", rowsArray.length());

            return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();

        } catch (Exception e) {
            Log.e(TAG, "SQL data read query pipeline failure", e);
            return buildErrorResponse(500, "SQLite analytical engine error: " + e.getMessage());
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            if (db != null && db.isOpen()) db.close();
        }
    }

    @RequestMapping(path = "/api/database/execute", method = "POST")
    public ResponseContext executeDatabaseStatement(RequestContext request) {
        SQLiteDatabase db = null;
        try {
            byte[] bodyBytes = request.getBody();
            String rawBodyText = (bodyBytes != null && bodyBytes.length > 0) ? new String(bodyBytes, StandardCharsets.UTF_8) : "{}";
            
            JSONObject bodyJson = new JSONObject(rawBodyText);
            String dbPath = bodyJson.optString("path", "");
            String sql = bodyJson.optString("sql", "");

            if (dbPath.trim().isEmpty() || sql.trim().isEmpty()) {
                return buildErrorResponse(400, "Required properties missing from body payload: path, sql");
            }

            String lowerSql = sql.trim().toLowerCase();
            if (lowerSql.contains("drop ") || lowerSql.contains("alter ") || lowerSql.contains("vacuum")) {
                return buildErrorResponse(403, "Structural schema alterations are blocked in test framework contexts");
            }

            File dbFile = new File(dbPath);
            if (!dbFile.exists()) {
                return buildErrorResponse(404, "Target database file does not exist at path: " + dbPath);
            }

            db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
            db.execSQL(sql);

            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("message", "SQL statement executed successfully");

            return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();

        } catch (Exception e) {
            Log.e(TAG, "SQL modification execution pipeline failure", e);
            return buildErrorResponse(500, "SQLite mutation transaction error: " + e.getMessage());
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }
    @RequestMapping(path = "/api/database/delete", method = "POST")
    public ResponseContext deleteDatabaseFile(RequestContext request) {
        try {
            byte[] bodyBytes = request.getBody();
            String rawBodyText = (bodyBytes != null && bodyBytes.length > 0) ? new String(bodyBytes, StandardCharsets.UTF_8) : "{}";
            
            JSONObject bodyJson = new JSONObject(rawBodyText);
            String dbPath = bodyJson.optString("path", "");

            if (dbPath.trim().isEmpty()) {
                return buildErrorResponse(400, "Required property missing from body payload: path");
            }

            File dbFile = new File(dbPath);
            if (!dbFile.exists()) {
                return buildErrorResponse(404, "Target database file does not exist for deletion: " + dbPath);
            }

            // Using Android's native SQLiteDatabase file removal hook safely purges related journal/WAL caches
            boolean deleted = SQLiteDatabase.deleteDatabase(dbFile);

            JSONObject result = new JSONObject();
            if (deleted) {
                result.put("status", "success");
                result.put("message", "Database file and its structural system journals purged completely");
                return ResponseContext.status(200).contentType("application/json").body(result.toString()).build();
            } else {
                return buildErrorResponse(500, "The OS rejected file deletion. File may be locked by an active external thread connection pool.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Database deletion pipeline failure", e);
            return buildErrorResponse(500, "SQLite file cleanup error: " + e.getMessage());
        }
    }

    private ResponseContext buildErrorResponse(int code, String message) {
        JSONObject errJson = new JSONObject();
        try {
            errJson.put("status", "error");
            errJson.put("message", message);
        } catch (Exception ignored) {}
        return ResponseContext.status(code).contentType("application/json").body(errJson.toString()).build();
    }
}

