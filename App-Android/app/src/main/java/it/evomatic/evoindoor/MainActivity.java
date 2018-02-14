package it.evomatic.evoindoor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import it.evomatic.evoindoor.util.Constants;

public class MainActivity extends AppCompatActivity {

    // Componenti della MainActivity
    private EditText Email;
    private EditText Password;
    private Button Login;

    // Componente per salvare lo stato
    private SharedPreferences stato;

    // Componente per MQTT
    MqttAndroidClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Riferimento ai componenti della MainActivity
        Email = (EditText) findViewById(R.id.etEmail);
        Password = (EditText) findViewById(R.id.etPassword);
        Login = (Button) findViewById(R.id.btnLogin);

        // Recupero il valore dello stato
        stato = getSharedPreferences("statoMainActivity",MODE_PRIVATE);
        stato.edit().clear().apply();
        Email.setText(stato.getString("email", ""));
        Password.setText(stato.getString("password", ""));

        // Gestisco l'evento click del bottone Login
        Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!Email.getText().toString().equals("") && !Password.getText().toString().equals("")) {
                    // Effettua la connessione
                    final String clientId = Email.getText().toString();

                    // Imposta l'indirizzo del broker e il nome del client
                    client = new MqttAndroidClient(getApplicationContext(), Constants.BROKER_URI, clientId);

                    // Setta le opzioni per la connessione MQTT
                    MqttConnectOptions opzioni = new MqttConnectOptions();
                    opzioni.setUserName(Email.getText().toString());
                    opzioni.setPassword(Password.getText().toString().toCharArray());
                    //opzioni.setConnectionTimeout(10);

                    try {
                        IMqttToken token = client.connect(opzioni);
                        token.setActionCallback(new IMqttActionListener() {

                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                // Sono connesso
                                // le credenziali sono state accettate

                                // Mi disconnetto
                                try {
                                    client.disconnect();
                                } catch (MqttException e) {
                                    //e.printStackTrace();
                                }

                                // Avvia la seconda Activity
                                Intent intent = new Intent(MainActivity.this, SecondActivity.class);

                                // Passo alla seconda Activity username e password inseriti
                                intent.putExtra("email",Email.getText().toString());
                                intent.putExtra("password",Password.getText().toString());
                                startActivity(intent);
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                // Qualcosa è andato storto e.s la connessione è caduta, username o password sono errati
                                Log.i("Eccezione","Errore di connessione");
                                Toast.makeText(MainActivity.this, "Username e password errati!", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    catch (MqttException e) {
                        // e.printStackTrace();
                        Log.i("Eccezione","Errore di connessione");
                        Toast.makeText(MainActivity.this, "Errore di connessione", Toast.LENGTH_LONG).show();
                    }
                }
                else // il campo email o il campo password sono vuoti
                    Toast.makeText(MainActivity.this, "Inserisci un email o password valide", Toast.LENGTH_LONG).show();
            }
        });
    }


    protected void onPause() {
        super.onPause();

        // Salvo lo stato
        stato.edit().putString("email", Email.getText().toString()).commit();
        stato.edit().putString("password", Password.getText().toString()).commit();
    }

}