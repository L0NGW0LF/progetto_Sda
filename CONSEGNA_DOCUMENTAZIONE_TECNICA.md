# Documentazione Tecnica - Progetto Sicurezza nelle Applicazioni
**Studente**: [Tuo Nome]
**Matricola**: [Tua Matricola]
**Anno Accademico**: 2025/2026

---

## 5.1 Analisi Statica

In questa sezione vengono descritte le scelte progettuali e implementative adottate per garantire la sicurezza dell'applicazione, in conformità con i requisiti del progetto.

### 5.1.1 Gestione dei Cookie
L'applicazione implementa una configurazione sicura dei cookie per prevenire intercettazioni e attacchi CSRF.

**Implementazione:**
La configurazione è centralizzata in `web.xml` e rafforzata programmaticamente per garantire la compatibilità con diversi container.
- **HttpOnly**: Impedisce l'accesso ai cookie tramite JavaScript (mitigazione XSS).
- **Secure**: Garantisce che i cookie siano inviati solo su connessioni cifrate (HTTPS) in produzione.
- **SameSite=Strict**: Impedisce l'invio di cookie in richieste cross-site (mitigazione CSRF).

**Codice Rilevante (`web.xml`):**
```xml
<cookie-config>
    <http-only>true</http-only>
    <secure>false</secure> <!-- Impostato a true in produzione -->
    <same-site>Strict</same-site>
</cookie-config>
```

Inoltre, è stato implementato un filtro specifico (`SameSiteCookieFilter.java`) per forzare l'attributo `SameSite` anche su container che non supportano pienamente la configurazione XML standard.

### 5.1.2 Gestione delle Sessioni HTTP
La gestione delle sessioni è progettata per prevenire il Session Fixation e limitare la finestra di attacco temporale.

**Implementazione:**
- **Prevenzione Session Fixation**: Al momento del login (`LoginServlet.java`), la sessione corrente viene invalidata e ne viene creata una nuova, garantendo un nuovo Session ID.
- **Session Timeout**: Configurato a 30 minuti in `web.xml`.
- **Autenticazione**: Il filtro `AuthFilter.java` protegge tutte le risorse sensibili (`/dashboard`, `/upload`, ecc.), verificando l'esistenza di una sessione valida.

**Codice Rilevante (`LoginServlet.java`):**
```java
// Invalidate old session to prevent session fixation
HttpSession oldSession = request.getSession(false);
if (oldSession != null) {
    oldSession.invalidate();
}
// Create new session
HttpSession newSession = request.getSession(true);
```

### 5.1.3 Protezione da SQL Injection
L'interazione con il database è protetta utilizzando sistematicamente i `PreparedStatement`.

**Implementazione:**
In tutte le classi DAO (`UserDAO.java`, `FileDAO.java`), i parametri di input non vengono mai concatenati alla stringa SQL, ma passati tramite placeholder (`?`). Questo delega al driver JDBC l'escaping corretto dei dati.

**Codice Rilevante (`UserDAO.java`):**
```java
String sql = "SELECT * FROM users WHERE email = ?";
try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
    pstmt.setString(1, email);
    // ...
}
```

### 5.1.4 Validazione e Sanitizzazione degli Input
Tutti gli input utente sono validati lato server prima di essere elaborati.

**Implementazione:**
La classe `ValidationUtil.java` centralizza le logiche di validazione:
- **Email**: Validazione formato regex.
- **Password**: Policy robusta (min 8 caratteri, maiuscole, numeri, simboli).
- **Filename**: Whitelist di caratteri permessi `[a-zA-Z0-9._-]`.

### 5.1.5 Caricamento Sicuro dei File
Il sistema di upload implementa controlli rigorosi per prevenire il caricamento di file malevoli (es. shell, malware).

**Implementazione:**
- **Validazione Estensione**: Solo file `.txt` sono permessi.
- **Validazione Contenuto (Tika)**: Utilizzo di Apache Tika per analizzare il magic number/header del file e confermare che sia effettivamente testo, indipendentemente dall'estensione dichiarata.
- **Prevenzione TOCTOU**: L'uso di lock nel servizio di upload garantisce che non ci siano race condition durante il controllo e la scrittura del file.

**Codice Rilevante (`UploadServlet.java`):**
```java
Tika tika = new Tika();
String detectedType = tika.detect(fileContent);
if (!isTextFile(detectedType)) {
    // Blocca upload
}
```

### 5.1.6 Prevenzione di XSS (Cross-Site Scripting)
L'applicazione mitiga il rischio XSS attraverso l'Output Encoding.

