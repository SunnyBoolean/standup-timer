package net.johnpwood.android.standuptimer.dao;

import static android.provider.BaseColumns._ID;

import java.util.ArrayList;
import java.util.List;

import net.johnpwood.android.standuptimer.model.Team;
import net.johnpwood.android.standuptimer.utils.Logger;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class TeamDAO extends DAOHelper {
    private static final String TABLE_NAME = "teams";
    private static final String NAME = "name";
    private static final String[] ALL_COLUMS = { _ID, NAME };

    public TeamDAO(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                NAME + " TEXT NOT NULL" +
                ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public Team save(Team team) {
        SQLiteDatabase db = getWritableDatabase();
        if (team.getId() != null) {
            return updateExistingTeam(db, team);
        } else {
            return createNewTeam(db, team);
        }
    }

    public Team findById(Long id) {
        Cursor cursor = null;
        Team team = null;

        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(TABLE_NAME, ALL_COLUMS, _ID + " = ?", new String[]{id.toString()}, null, null, null);
            if (cursor.getCount() == 1) {
                if (cursor.moveToFirst()) {
                    String name = cursor.getString(1);
                    team = new Team(id, name);
                }
            }
        } finally {
            closeCursor(cursor);
        }

        return team;
    }

    public Team findByName(String name) {
        Cursor cursor = null;
        Team team = null;
        name = name.trim();

        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(TABLE_NAME, ALL_COLUMS, NAME + " = ?", new String[]{name}, null, null, null);
            if (cursor.getCount() == 1) {
                if (cursor.moveToFirst()) {
                    long id = cursor.getLong(0);
                    name = cursor.getString(1);
                    team = new Team(id, name);
                }
            }
        } finally {
            closeCursor(cursor);
        }

        Logger.d((team == null ? "Unsuccessfully" : "Successfully") + " found team with a name of '" + name + "'");
        return team;
    }

    public List<String> findAllTeamNames() {
        List<String> teamNames = new ArrayList<String>();
        Cursor cursor = null;

        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(TABLE_NAME, new String[]{NAME}, null, null, null, null, NAME);
            while (cursor.moveToNext()) {
                teamNames.add(cursor.getString(0));
            }
        } finally {
            closeCursor(cursor);
        }

        Logger.d("Found " + teamNames.size() + " teams");
        return teamNames;
    }

    public void deleteAll() {
        Logger.d("Deleting all teams");
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
    }

    public void delete(Team team) {
        Logger.d("Deleting team with the name of '" + team.getName() + "'");
        if (team.getId() != null) {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(TABLE_NAME, _ID + " = ?", new String[]{team.getId().toString()});
        }
    }

    private boolean attemptingToCreateDuplicateTeam(Team team) {
        return team.getId() == null && findByName(team.getName()) != null;
    }

    private Team createNewTeam(SQLiteDatabase db, Team team) {
        if (team.getName() == null || team.getName().trim().length() == 0) {
            String msg = "Attempting to create a team with an empty name";
            Logger.w(msg);
            throw new InvalidTeamNameException(msg);
        }

        if (attemptingToCreateDuplicateTeam(team)) {
            String msg = "Attempting to create duplicate team with the name " + team.getName();
            Logger.w(msg);
            throw new DuplicateTeamException(msg);
        }

        Logger.d("Creating new team with a name of '" + team.getName() + "'");
        ContentValues values = new ContentValues();
        values.put(NAME, team.getName());
        long id = db.insertOrThrow(TABLE_NAME, null, values);
        return new Team(id, team.getName());
    }

    private Team updateExistingTeam(SQLiteDatabase db, Team team) {
        Logger.d("Updating team with the name of '" + team.getName() + "'");
        ContentValues values = new ContentValues();
        values.put(NAME, team.getName());
        long id = db.update(TABLE_NAME, values, _ID + " = ?", new String[]{team.getId().toString()});
        return new Team(id, team.getName());
    }
}
