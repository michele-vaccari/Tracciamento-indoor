package it.evomatic.evoindoor;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import it.evomatic.evoindoor.util.Constants;
import it.evomatic.evoindoor.util.Util;

public class SecondActivity extends AppCompatActivity {

    /////////////// START VARIABILI DI PAGINA ///////////////

    // Componenti della SecondActivity
    private TextView Info;
    private Button Logout;
    private Button Start;
    private Button Stop;

    // Componente per salvare lo stato
    private SharedPreferences stato;

    // Parametri passati dalla MainActivity
    // mi servono per gestire la connessione MQTT
    private String email;
    private String password;

    // Componente per MQTT
    MqttAndroidClient conn1;

    // Componente per Database
    DatabaseHandler db;

    BluetoothAdapter btAdapter;

    private final static int REQUEST_ENABLE_BT = 1;

    /////////////// STOP VARIABILI DI PAGINA ///////////////


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        // Recupero email e password della main activity
        email = getIntent().getExtras().getString("email");
        password = getIntent().getExtras().getString("password");

        // Riferimento ai componenti della SecondActivity
        Info = (TextView) findViewById(R.id.tvInfo);
        Logout = (Button) findViewById(R.id.btnLogout);
        Start = (Button) findViewById(R.id.btnStart);
        Stop = (Button) findViewById(R.id.btnStop);

        // Ricordo l'utente con che email ha effettuato l'accesso
        Info.setText("Accesso effettuato come: " + email);

        // Recupero il valore dello stato
        stato = getSharedPreferences("statoSecondActivity",MODE_PRIVATE);
        Start.setEnabled(stato.getBoolean("start", true));
        Stop.setEnabled(stato.getBoolean("stop", false));



        /////////////// MEMORIZZO LA LISTA DEI SENSORI ///////////////

        // Cancello il database rimasto in memoria
        this.deleteDatabase("evoindoor");
        // Creo il database
        db = new DatabaseHandler(this);


        // Parametri per la connessione MQTT
        final String clientId = email;

        // Imposta l'indirizzo del broker e il nome del client
        conn1 = new MqttAndroidClient(getApplicationContext(), Constants.BROKER_URI, clientId);

        // Setta le opzioni per la connessione MQTT
        MqttConnectOptions o = new MqttConnectOptions();
        o.setUserName(email);
        o.setPassword(password.toCharArray());

        // Effettuo la connessione MQTT
        try {
            IMqttToken token = conn1.connect(o);
            token.setActionCallback(new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // Sono connesso
                    Log.i("SENSORI", "Connessione al broker avvenuta");

                    // Mi iscrivo al topic /sensori
                    try {
                        // effettua chiamata a sottoscrizione del topic
                        IMqttToken subToken = conn1.subscribe(Constants.SUBSCRIBE_TOPIC, 1);
                        subToken.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                // Sono connesso
                                Log.i("SENSORI", "Subscribe topic: /sensori");
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                // La sottoscrizione non funziona, forse l'utente non è autorizzato a
                                // sottoscriversi nel topic specificato e.s. uso di wildcards
                                Log.i("SENSORI", "Subscribe topic: /sensori fallita");
                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Qualcosa è andato storto e.s la connessione è caduta, username o password sono errati
                    Log.i("SENSORI", "Errore nella connessione ");
                }
            });
        } catch (MqttException e) {
            // e.printStackTrace();
            Log.i("SENSORI", "Eccezione MQTT");
        }

        // Popolo la tabella SENSORI del DB

        // Va messa nella second activity
        conn1.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i("SENSORI", "Connessione persa");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i("SENSORI", "Messaggio ricevuto");

                db.deleteAllSensore();

                // Aggiungo sensori al database
                for (int i = 0; i < Util.getSensori(message).length; i++)
                    db.addSensore(Util.getSensori(message)[i]);

                Log.i("SENSORI", "Sensori aggiunti al database");

                // Mi disconneto dal broker e chiudo la connessione MQTT
                conn1.disconnect();
                conn1.close();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        ///////////////////////////////////////////////////////////////


        // Gestisco l'evento click del bottone Logout
        Logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Stop.setEnabled(false);
                Start.setEnabled(true);

                // Fermo il servizio TracciaService
                stopService(v);

                // Redireziono alla MainActivity
                Intent intent = new Intent(SecondActivity.this, MainActivity.class);
                startActivity(intent);

            }
        });


        // Gestisco l'evento click del bottone Start
        Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Controllo se il bluetooth è attivo altrimenti chiedo all'utente di attivarlo
                btAdapter = BluetoothAdapter.getDefaultAdapter();

                if (btAdapter != null && !btAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }

                Stop.setEnabled(true);
                Start.setEnabled(false);

                // Avvio il servizio TracciaService
                startService(v);
            }
        });


        // Gestisco l'evento click del bottone Stop
        Stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Start.setEnabled(true);
                Stop.setEnabled(false);

                // Fermo il servizio TracciaService
                stopService(v);
            }
        });

    }


    @Override
    protected void onPause() {
        super.onPause();
        // Salvo lo stato
        SharedPreferences.Editor ed = stato.edit();
        ed.putBoolean("stop", Stop.isEnabled());
        ed.putBoolean("start", Start.isEnabled());
        ed.commit();
    }


    // Disattivo l'azione per il tasto Indietro
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }


    /////////////// START GESTIONE DEL SERVICE TracciaService ///////////////

    // Fermo il servizio TracciaService
    public void startService(View v) {

        // Invio al service email e password per effettuare la connessione
        Intent intent = new Intent(this,TracciaService.class);
        intent.putExtra("email",email);
        intent.putExtra("password", password);

        // Avvio il servizio
        startService(intent);

        // Informo l'utente
        Toast.makeText(SecondActivity.this, "Servizio avviato...", Toast.LENGTH_LONG).show();
    }

    // Fermo il servizio TracciaService
    public void stopService(View v) {

        // Fermo il servizio
        stopService(new Intent(this,TracciaService.class));

        // Informo l'utente
        Toast.makeText(SecondActivity.this, "Servizio terminato...", Toast.LENGTH_LONG).show();
    }

    /////////////// STOP GESTIONE DEL SERVICE TracciaService ///////////////
}