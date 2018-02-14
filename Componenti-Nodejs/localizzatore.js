// Dipendenze:
var mqtt = require('mqtt')
var mssql = require('mssql');
var matematica = require('./moduli/Matematica')
var KalmanFilter = require('kalmanjs').default;
var indirizzoBroker = 'mqtt://192.168.0.126';

// Crea la connessione
var connessioneDB = new mssql.Connection({
    server: "62.173.163.189",
    database: "EVOIndoor",
    user: "michele",
    password: "michele",
    port: 1433
});

var opzioniSQL = {
    server: "62.173.163.189",
    database: "EVOIndoor",
    user: "michele",
    password: "michele",
    port: 1433
};

var m = new matematica();
var kf = new KalmanFilter();

// Effettua la connessione al database
connessioneDB.connect(function(err) {
	if (err) throw err;
	console.log("Database connesso!");
});

var opzioni = {
    clientId: 'client-db',
    port: 1883,
    username: 'clientdb',
    password: 'clientdb',
    keepalive : 60
};

// Primitive MQTT
var client  = mqtt.connect(indirizzoBroker, opzioni); // connessione al broker
client.on('connect', mqtt_connect); // connetti al broker
client.on('reconnect', mqtt_reconnect);
client.on('error', mqtt_error);
client.on('message', mqtt_messsageReceived);
client.on('close', mqtt_close);

// Funzioni MQTT
function mqtt_connect() {
    console.log("Database connesso al broker");
    client.subscribe("/utenti/#", mqtt_subscribe); // sottoscrivi al topic /test/utenti/#

    // pubblico la lista dei sensori presenti nel database con retain
    getListaSensori().then(res => {
        client.publish("/sensori",res, {qos: 1, retain: true});
        console.log('Il messaggio (lista sensori):\n' + res + '\npubblicato sul topic: /sensori');
    });
}

function mqtt_subscribe(err, granted) {
    console.log("Subscribe eseguita");
    if (err) {console.log(err);}
}

function mqtt_reconnect(err) {
    console.log("Database riconnesso via MQTT");
    if (err) {console.log(err);}
	client  = mqtt.connect(indirizzoBroker, opzioni);
}

function mqtt_error(err) {
    console.log("Errore!");
    if (err) {console.log(err);}
}

function after_publish() {
    // dopo aver pubblicato non fare niente
}

// quando ricevo un messaggio dove sono iscritto
function mqtt_messsageReceived(topic, message) {
    console.log('Messaggio ricevuto da ' + topic + ' = ' + message);
    // Converto il messaggio ricevuto in una lista tracce
    var ltjson = JSON.parse(message);

    for (i in ltjson.tracce) {
        Promise.all([inserisciTraccia(topic.split('/')[2], ltjson.tracce[i]), getPosizioneTAG(ltjson.tracce[i].MAC_1), getPosizioneTAG(ltjson.tracce[i].MAC_2), getPosizioneTAG(ltjson.tracce[i].MAC_3)])
        .then(p => {
            /*
            console.log('Email: ' + topic.split('/')[2]);
            console.log('Datetime: ' + p[0].dateTime);
            console.log('TAG1 -> x: ' + p[1].x + ' y: ' + p[1].y);
            console.log('d1: ' + m.calcoloRssiMetri(p[0].potenza_1));
            console.log('TAG2 -> x: ' + p[2].x + ' y: ' + p[2].y);
            console.log('d2: ' + m.calcoloRssiMetri(p[0].potenza_2));
            console.log('TAG3 -> x: ' + p[3].x + ' y: ' + p[3].y);
            console.log('d3: ' + m.calcoloRssiMetri(p[0].potenza_3));

            var posizione = m.eseguiTrilaterazione(p[1].x, p[1].y, m.calcoloRssiMetri(p[0].potenza_1), p[2].x, p[2].y, m.calcoloRssiMetri(p[0].potenza_2), p[3].x, p[3].y, m.calcoloRssiMetri(p[0].potenza_3));
            console.log('Posizione -> x: ' + posizione.x + ' y: ' + posizione.y);
            */


            inserisciPosizione(topic.split('/')[2], p[0].dateTime, m.eseguiTrilaterazione(p[1].x, p[1].y, m.calcoloRssiMetri(kf.filter(p[0].potenza_1)), p[2].x, p[2].y, m.calcoloRssiMetri(kf.filter(p[0].potenza_2)), p[3].x, p[3].y, m.calcoloRssiMetri(kf.filter(p[0].potenza_3))));
            console.log('Inserita posizione di:' + topic.split('/')[2] + ' del: ' + p[0].dateTime);
        })
        .catch(err => {'Errore inserimento tracce'})
    }
}

