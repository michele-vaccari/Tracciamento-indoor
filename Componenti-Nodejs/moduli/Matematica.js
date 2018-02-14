// Costruttore:
function Matematica() {
    // Istanzio una nuova connessione
}

// Conversione da RSSI a Metri
Matematica.prototype.calcoloRssiMetri = function(rssi) {
    
    var potenzaTx = -63 // valore della potenza codificato. Di solito varia da -59 a -65
    
    if (rssi == 0) {
        return -1.0; 
    }
    
    var ratio = rssi * 1.0 / potenzaTx;
    
    if (ratio < 1.0) {
        return Math.pow(ratio,10);
    }
    else {
        var distance =  (0.89976) * Math.pow(ratio,7.7095) + 0.111;    
        return distance;
    }
    
}

// Trilaterazione
Matematica.prototype.eseguiTrilaterazione = function(x1, y1, d1, x2, y2, d2, x3, y3, d3) {

    return getMediaPunti(new Array(getTrilaterazione(x1, y1, d1, x2, y2, d2, x3, y3, d3), getTrilaterazione(x3, y3, d3, x1, y1, d1, x2, y2, d2), getTrilaterazione(x2, y2, d2, x3, y3, d3, x1, y1, d1)))

}


// getTrilaterazione
getTrilaterazione = function(x1, y1, d1, x2, y2, d2, x3, y3, d3) {
    
    // oggetto da ritornare: è composto da due valori risultato.x e risultato.y
    var risultato = new Object();

    // verifico se i due cerchi si incrociano
    if (Math.sqrt(Math.pow(x1-x2,2) + Math.pow(y1-y2,2)) <= (d1+d2)) {
        // dichiarazione variabili
        var a1 = -2 * x1;
        var b1 = -2 * y1;
        var c1 = Math.pow(x1,2) + Math.pow(y1,2) - Math.pow(d1,2);

        var a2 = -2 * x2;
        var b2 = -2 * y2;
        var c2 = Math.pow(x2,2) + Math.pow(y2,2) - Math.pow(d2,2);

        if(a1 != a2) {
            // ricavo le coordinate del polinomio
            var a = 1 + Math.pow(b2-b1,2) / Math.pow(a1-a2,2);
            var b = 2 * (b2-b1) * (c2-c1) / Math.pow(a1-a2,2) + (a1 * (b2-b1)) / (a1-a2) + b1;
            var c = Math.trunc(c1 + a1 * (c2-c1) / (a1-a2) + Math.pow(c2-c1,2) / Math.pow(a1-a2,2));


            // risolvo l'eq. di secondo grado a*x^2 + b*x + c
            var delta = Math.sqrt(Math.pow(b,2) - 4*a*c);
            var ys1 = (-b + delta) / (2*a);
            var ys2 = (-b - delta) / (2*a);

            // ricavo i due valori di x risolvendo l'eq.
            var xs1 = (c2-c1) / (a1-a2) + ((b2-b1) * ys1) / (a1-a2);
            var xs2 = (c2-c1) / (a1-a2) + ((b2-b1) * ys2) / (a1-a2);

            // se le due soluzioni sono uguali ho trovato il punto di intersezione
            if(xs1 == xs2 && ys1 == ys2) {
                risultato.x = xs1;
                risultato.y = ys1;
                return risultato;
            }
            else { // altrimenti la distanza del nodo stimato dal terzo anchor
                d1 = Math.sqrt(Math.pow(xs1-x3,2) + Math.pow(ys1-y3,2));
                d2 = Math.sqrt(Math.pow(xs2-x3,2) + Math.pow(ys2-y3,2));

                if ((d1 - d3) < (d2 - d3)) {
                    risultato.x = xs1;
                    risultato.y = ys1;
                    return risultato;
                }
                else {
                    risultato.x = xs2;
                    risultato.y = ys2;
                    return risultato;
                }
            }
        }
        else {
            // ricavo le coordinate del polinomio
            var a = 1 + Math.pow(a2-a1,2) / Math.pow(b1-b2,2);
            var b = 2 * (a2-a1) * (c2-c1) / Math.pow(b1-b2,2) + (b1 * (a2-a1)) / (b1-b2) + a1;
            var c = Math.trunc(c1 + b1 * (c2-c1) / (b1-b2) + Math.pow(c2-c1,2) / Math.pow(b1-b2,2));

            // risolvo l'eq. di secondo grado a*x^2 + b*x + c
            var delta = Math.sqrt(Math.pow(b,2) - 4*a*c);
            var xs1 = (-b + delta) / (2*a);
            var xs2 = (-b - delta) / (2*a);

            // ricavo i due valori di y risolvendo l'eq.
            var ys1 = (c2-c1) / (b1-b2) + ((a2-a1) * xs1) / (b1-b2);
            var ys2 = (c2-c1) / (b1-b2) + ((a2-a1) * xs2) / (b1-b2);

            // se le due soluzioni sono uguali ho trovato il punto di intersezione
            if(xs1 == xs2 && ys1 == ys2) {
                risultato.x = xs1;
                risultato.y = ys1;
                return risultato;
            }
            else { // altrimenti la distanza del nodo stimato dal terzo anchor
                d1 = Math.sqrt(Math.pow(xs1-x3,2) + Math.pow(ys1-y3,2));
                d2 = Math.sqrt(Math.pow(xs2-x3,2) + Math.pow(ys2-y3,2));

                if ((d1 - d3) < (d2 - d3)) {
                    risultato.x = xs1;
                    risultato.y = ys1;
                    return risultato;
                }
                else {
                    risultato.x = xs2;
                    risultato.y = ys2;
                    return risultato;
                }
            }
        }
    }
    else { // altrimenti se i due cerchi non si incrociano
        var alfa = (Math.sqrt(Math.pow(x1-x2,2) + Math.pow(y1-y2,2)) - (d1+d2)) / 2;

        // nuovi raggi
        d1 += alfa;
        d2 += alfa;

        return getTrilaterazione(x1, y1, d1, x2, y2, d2, x3, y3, d3);
    }
}

getMediaPunti = function(punti) {
    
    var r = new Object();
    r.x = 0;
    r.y = 0;

    for (var i = 0; i < punti.length; i++) {
        r.x += punti[i].x;
        r.y += punti[i].y;
    }

    r.x = r.x / punti.length;
    r.y = r.y / punti.length;

    return r;
}

module.exports = Matematica;