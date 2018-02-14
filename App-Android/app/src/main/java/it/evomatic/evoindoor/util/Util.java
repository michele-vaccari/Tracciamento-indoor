package it.evomatic.evoindoor.util;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import it.evomatic.evoindoor.blogics.Sensore;
import it.evomatic.evoindoor.blogics.Traccia;

/**
 * Created by Michele on 27/11/2017.
 */

public class Util {


    // Costruttore privato
    private Util() {}


    // Crea messaggio data la lista delle tracce
    public static String getMessaggio(List<Traccia> listaTracce) {

        String messaggio = "";

        JSONObject m = new JSONObject();
        JSONArray a = new JSONArray();

        for(int i = 0; i < listaTracce.size(); i++) {
            JSONObject t = new JSONObject();
            try {
                t.put("dateTime",listaTracce.get(i).getDateTime());
                t.put("MAC_1",listaTracce.get(i).getMAC_1());
                t.put("potenza_1",listaTracce.get(i).getPotenza_1());
                t.put("MAC_2",listaTracce.get(i).getMAC_2());
                t.put("potenza_2",listaTracce.get(i).getPotenza_2());
                t.put("MAC_3",listaTracce.get(i).getMAC_3());
                t.put("potenza_3",listaTracce.get(i).getPotenza_3());
                a.put(t);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            m.put("tracce", a);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return m.toString();
    }


    // Crea il vettore di sensori dato il messaggio MQTT
    public static Sensore[] getSensori(MqttMessage messaggio) {

        JSONObject m = null;
        JSONArray s = null;
        Sensore[] sensori;

        try {
            m = new JSONObject(messaggio.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            s = new JSONArray(m.getString("sensori"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sensori = new Sensore[s.length()];

        for (int i = 0; i < s.length(); i++) {
            JSONObject tag = null;
            try {
                tag = new JSONObject(s.get(i).toString());
                sensori[i] = new Sensore(tag.getString("indirizzoMAC"), Integer.parseInt(tag.getString("piano")));
            } catch (JSONException e) {
                e.printStackTrace();
            };
        }

        return sensori;

    }


}