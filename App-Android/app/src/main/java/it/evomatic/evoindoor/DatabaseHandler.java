package it.evomatic.evoindoor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import it.evomatic.evoindoor.blogics.Scansione;
import it.evomatic.evoindoor.blogics.Sensore;
import it.evomatic.evoindoor.blogics.Traccia;

/**
 * Created by Michele on 28/11/2017.
 */

public class DatabaseHandler extends SQLiteOpenHelper {

    // Property
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "evoindoor";    // Nome Database

    private static final String TABLE_SENSORE = "sensore";      // Nome tabella SENSORE
    private static final String T_SNS_COL_1 = "mac";            // Nome colonna 1 tabella SENSORE
    private static final String T_SNS_COL_2 = "piano";            // Nome colonna 2 tabella SENSORE

    private static final String TABLE_TRACCIA = "traccia";      // Nome tabella TRACCIA
    private static final String T_T_COL_1 = "datetime";         // Nome colonna 1 tabella TRACCIA
    private static final String T_T_COL_2 = "mac_1";            // Nome colonna 2 tabella TRACCIA
    private static final String T_T_COL_3 = "potenza_1";        // Nome colonna 3 tabella TRACCIA
    private static final String T_T_COL_4 = "mac_2";            // Nome colonna 4 tabella TRACCIA
    private static final String T_T_COL_5 = "potenza_2";        // Nome colonna 5 tabella TRACCIA
    private static final String T_T_COL_6 = "mac_3";            // Nome colonna 6 tabella TRACCIA
    private static final String T_T_COL_7 = "potenza_3";        // Nome colonna 7 tabella TRACCIA
    private static final String T_T_COL_8 = "caricato";         // Nome colonna 8 tabella TRACCIA

