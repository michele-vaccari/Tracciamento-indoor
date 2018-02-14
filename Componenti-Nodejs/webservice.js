var express = require('express');
var mssql = require('mssql');
var bodyParser = require('body-parser');
var app = express();

app.use(bodyParser.json()); // for parsing application/json
app.use(bodyParser.urlencoded({ extended: true })); // for parsing application/x-www-form-urlencoded

app.use(function(req, res, next) {
    res.header("Access-Control-Allow-Origin", "*");
    res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
    res.header("Access-Control-Allow-Methods", "GET, POST, DELETE");
    next();
});

var opzioniSQL = {
    server: "62.173.163.189",
    database: "EVOIndoor",
    user: "michele",
    password: "michele",
    port: 1433
};

/* Mostra l'ultima posizione registrata */
app.post('/lastPosizione', function (req, res) {

    getLastPosizione().then(function(result) {
        console.log('Inviata rilevazione di: ' + result[0].Email_Utente + ' datetime: ' + result[0].Datetime);

        /* Crea l'oggetto JSON da inviare */
        var posizione = new Object();
        posizione.email = result[0].Email_Utente;
        posizione.datetime = result[0].Datetime;
        posizione.x = result[0].Posizione_X;
        posizione.y = result[0].Posizione_Y;
        
        /* Invio la posizione al client */
        res.end(JSON.stringify(posizione));
    });

})

/* Seleziona l'ultima posizione registrata */
var getLastPosizione = function() {
    
    return mssql.connect(opzioniSQL).then(pool => {
        return pool.request().query('select top 1 * from POSIZIONE order by Datetime desc')
    }).then(result => {
        return result;
    }).catch(err => {
        console.log('Errore -> getLastPosizione()');
    })
}


/* Mostra l'ultima posizione registrata */
app.post('/allLastPosizione', function (req, res) {
    
    getAllLastPosizione().then(function(result) {

        /* Crea l'oggetto JSON da inviare */
        var posizioni = new Array();

        for(i in result) {
            posizioni[i] = new Object();
            posizioni[i].email = result[i].Email_Utente;
            posizioni[i].datetime = result[i].Datetime;
            posizioni[i].x = result[i].Posizione_X;
            posizioni[i].y = result[i].Posizione_Y;
        }
        
        /* Invio la posizione al client */
        res.end(JSON.stringify(posizioni));
    });

})
    
/* Seleziona l'ultima posizione registrata */
var getAllLastPosizione = function() {
    
    return mssql.connect(opzioniSQL).then(pool => {
        return pool.request().query('select p.* from posizione p inner join (select Email_Utente, max(datetime) as MaxDatetime from posizione group by Email_Utente) ultimap on p.Email_Utente = ultimap.Email_Utente and p.datetime = ultimap.MaxDatetime');
    }).then(result => {
        return result;
    }).catch(err => {
        console.log('Errore -> getAllLastPosizione()');
    })
}


/* Mostra lista di tutti i tag */
app.get('/listTag', function (req, res) {
    
    getListTag().then(function(result) {
        console.log('Inviata lista Tag');

        /* Crea l'oggetto JSON da inviare */
        var tag = new Array();

        for(i in result) {
            tag[i] = new Object();
            tag[i].mac = result[i].Indirizzo_MAC;
            tag[i].piano = result[i].Piano;
            tag[i].x = result[i].Posizione_X;
            tag[i].y = result[i].Posizione_Y;
            tag[i].nome = result[i].Nome;
        }
        
        /* Invio la posizione al client */
        res.end(JSON.stringify(tag));
    });

})

/* Seleziona tutti i tag ble */
var getListTag = function() {
    
    return mssql.connect(opzioniSQL).then(pool => {
        return pool.request().query('select * from TAG_BLE')
    }).then(result => {
        return result;
    }).catch(err => {
        console.log('Errore -> getListTag()');
    })
}


/* Aggiungi nuovo tag */
app.post('/addTag', function (req, res) {
    console.log(req.body);
    addTag(req.body).then(function(result) {
        console.log('Inserito nuovo Tag');

        // pubblico la lista dei sensori presenti nel database con retain
        getListaSensori().then(res => {
            client.publish("/sensori",res, {qos: 1, retain: true});
            console.log('Il messaggio (lista sensori):\n' + res + '\npubblicato sul topic: /sensori');
        });

        res.end(JSON.stringify(result));
    });

})

/* Aggiungi un tag ble */
var addTag = function(tag) {

    return mssql.connect(opzioniSQL).then(pool => {
        return pool.request().query('insert into TAG_BLE (Indirizzo_MAC, Piano, Posizione_X, Posizione_Y, Nome) values (\'' + tag.mac + '\',' + tag.piano + ',' + tag.x + ',' + tag.y + ',\'' + tag.nome + '\')');
    }).then(result => {
        return result;
    }).catch(err => {
        console.log('Errore -> addTag()');
    })
}


/* Aggiorna un tag */
app.post('/updateTag', function (req, res) {
    
    updateTag(req.body).then(function(result) {
        console.log('Aggiornato un Tag');

        // pubblico la lista dei sensori presenti nel database con retain
        getListaSensori().then(res => {
            client.publish("/sensori",res, {qos: 1, retain: true});
            console.log('Il messaggio (lista sensori):\n' + res + '\npubblicato sul topic: /sensori');
        });

        res.end(JSON.stringify(result));
    });

})

/* Aggiorna un tag ble */
var updateTag = function(tag) {

    return mssql.connect(opzioniSQL).then(pool => {
        return pool.request().query('update TAG_BLE set Posizione_X=' + tag.x + ', Posizione_Y=' + tag.y + ' where Indirizzo_MAC=\'' + tag.mac + '\'');
    }).then(result => {
        return result;
    }).catch(err => {
        console.log('Errore -> updateTag()');
    })
}




/* Cancella un tag esistente */
app.delete('/deleteTag', function (req, res) {
    console.log(req.body);
    deleteTag(req.body.mac).then(function(result) {
        console.log('Cancellato Tag con MAC= ' + result);

        // pubblico la lista dei sensori presenti nel database con retain
        getListaSensori().then(res => {
            client.publish("/sensori",res, {qos: 1, retain: true});
            console.log('Il messaggio (lista sensori):\n' + res + '\npubblicato sul topic: /sensori');
        });

        res.end(JSON.stringify(result));
    });

})

/* Cancella un tag ble */
var deleteTag = function(mac) {

    return mssql.connect(opzioniSQL).then(pool => {
        return pool.request().query('delete TAG_BLE where Indirizzo_MAC=\'' + mac + '\';');
    }).then(result => {
        return mac;
    }).catch(err => {
        console.log('Errore -> deleteTag()');
    })
}

/* Avvio del server sulla porta 8081 */
var server = app.listen(8081, function () {

  var host = server.address().address
  var port = server.address().port

  console.log("Server in ascolto all'indirizzo http://%s:%s", host, port)

})

////////////////// GESTIONE CONNESSIONE MQTT //////////////

var mqtt = require('mqtt');
var indirizzoBroker = 'mqtt://192.168.0.126';

var opzioni = {
    clientId: 'webservice',
    port: 1883,
    username: 'webservice',
    password: 'webservice',
    keepalive : 60
};

var client  = mqtt.connect(indirizzoBroker, opzioni); // connessione al broker

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

////////////////// GESTIONE CONNESSIONE MQTT //////////////