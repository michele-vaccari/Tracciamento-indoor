// Dipendenze:
var mssql = require('mssql');

var sqlConfig = {
    server: "62.173.163.189",
    database: "EVOIndoor",
    user: 'michele',
    password: 'michele',
    port: 1433
}

// Costruttore:
function ModuloAutorizzazione() {
    // Istanzio una nuova connessione
}

// Autenticazione
ModuloAutorizzazione.prototype.authenticate = function(client, username, password, callback) {

    var autorizzato = false;
    
    console.log('Tentativo di accesso di username: ' + username + ' password: ' + password);

    return mssql.connect(sqlConfig).then(function() {
        var request = new mssql.Request(mssql);

        // conta gli utenti che hanno lo stesso username e password
        var stringRequest = "SELECT COUNT(*) AS presente FROM utente WHERE Email = '" + username + "' AND Password = '" + password + "';";
        // autorizzo se presente altrimenti rifiuto la connessione
        return request.query(stringRequest).then(function(result) {
            autorizzato = result[0].presente == 1;
            if (!autorizzato) {
                console.log(username + ' NON autorizzato!');
                return callback(null, autorizzato);
            }
            else {
                console.log(username + ' autorizzato.');
                client.user = username;
                return callback(null, autorizzato);
            }
            mssql.close();
        })
        .catch(function (err) {
            console.log(err);
            mssql.close();
        });
    }).catch(function (err) {
        console.log(err);
    });
}

// Ritorna true se il client può pubblicare in un topic
ModuloAutorizzazione.prototype.authorizePublish = function(client, topic, payload, callback) {

    // // va fatto il controllo sul database
    callback(null, true);
}

// Ritorna true se il client può sottoscriversi ad un topic
ModuloAutorizzazione.prototype.authorizeSubscribe = function(client, topic, callback) {

    var autorizzato = false;
    
    console.log('Verifica autorizzazione di sottoscrizione su topic: ' + topic + ' di: ' + client.user);

    // return mssql.connect(sqlConfig).then(function() {
    //     var request = new mssql.Request(mssql);
        
    //     // conta gli utenti che hanno lo stesso username e topic
    //     var stringRequest = " SELECT COUNT(*) AS presente " +
    //                         " FROM sottoscritto join topic as T on S.Nome_Topic = T.Nome " +
    //                         " WHERE S.Email_Utente = '" + client.username + "' AND S.Nome_Topic = '" + topic + "';";
        
    //     // autorizzo se presente altrimenti rifiuto la connessione
    //     return request.query(stringRequest).then(function(result) {
    //         autorizzato = result[0].presente == 1;
    //         if (!autorizzato) {
    //             console.log(client.username + ' NON autorizzato a sottoscrivere il topic: ' + topic);
    //             return callback(null, autorizzato);
    //         }
    //         else {
    //             console.log(client.username + ' autorizzato a sottoscrivere il topic: ' + topic);
    //             return callback(null, autorizzato);
    //         }
    //         mssql.close();
    //     })
    //     .catch(function (err) {
    //         console.log(err);
    //         mssql.close();
    //     });
    // }).catch(function (err) {
    //     console.log(err);
    // });

    // va fatto il controllo sul database
    callback(null, true);
}

module.exports = ModuloAutorizzazione;