function mqtt_close() {
	console.log("Chiudi connessione al database");
}

// Inserisce le tracce contenute nel messaggio nella tabella TRACCIA
function inserisciTraccia(email, traccia) {

    return mssql.connect(opzioniSQL).then(pool => {
        return pool.request().query("INSERT INTO traccia (Email_Utente,Datetime,MAC_1,Potenza_1,MAC_2,Potenza_2,MAC_3,Potenza_3) VALUES ('" + email +"','" + traccia.dateTime + "','" + traccia.MAC_1 + "'," + traccia.potenza_1 + ",'" + traccia.MAC_2 + "'," + traccia.potenza_2 + ",'" + traccia.MAC_3 + "'," + traccia.potenza_3 + ");")
    }).then(result => {
        return traccia;
    }).catch(err => {
        console.log('errore inserisciTraccia()');
    })
}

// Ritorno la posizione del tag passato come parametro
function getPosizioneTAG(mac) {
    var posizione = new Object();

    return mssql.connect(opzioniSQL).then(pool => {
        return pool.request().query("SELECT tag.Posizione_X as x, tag.Posizione_Y as y FROM TAG_BLE AS tag WHERE tag.Indirizzo_MAC ='" + mac + "';")
    }).then(result => {
        posizione.x = result[0].x;
        posizione.y = result[0].y
        return posizione;
    }).catch(err => {
        console.log('errore getPosizioneTAG');
    })
}

// // Ritorno la posizione del tag passato come parametro
// function getPosizioneTAG(mac) {
//     var posizione = new Object();

//     return mssql.connect(opzioniSQL).then(pool => {
//         return pool.request().query("SELECT tag.Posizione_X as x, tag.Posizione_Y as y FROM TAG_BLE AS tag WHERE tag.Indirizzo_MAC ='" + mac + "';")
//     }).then(result => {
//         posizione.x = result[0].x;
//         posizione.y = result[0].y
//         return posizione;
//     }).catch(err => {
//         console.log('errore');
//     })
// }

// Inserisco la posizione nella tabella POSIZIONE
function inserisciPosizione(email, datetime, posizione) {
    return mssql.connect(opzioniSQL).then(pool => {
        return pool.request().query("INSERT INTO Posizione (Email_Utente,Datetime,Posizione_X,Posizione_Y) VALUES ('" + email + "','" + datetime + "'," + posizione.x + "," + posizione.y + ");")
    }).then(result => {
        return posizione;
    }).catch(err => {
        console.log('errore inserisciPosizione');
    })
}

function getListaSensori() {
    
    // inizio a preparare la lista dei sensori
    var messaggio = '{"sensori": [';
    
    return mssql.connect(opzioniSQL).then(function() {
        var request = new mssql.Request(mssql);

        // seleziona tutti i tag ble registrati
        var stringRequest = "SELECT Indirizzo_MAC,Piano FROM TAG_BLE;";

        // scorrendo il resultset vado a formare il JSON da inviare ai dispositivi
        return request.query(stringRequest).then(function(result) {
            for (i in result)
                messaggio += '{"indirizzoMAC":"' + result[i].Indirizzo_MAC + '","piano":"' + result[i].Piano + '"}' + (i != result.length - 1 ? ',' : ']');
            messaggio += '}';
            return messaggio;
            mssql.close();
        })
        .catch(function (err) {
            console.log(err);
            mssql.close();
        });
    }).catch(function (err) {
        console.log(err);
    });
};

// // Per pubblicare un messaggio
// var messaggio = {
//     topic: '/test/sensori',
//     payload: 'lista dei miei bei sensori', // or a Buffer
//     qos: 1, // 0, 1, or 2
//     retain: true // or true
// };

// client.publish(messaggio, function() {
//     console.log('Inviato!');
// });