**Implementazione:**
- **Visualizzazione File**: Il contenuto dei file viene codificato usando OWASP Java Encoder (`Encode.forHtml()`) prima di essere inviato al browser. Inoltre, l'header `Content-Type` è forzato a `text/plain` e `X-Content-Type-Options: nosniff` è impostato.
- **JSP**: Utilizzo delle funzioni JSTL (`fn:escapeXml`) per visualizzare dati dinamici come i nomi dei file.

**Codice Rilevante (`FileContentServlet.java`):**
```java
String encodedContent = Encode.forHtml(fileContent);
response.getWriter().write(encodedContent);
```

### 5.1.7 Gestione Sicura delle Password
Le password non sono mai salvate in chiaro.

**Implementazione:**
Utilizzo dell'algoritmo **BCrypt** (`jbcrypt`), che implementa nativamente salt casuale e hashing lento adattivo (work factor), rendendo computazionalmente onerosi gli attacchi di forza bruta o rainbow table.

**Codice Rilevante (`UserDAO.java`):**
```java
String passwordHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
```

### 5.1.8 Programmazione Difensiva
Il codice segue i principi del minimo privilegio e dell'incapsulamento.
- **Scope**: Le variabili sono dichiarate nel minor scope possibile.
- **Access Modifiers**: Campi delle classi Model sono `private` e accessibili solo via getter/setter.
- **Information Hiding**: I messaggi di errore (es. login fallito) sono generici ("Invalid credentials") per non rivelare se un'email esiste nel sistema.

### 5.1.9 Concorrenza e Sincronizzazione (RF 3.8)
La gestione dei file supporta l'accesso concorrente sicuro.

**Implementazione:**
La classe `ConcurrentUploadService.java` utilizza:
- **ExecutorService**: Per gestire un pool di thread dedicato agli upload.
- **ReentrantLock**: Per sincronizzare l'accesso alla directory di upload e prevenire race condition (es. due utenti che caricano file con lo stesso nome nello stesso istante).
- **AtomicLong**: Per generare nomi di file univoci in modo thread-safe.

**Codice Rilevante (`ConcurrentUploadService.java`):**
```java
fileSystemLock.lock();
try {
    // Check esistenza e scrittura file
} finally {
    fileSystemLock.unlock();
}
```

---

## 5.2 Analisi Dinamica

Questa sezione documenta i test effettuati per verificare la sicurezza dell'applicazione.
*(Nota: inserire gli screenshot per ogni test come evidenza)*

### 5.2.1 Test d’Uso (Funzionalità Corrette)

| ID | Descrizione | Risultato Atteso | Esito |
|----|-------------|------------------|-------|
| **TU1** | Creazione account valido | Account creato, redirect a login | ✅ PASS |
| **TU2** | Login credenziali corrette | Accesso dashboard, nuova sessione | ✅ PASS |
| **TU3** | Login credenziali errate | Accesso negato, messaggio generico | ✅ PASS |
| **TU4** | Accesso riservato | Accesso consentito con sessione | ✅ PASS |
| **TU5** | Upload file .txt | File caricato e salvato | ✅ PASS |
| **TU6** | Visualizzazione file | Contenuto mostrato, script non eseguiti | ✅ PASS |
| **TU7** | Session Timeout | Redirect a login dopo 30 min inattività | ✅ PASS |
| **TU8** | Logout | Sessione invalidata, cookie rimosso | ✅ PASS |
| **TU9** | Accesso post-logout | Accesso negato, redirect a login | ✅ PASS |
| **TU10** | Upload Concorrente | Nessuna sovrascrittura o errore | ✅ PASS |

### 5.2.2 Test di Abuso (Attacchi Simulati)

| ID | Attacco | Comportamento Difensivo | Esito |
|----|---------|-------------------------|-------|
| **TA1** | SQL Injection in Login | `PreparedStatement` tratta input come stringa letterale. Login fallito. | ✅ BLOCCATO |
| **TA2** | Bypass Autenticazione | `AuthFilter` intercetta richiesta senza sessione. Redirect a login. | ✅ BLOCCATO |
| **TA3** | Upload estensione vietata (.exe) | Controllo estensione rifiuta il file. | ✅ BLOCCATO |
| **TA4** | Upload contenuto malevolo (fake .txt) | Apache Tika rileva header binario/exe. Upload rifiutato. | ✅ BLOCCATO |
| **TA5** | Stored XSS in file txt | Output Encoding e Content-Type text/plain impediscono esecuzione script. | ✅ MITIGATO |
| **TA6** | Accesso diretto a risorse | `AuthFilter` blocca accesso. | ✅ BLOCCATO |
| **TA7** | Riutilizzo sessione scaduta/logout | Session ID non più valido lato server. Accesso negato. | ✅ BLOCCATO |
| **TA8** | Esecuzione file caricati | File salvati fuori dalla webroot e serviti via Servlet protetta. | ✅ BLOCCATO |
