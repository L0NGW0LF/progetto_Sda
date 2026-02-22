# Secure Web Application

**Corso**: Sicurezza nelle Applicazioni  
**Università**: Università degli Studi di Bari Aldo Moro  
**Anno Accademico**: 2025/2026

## Descrizione

Applicazione web Java EE che implementa un sistema sicuro di gestione utenti e condivisione file, sviluppata secondo i principi della secure software development e le linee guida CERT.

## Caratteristiche di Sicurezza Implementate

### Autenticazione e Gestione Sessioni (RF1, RF2, RF3, RF7)
- ✅ Password policy robusta (minimo 8 caratteri, maiuscole, minuscole, numeri, caratteri speciali)
- ✅ Hashing password con BCrypt e salt automatico
- ✅ Protezione da session fixation (rigenerazione session ID al login)
- ✅ Session timeout configurabile (30 minuti)
- ✅ Logout sicuro con invalidazione sessione server-side

### Cookie Sicuri (RF4, 3.1)
- ✅ HttpOnly flag (previene accesso JavaScript)
- ✅ Secure flag (solo HTTPS in produzione)
- ✅ SameSite=Strict (protezione CSRF)
- ✅ Nessun dato sensibile nei cookie

### Upload File Sicuro (RF5, 3.3)
- ✅ Validazione tipo file con Apache Tika (content-based)
- ✅ Whitelist estensioni (.txt only)
- ✅ Protezione TOCTOU tramite ConcurrentUploadService
- ✅ File salvati in directory non eseguibile

### Gestione Concorrenza (3.8)
- ✅ Thread pool esplicito (ExecutorService)
- ✅ Sincronizzazione con ReentrantLock
- ✅ AtomicLong per naming thread-safe
- ✅ Prevenzione race conditions e sovrascritture

### Protezione XSS (RF6, 3.4)
- ✅ Output encoding con OWASP Encoder
- ✅ Content-Type text/plain per visualizzazione file
- ✅ JSTL fn:escapeXml nelle JSP

### Protezione SQL Injection (3.4)
- ✅ Tutti i database access usano PreparedStatements
- ✅ Nessuna concatenazione SQL

### Programmazione Difensiva (3.7)
- ✅ Scope variabili ridotto al minimo
- ✅ Modificatori accesso appropriati (private/public)
- ✅ Information hiding
- ✅ Gestione errori senza esporre dettagli interni

## Requisiti

- **Java**: JDK 8+
- **Maven**: 3.6+
- **Application Server**: Tomcat 9+ o Jetty 9+
- **Database**: H2 (embedded, creato automaticamente)

## Struttura del Progetto

```
progetto_SdA/
├── src/
│   └── main/
│       ├── java/com/secureapp/
│       │   ├── dao/              # Data Access Objects
│       │   ├── filter/           # Security Filters
│       │   ├── model/            # Domain Models
│       │   ├── service/          # Business Services
│       │   ├── servlet/          # HTTP Servlets
│       │   └── util/             # Utilities
│       └── webapp/
│           ├── WEB-INF/
│           │   ├── views/        # JSP Views
│           │   └── web.xml       # Web Configuration
│           └── index.jsp
├── target/
│   └── secure-web-app.war        # Deployable WAR file
└── pom.xml
```

## Build e Deployment

### Build con Maven

```powershell
# Con tools locali
$env:JAVA_HOME = "C:\Users\anton\Desktop\progetto_SdA\tools\jdk"
.\tools\maven\bin\mvn.cmd clean package

# Output: target/secure-web-app.war
```

### Deploy su Tomcat

1. Copiare `target/secure-web-app.war` in `<TOMCAT_HOME>/webapps/`
2. Avviare Tomcat: `bin/startup.bat` (Windows) o `bin/startup.sh` (Linux)
3. Accedere a: `http://localhost:8080/secure-web-app/`

### Configurazione HTTPS (Produzione)

Per abilitare il flag `Secure` sui cookie in produzione:

1. Configurare Tomcat con certificato SSL
2. Modificare `web.xml`:
   ```xml
   <secure>true</secure>
   ```

## Utilizzo

### 1. Registrazione
- URL: `/register`
- Inserire email valida e password conforme alla policy
- La password viene hashata con BCrypt prima del salvataggio

### 2. Login
- URL: `/login`
- Autenticazione crea nuova sessione con ID rigenerato
- Cookie JSESSIONID impostato con attributi sicuri

### 3. Dashboard
- URL: `/dashboard`
- Visualizza lista file caricati da tutti gli utenti
- Form per upload nuovi file (.txt)

### 4. Upload File
- Solo file .txt accettati
- Validazione content-based con Apache Tika
- Processing concorrente thread-safe

### 5. Visualizzazione File
- Click su "View" nella tabella
- Content-Type: text/plain (no execution)
- Output encoded con OWASP Encoder

### 6. Logout
- URL: `/logout`
- Sessione invalidata server-side
- Cookie rimosso

## Sicurezza Implementata

### Prevenzione Attacchi

| Attacco | Contromisura |
|---------|--------------|
| SQL Injection | PreparedStatements |
| XSS | OWASP Encoder + Content-Type |
| CSRF | SameSite=Strict cookie |
| Session Hijacking | HttpOnly + Secure cookies |
| Session Fixation | Session ID regeneration |
| Malicious Upload | Apache Tika validation |
| TOCTOU | ReentrantLock synchronization |
| Password Cracking | BCrypt + salt |

## Database

### Schema

**users**
- id (INT, PK, AUTO_INCREMENT)
- email (VARCHAR, UNIQUE)
- password_hash (VARCHAR)
- created_at (TIMESTAMP)

**files**
- id (INT, PK, AUTO_INCREMENT)
- user_id (INT, FK → users.id)
- original_filename (VARCHAR)
- stored_filename (VARCHAR, UNIQUE)
- file_size (BIGINT)
- upload_date (TIMESTAMP)

### Posizione Database
`~/secure-app-db.mv.db` (H2 file-based)

### Posizione Upload
`~/secure-app-uploads/` (creata automaticamente)

## Testing

Vedere file `TESTING.md` per i test d'uso e d'abuso previsti dal progetto.

## Dipendenze Principali

- `javax.servlet-api` 4.0.1
- `jstl` 1.2
- `h2` 2.1.214
- `jbcrypt` 0.4
- `tika-core` 2.6.0
- `encoder` 1.2.3 (OWASP)

## Note di Sviluppo

- Codice commentato con focus su sicurezza
- Nessuna hardcoded key/password
- Error messages non rivelano dettagli interni
- Defensive copying per oggetti mutabili (Timestamp)
- Tutti i file in UTF-8

## Autore

Sviluppato per il corso di Sicurezza nelle Applicazioni  
CDL Magistrale in Sicurezza Informatica  
Università degli Studi di Bari Aldo Moro

## Licenza

Progetto didattico - A.A. 2025/2026
