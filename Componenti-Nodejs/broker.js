// Dipendenze:
var mosca = require('mosca');
var ModuloAuth = require('./moduli/ModuloAutorizzazione');

var moscaSettings = {
    port: 1883,
    backend: ascoltatore,
    persistence: {
      factory: mosca.persistence.Mongo,
      url: 'mongodb://192.168.0.126:27017/mqtt'
    }
  };

var ascoltatore = {
    // Uso ascoltatore
    type: 'mongo',		
    url: 'mongodb://localhost:27017/mqtt',
    pubsubCollection: 'ascoltatori',
    mongo: {}
  };

// Devo istanziare una nuova connessione ogni volta che mi connetto al db
var db = new ModuloAuth();

// Imposta il server Mosca
var server = new mosca.Server(moscaSettings);

// Collegamento autenticazione e autorizzazione a mosca
server.authenticate = db.authenticate;
server.authorizePublish = db.authorizePublish;
server.authorizeSubscribe = db.authorizeSubscribe;

server.on('ready', setup);

// Lanciato quando il server è pronto
function setup() {
    console.log('Il broker è attivo e funzionante');
}

// Lanciato quando un client si connette
server.on('clientConnected', function(client) {
    console.log('Client connesso: ', client.id);
});

// Lanciato quando un client si disconnette
server.on('clientDisconnected', function(client) {
    console.log('Client disconnesso:', client.id);
});

// Lanciato quando un client pubblica
server.on('published', function(packet, client) {
    console.log('Pubblicato Messaggio: ', packet.payload);
});