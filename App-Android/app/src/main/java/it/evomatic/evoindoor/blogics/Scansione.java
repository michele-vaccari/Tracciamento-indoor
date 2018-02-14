package it.evomatic.evoindoor.blogics;

/**
 * Created by Michele on 28/11/2017.
 */

public class Scansione {

    // Property
    private String MAC;
    private int RSSI;

    // Costruttore
    public Scansione(String MAC, int RSSI) {
        this.MAC = MAC;
        this.RSSI = RSSI;
    }

    // Metodi getter e setter
    public String getMAC() {
        return MAC;
    }

    public void setMAC(String MAC) {
        this.MAC = MAC;
    }

    public int getRSSI() {
        return RSSI;
    }

    public void setRSSI(int RSSI) {
        this.RSSI = RSSI;
    }

}