    private static final String TABLE_SCANSIONE = "scansione";  // Nome tabella SCANSIONE
    private static final String T_SCN_COL_1 = "mac";            // Nome colonna 1 tabella SCANSIONE
    private static final String T_SCN_COL_2 = "rssi";           // Nome colonna 2 tabella SCANSIONE

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Crea le tabelle
    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql_1 = "CREATE TABLE " + TABLE_SENSORE + " ("
                       + T_SNS_COL_1 + " TEXT,"
                       + T_SNS_COL_2 + " INTEGER)";
        String sql_2 = "CREATE TABLE " + TABLE_TRACCIA + " ("
                       + T_T_COL_1 + " DATETIME DEFAULT (datetime('now','localtime')),"
                       + T_T_COL_2 + " TEXT,"
                       + T_T_COL_3 + " INTEGER,"
                       + T_T_COL_4 + " TEXT,"
                       + T_T_COL_5 + " INTEGER,"
                       + T_T_COL_6 + " TEXT,"
                       + T_T_COL_7 + " INTEGER,"
                       + T_T_COL_8 + " TEXT)";
        String sql_3 = "CREATE TABLE " + TABLE_SCANSIONE + " ("
                       + T_SCN_COL_1 + " TEXT,"
                       + T_SCN_COL_2 + " INTEGER)";
        db.execSQL(sql_1);
        db.execSQL(sql_2);
        db.execSQL(sql_3);
        Log.i("DB","DATABASE CREATO");
    }

    // Aggiorna le tabelle
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = " DROP TABLE IF EXISTS " + TABLE_SENSORE +
                     " DROP TABLE IF EXISTS " + TABLE_TRACCIA +
                     " DROP TABLE IF EXISTS " + TABLE_SCANSIONE;

        // Fai cadere le tabelle se esistevano
        db.execSQL(sql);

        // Crea le tabelle di nuovo
        onCreate(db);
    }


    /////////////// OPERAZIONI CRUD SENSORE ///////////////

    // Aggiungi un nuovo sensore
    public void addSensore(Sensore sensore) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues valori = new ContentValues();
        valori.put(T_SNS_COL_1, sensore.getMAC());      // MAC
        valori.put(T_SNS_COL_2, sensore.getPiano());    // Piano

        // Inserisci righe
        db.insert(TABLE_SENSORE, null, valori);
        db.close(); // Chiudi la connessione al database
    }

    // Cancella tutti i sensori
    public void deleteAllSensore() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_SENSORE);
        db.close();
    }
    ///////////////////////////////////////////////////////

    /////////////// OPERAZIONI CRUD TRACCIA ///////////////

    // Aggiungi una nuova traccia
    public void addTraccia() {
        SQLiteDatabase db = this.getWritableDatabase();

        int distinctMAC = 0;

        // Prima faccio il GroupBy e conto se ho almeno 3 MAC Address diversi
        String sqlDistinct = "SELECT COUNT(DISTINCT " + T_SCN_COL_1 + ") FROM " + TABLE_SCANSIONE + ";";

        Cursor cursor = db.rawQuery(sqlDistinct, null);

        // Leggi tutte le righe della tabella traccia
        if (cursor.moveToFirst()) {
            do {
                distinctMAC = Integer.parseInt(cursor.getString(0));

            } while (cursor.moveToNext());
        }

        if (distinctMAC >= 3) {
            // Creo la traccia
            String sql = " SELECT " + T_SCN_COL_1 + ", AVG(" + T_SCN_COL_2 + ") AS Potenza" +
                         " FROM " + TABLE_SCANSIONE +
                         " GROUP BY " + T_SCN_COL_1 +
                         " ORDER BY Potenza DESC" +
                         " LIMIT 3;";

            cursor = db.rawQuery(sql, null);

            ContentValues valori = new ContentValues();

            // Creo la traccia
            if (cursor.moveToFirst()) {
                valori.put(T_T_COL_2, cursor.getString(0));
                valori.put(T_T_COL_3, (int) Double.parseDouble(cursor.getString(1)));
                cursor.moveToNext();
                valori.put(T_T_COL_4, cursor.getString(0));
                valori.put(T_T_COL_5, (int) Double.parseDouble(cursor.getString(1)));
                cursor.moveToNext();
                valori.put(T_T_COL_6, cursor.getString(0));
                valori.put(T_T_COL_7, (int) Double.parseDouble(cursor.getString(1)));
            }
            valori.put(T_T_COL_8, "N");

            // Inserisci righe
            db.insert(TABLE_TRACCIA, null, valori);
            Log.i("DB", "Traccia inserita");
            // cancello tabella scansioni
            deleteAllScansioni();
        }

        //db.close(); // Chiudi la connessione al database
    }

    // update traccia
    public int updateTraccia(String datetime) {
        SQLiteDatabase db = this.getWritableDatabase();

        String sql = "SELECT * FROM " + TABLE_TRACCIA;

        Cursor cursor = db.rawQuery(sql, null);

        ContentValues values = new ContentValues();
        values.put(T_T_COL_8, "Y");

        // aggiorna riga
        return db.update(TABLE_TRACCIA, values, T_T_COL_1 + " = ?",
                new String[] { String.valueOf(datetime) });
    }

    // get all traccia
    public List<Traccia> getAllTraccia() {
        List<Traccia> tracciaList = new ArrayList<Traccia>();

        SQLiteDatabase db = this.getWritableDatabase();

        String sql = "SELECT * FROM " + TABLE_TRACCIA + " WHERE " + T_T_COL_8 + "='N'";

        Cursor cursor = db.rawQuery(sql, null);

        // Leggi tutte le righe della tabella traccia
        if (cursor.moveToFirst()) {
            do {
                Traccia traccia = new Traccia();
                traccia.setDateTime(cursor.getString(0));
                traccia.setMAC_1(cursor.getString(1));
                traccia.setPotenza_1(Integer.parseInt(cursor.getString(2)));
                traccia.setMAC_2(cursor.getString(3));
                traccia.setPotenza_2(Integer.parseInt(cursor.getString(4)));
                traccia.setMAC_3(cursor.getString(5));
                traccia.setPotenza_3(Integer.parseInt(cursor.getString(6)));
                traccia.setCaricata(cursor.getString(7));

                Log.i("DB","Traccia da inviare: --> " + traccia);
                // Aggiungi traccia alla lista
                tracciaList.add(traccia);

                // Update traccia
                updateTraccia(traccia.getDateTime());
            } while (cursor.moveToNext());
        }

        return tracciaList;
    }

    // Cancella tutte le tracce caricate
    public void deleteAllTracciaCaricata() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRACCIA,T_T_COL_8 + " = ?",new String[] { String.valueOf("Y")});
        //db.close();
    }
    ///////////////////////////////////////////////////////

    /////////////// OPERAZIONI CRUD SCANSIONE ///////////////

    // Aggiungi una nuova scansione
    public void addScansione(Scansione scansione) {
        SQLiteDatabase db = this.getWritableDatabase();

        int presente = 0;

        String sql = " SELECT COUNT(*)" +
                     " FROM " + TABLE_SENSORE +
                     " WHERE " + T_SNS_COL_1 + "='" + scansione.getMAC() + "';";

        Cursor cursor = db.rawQuery(sql, null);

        if (cursor.moveToFirst()) {
            presente = cursor.getInt(0);
        }
        cursor.close();

        //Log.i("DB", "presente -> " + presente);

        // Se la scansione contiene il MAC di un sensore allora la memorizzo
        if (presente >= 1) {

            ContentValues valori = new ContentValues();
            valori.put(T_SCN_COL_1, scansione.getMAC());    // MAC
            valori.put(T_SCN_COL_2, scansione.getRSSI());   // RSSI

            //Log.i("DB", "inserimento MAC: " + scansione.getMAC() + " RSSI: " + scansione.getRSSI());

            // Inserisci righe
            db.insert(TABLE_SCANSIONE, null, valori);
        }
        //db.close(); // Chiudi la connessione al database
    }

    // Cancella tutte le scansioni
    public void deleteAllScansioni() {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_SCANSIONE, null, null);

        Log.i("DB", "scansioni eliminate");

        //db.close();
    }
    ///////////////////////////////////////////////////////

}
