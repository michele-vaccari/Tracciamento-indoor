package it.evomatic.evoindoor;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import android.bluetooth.BluetoothDevice;

import it.evomatic.evoindoor.blogics.Scansione;
import it.evomatic.evoindoor.util.Constants;
import it.evomatic.evoindoor.util.Util;

/**
 * Created by Michele on 28/11/2017.
 */

public class TracciaService extends IntentService {


    /////////////// VARIABILI DI SERVIZIO ///////////////

    // Mi indica lo stato del servizio
    boolean isEnable;

    // Riferimento ai Thread
    Thread tracciaUpload;
    Thread tracciaBuffer;

    // Parametri passati dalla MainActivity
    // mi servono per gestire la connessione MQTT
    private String email;
    private String password;

    // Componente per MQTT
    MqttAndroidClient client;

    // Componente per Database
    DatabaseHandler db;

    // Componente bluetooth
    private BluetoothAdapter mBluetoothAdapter;

    /////////////////////////////////////////////////////


    // Costruttore
    public TracciaService() {
        super("TracciaService");
    }


    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) { // (1)

        // Recupero email e password della second activity
        email = intent.getExtras().getString("email");
        password = intent.getExtras().getString("password");

        // Creo il database
        db = new DatabaseHandler(this);
        // Abilito l'esecuzione di query multithread
        //db.setWriteAheadLoggingEnabled(true);

        // Creo l'Adapter bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Il servizio è attivo
        isEnable = true;

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) { // (2)

        Log.i("TRACCIA SERVICE", "Avvio il thread tracciaUpload");

        while (isEnable) {

            tracciaUpload = new Thread(new TracciaUpload());
            tracciaUpload.start();

            try {
                tracciaUpload.join();
            } catch (InterruptedException e) {
                Log.i("TRACCIA SERVICE", "Eccezione nell'avvio del thread tracciaUpload");
            }
        }
    }


    @Override
    public void onDestroy() { // (3) QUANDO FERMO IL SERVIZIO
        Log.i("TRACCIA SERVICE", "Stop del service");

        mBluetoothAdapter.stopLeScan(mLeScanCallback);

        tracciaUpload.interrupt();

        isEnable = false;
    }


    /////////////// THREAD 1: UPLOAD TRACCE OGNI n SECONDI ///////////////
    class TracciaUpload implements Runnable {

        int n = 1;

        @Override
        public void run() {

            tracciaBuffer = new Thread(new TracciaBuffer());

            tracciaBuffer.start();


            // Imposta l'indirizzo del broker e il nome del client
            client = new MqttAndroidClient(getApplicationContext(), Constants.BROKER_URI, email);
            // Setta le opzioni per la connessione MQTT
            MqttConnectOptions opzioni = new MqttConnectOptions();
            opzioni.setUserName(email);
            opzioni.setPassword(password.toCharArray());


            while (isEnable) {

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Log.i("TRACCIA SERVICE", "Thread UPLOAD Tracce -> InterruptedException");
                    break;
                }

                // Effettuo la connessione MQTT
                try {
                    IMqttToken token = client.connect(opzioni);
                    token.setActionCallback(new IMqttActionListener() {

                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            // Sono connesso
                            try {
                                client.publish(Constants.PUBLISH_TOPIC + email, Util.getMessaggio(db.getAllTraccia()).getBytes(), 1, false);
                            } catch (MqttException e) {
                                Log.i("TRACCIA SERVICE", "Thread UPLOAD Tracce -> Errore nella pubblicazione della lista delle tracce");
                            }
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            // Qualcosa è andato storto e.s la connessione è caduta, username o password sono errati
                            Log.i("TRACCIA SERVICE", "Thread UPLOAD Tracce -> Errore durante la connessione per l'invio della lista delle tracce");
                        }
                    });
                } catch (MqttException e) {
                    // e.printStackTrace();
                    Log.i("TRACCIA SERVICE", "Thread UPLOAD Tracce -> Eccezione");
                    ;
                }

                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        Log.i("TRACCIA SERVICE", "Thread UPLOAD Tracce -> Connessione persa");
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                        Log.i("TRACCIA SERVICE", "Thread UPLOAD Tracce -> Connessione persa");


                        // Cancello le tracce inviate
                        db.deleteAllTracciaCaricata();
                    }
                });

            }

            // Faccio l'ultimo upload
            //Log.i("TRACCIA UPLOAD", "Faccio l'ultimo upload.");

            client.close();
        }
    }
    /////////////////////////////////////////////////////////////////////


    /////////////// THREAD 2: REGISTRA TRACCIA (SE I TAG SONO DISPONIBILI) OGNI n SECONDI ///////////////
    class TracciaBuffer implements Runnable {

        int rbuffer = 1;

        @Override
        public void run() {
            while (isEnable) {

                mBluetoothAdapter.startLeScan(mLeScanCallback);

                try {
                    Thread.sleep(3000);

                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                    db.addTraccia();

                } catch (InterruptedException e) {
                    Log.i("TRACCIA SERVICE", "Rilevazione Buffer -> InterruptedException");
                }
                Log.i("TRACCIA SERVICE", "Rilevazione buffer n. " + rbuffer++);
            }
        }
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////


    /////////////// GESTIONE DELLA SCANSIONE BLUETOOTH ///////////////

    // Callback per la scansione del bluetooth
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    //Log.i("SCANSIONE BLE", "MAC: " + device.getAddress() + " RSSI: " + rssi);
                    db.addScansione(new Scansione(device.getAddress(),rssi));
                }
            };

    //////////////////////////////////////////////////////////////////
}