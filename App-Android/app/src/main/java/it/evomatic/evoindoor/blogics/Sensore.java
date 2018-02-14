package it.evomatic.evoindoor.blogics;

/**
 * Created by Michele on 28/11/2017.
 */

public class Sensore {

    // Property
    private String MAC;
    private int piano;

    // Costruttore default
    public Sensore() {}

    // Costruttore
    public Sensore(String MAC, int piano) {
        this.MAC = MAC;
        this.piano = piano;
    }

    // Metodi getter e setter
    public String getMAC() {
        return MAC;
    }

    public void setMAC(String MAC) {
        this.MAC = MAC;
    }

    public int getPiano() {
        return piano;
    }

    public void setPiano(int piano) {
        this.piano = piano;
    }
}
