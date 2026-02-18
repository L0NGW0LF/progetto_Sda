# SSL Configuration - Limitazioni di Jetty Runner

## Problema

Ho tentato di configurare SSL per localhost ma `jetty-runner` è uno strumento deprecated con supporto SSL molto limitato. Il tool è progettato principalmente per deployment HTTP semplici e non supporta adeguatamente le configurazioni SSL via XML.

## Limitazioni Identificate

1. **jetty-runner è deprecated**: Lo strumento stesso mostra warnings e non è più mantenuto attivamente.
2. **Supporto SSL limitato**: Non supporta configurazioni SSL complesse via file XML.
3. **Errori di ClassCast**: Quando si tenta di passare configurazioni XML, jetty-runner le interpreta erroneamente come context handlers.

## Alternative per HTTPS

### Opzione 1: Usare un Server Jetty Completo

Invece di `jetty-runner.jar`, installare e configurare Jetty standalone completo che supporta pienamente SSL.

### Opzione 2: Usare Apache Tomcat

Tomcat 9+ ha un eccellente supporto SSL e può essere configurato facilmente con:
```xml
<Connector port="8443" protocol="HTTP/1.1" SSLEnabled="true"
           maxThreads="150" scheme="https" secure="true"
           keystoreFile="keystore.p12" keystorePass="password"
           clientAuth="false" sslProtocol="TLS"/>
```

### Opzione 3: Testing Solo HTTP

Per lo sviluppo locale, puoi testare tutte le funzionalità di sicurezza tranne il flag `Secure` dei cookie:
- ✅ `HttpOnly` - funziona
- ✅ `SameSite=Strict` - funziona  
- ❌ `Secure` - richiede HTTPS

## File Generati (Conservati per Riferimento Futuro)

- `keystore.p12`: Certificato self-signed (password: `password`)
- `jetty-ssl.xml`: Configurazione SSL per Jetty (non compatibile con jetty-runner)

Questi file possono essere riutilizzati se in futuro deciderai di passare a Tomcat o Jetty standalone.

## Configurazione Attuale

Per ora, l'applicazione funziona in **HTTP** su `http://localhost:9090/` con:
- Cookie `HttpOnly`: ✅ Attivo
- Cookie `SameSite=Strict`: ✅ Attivo (grazie al filtro corretto)
- Cookie `Secure`: ❌ Disabilitato (richiede HTTPS)
