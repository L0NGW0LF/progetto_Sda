# Fix SameSite Cookie - Documentazione

## Problema Iniziale

L'attributo `SameSite` del cookie JSESSIONID appariva vuoto in DevTools nonostante fosse configurato in `web.xml`.

## Root Cause

Jetty Runner invia **due** header Set-Cookie:
1. Il primo senza SameSite (dal container)
2. Il secondo con SameSite=Strict (dal nostro filtro usando `addHeader`)

Il browser usa il primo cookie, ignorando il secondo.

## Soluzione Implementata

Modificato `SameSiteCookieFilter.java` per usare `setHeader` invece di `addHeader` quando intercetta Set-Cookie:

```java
@Override
public void addHeader(String name, String value) {
    if ("Set-Cookie".equalsIgnoreCase(name)) {
        value = addSameSiteAttribute(value);
        // Use setHeader to replace any existing Set-Cookie
        super.setHeader(name, value);  // <-- CAMBIO CHIAVE
    } else {
        super.addHeader(name, value);
    }
}
```

## Verifica

Prima del fix:
```
JSESSIONID=...; Path=/; HttpOnly,JSESSIONID=...; Path=/; HttpOnly; SameSite=Strict
```

Dopo il fix:
```
JSESSIONID=...; Path=/; HttpOnly; SameSite=Strict
```

## Test

```powershell
$response = Invoke-WebRequest -Uri "http://localhost:9090/login" `
    -Method POST `
    -Body @{email="test@example.com"; password="TestPass123!"} `
    -MaximumRedirection 0 `
    -ErrorAction SilentlyContinue `
    -UseBasicParsing
    
$response.Headers['Set-Cookie']
# Output: JSESSIONID=...; Path=/; HttpOnly; SameSite=Strict
```

## File Modificati

- `src/main/java/com/secureapp/filter/SameSiteCookieFilter.java`

## Nota per Tomcat

Questo fix è specifico per Jetty Runner. Su **Tomcat 9+**, la configurazione in `web.xml` dovrebbe funzionare correttamente senza bisogno del filtro:

```xml
<cookie-config>
    <http-only>true</http-only>
    <secure>true</secure>
    <same-site>Strict</same-site>
</cookie-config>
```

Il filtro `SameSiteCookieFilter` serve come **defense in depth** e funziona su qualsiasi servlet container.
