package it.evomatic.evoindoor.blogics;

/**
 * Created by Michele on 28/11/2017.
 */

public class Traccia {

    // Property
    private String dateTime;
    private String MAC_1;
    private int potenza_1;
    private String MAC_2;
    private int potenza_2;
    private String MAC_3;
    private int potenza_3;
    private String caricata;

    // Costruttore di default
    public Traccia() { }

    // Costruttore
    public Traccia(String dateTime, String MAC_1, int potenza_1, String MAC_2, int potenza_2, String MAC_3, int potenza_3, String caricata) {
        this.dateTime = dateTime;
        this.MAC_1 = MAC_1;
        this.potenza_1 = potenza_1;
        this.MAC_2 = MAC_2;
        this.potenza_2 = potenza_2;
        this.MAC_3 = MAC_3;
        this.potenza_3 = potenza_3;
        this.caricata = caricata;
    }

    // ToString()
    @Override
    public String toString() {
        return  "Datetime: " + this.dateTime +
                " MAC1: " + this.MAC_1 + " P1: " + this.potenza_1 +
                " MAC2: " + this.MAC_2 + " P2: " + this.potenza_2 +
                " MAC3: " + this.MAC_3 + " P3: " + this.potenza_3 +
                " Caricata: " + this.caricata;
    }

    // Metodi getter e setter
    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getMAC_1() {
        return MAC_1;
    }

    public void setMAC_1(String MAC_1) {
        this.MAC_1 = MAC_1;
    }

    public int getPotenza_1() {
        return potenza_1;
    }

    public void setPotenza_1(int potenza_1) {
        this.potenza_1 = potenza_1;
    }

    public String getMAC_2() {
        return MAC_2;
    }

    public void setMAC_2(String MAC_2) {
        this.MAC_2 = MAC_2;
    }

    public int getPotenza_2() {
        return potenza_2;
    }

    public void setPotenza_2(int potenza_2) {
        this.potenza_2 = potenza_2;
    }

    public String getMAC_3() {
        return MAC_3;
    }

    public void setMAC_3(String MAC_3) {
        this.MAC_3 = MAC_3;
    }

    public int getPotenza_3() {
        return potenza_3;
    }

    public void setPotenza_3(int potenza_3) {
        this.potenza_3 = potenza_3;
    }

    public String getCaricata() {
        return caricata;
    }

    public void setCaricata(String caricata) {
        this.caricata = caricata;
    }
}
