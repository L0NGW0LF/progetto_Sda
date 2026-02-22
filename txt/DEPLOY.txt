# Guida Rapida - Deployment e Test

**Applicazione**: Secure Web Application  
**Corso**: Sicurezza nelle Applicazioni  
**Valutazione**: Progetto A.A. 2025/2026

---

## 📦 Deliverables

### File WAR (Pronto per Deploy)
**Posizione**: `target/secure-web-app.war`

### Documentazione
- `README.md` - Descrizione completa del progetto
- `TESTING.md` - Guida ai test (TU1-TU10, TA1-TA8)
- `walkthrough.md` - Walkthrough tecnico dell'implementazione

---

## 🚀 Deploy Rapido su Tomcat

### Prerequisiti
- **Tomcat 9+** (o qualsiasi servlet container compatibile con Servlet API 4.0)
- **Java 8+**

### Passi

1. **Copiare il WAR**:
   ```bash
   cp target/secure-web-app.war $TOMCAT_HOME/webapps/
   ```

2. **Avviare Tomcat**:
   ```bash
   # Linux/Mac
   $TOMCAT_HOME/bin/startup.sh
   
   # Windows
   %TOMCAT_HOME%\bin\startup.bat
   ```

3. **Accedere all'Applicazione**:
   ```
   http://localhost:8080/secure-web-app/
   ```

4. **Primo Utilizzo**:
   - Cliccare su "Register"
   - Creare account con password robusta (es: `TestPass123!`)
   - Effettuare login
   - Testare upload file `.txt`

---

## 🧪 Test Rapidi di Verifica

### Test 1: Password Policy (RF1)
```
Email: test@uniba.it
Password: weak       ❌ Rifiutata
Password: Test123!   ✅ Accettata
```

### Test 2: BCrypt Hashing (3.6)
**Query Database**:
```bash
# Accedere a H2 Console
URL: jdbc:h2:~/secure-app-db
User: sa
Password: (vuoto)

# Verificare hash
SELECT email, password_hash FROM users;
```
**Output atteso**: `$2a$12$...` (BCrypt hash)

### Test 3: Cookie Sicuri (RF4, 3.1)
1. Effettuare login
2. Aprire **DevTools** → **Application** → **Cookies**
3. Verificare attributi `JSESSIONID`:
   - ✅ `HttpOnly`
   - ✅ `SameSite: Strict`

### Test 4: SQL Injection (TA1)
```
Email: admin' OR '1'='1
Password: anything
```
**Risultato**: Login fallito (PreparedStatements bloccano injection)

### Test 5: Upload File Malevolo (TA4)
1. Rinominare un `.exe` in `.txt`
2. Tentare upload
3. **Risultato**: Upload rifiutato (Apache Tika rileva tipo reale)

### Test 6: XSS (TA5)
1. Creare file `xss.txt`:
   ```html
   <script>alert('XSS')</script>
   ```
2. Caricare file
3. Cliccare "View"
4. **Risultato**: Script mostrato come testo, non eseguito

### Test 7: Session Timeout (TU7)
1. Effettuare login
2. Attendere 31 minuti (o modificare `web.xml` per test rapido)
3. Tentare accesso a `/dashboard`
4. **Risultato**: Redirect a login con errore "session expired"

### Test 8: Concorrenza (TU10)
1. Aprire 2 tab del browser
2. Login con 2 account diversi
3. Caricare file simultaneamente
4. **Risultato**: Entrambi caricati, nomi univoci (nessuna sovrascrittura)

---

## 🔍 Verifica Implementazione Sicurezza

### Checklist Rapida

| Requisito | Verifica | Tool |
|-----------|----------|------|
| **RF1** - Password Policy | Registrazione con password debole rifiutata | Browser |
| **RF2** - Session Fixation | Session ID diverso prima/dopo login | DevTools Network |
| **RF4** - Cookie Sicuri | HttpOnly, SameSite presenti | DevTools Cookies |
| **3.4** - SQL Injection | `admin' OR '1'='1` fallisce | Login form |
| **3.3** - File Upload | `.exe` rinominato rifiutato | Upload form |
| **3.6** - BCrypt | Hash inizia con `$2a$12$` | H2 Console |
| **3.8** - Concorrenza | Upload simultanei OK | 2 browser tabs |
| **RF6** - XSS | Script non eseguito | View file |

---

## 📂 Struttura Codice Sorgente

```
src/main/java/com/secureapp/
├── dao/
│   ├── UserDAO.java           ★ BCrypt, PreparedStatements
│   └── FileDAO.java           ★ PreparedStatements
├── filter/
│   ├── AuthFilter.java        ★ Session validation
│   └── SecurityHeadersFilter.java ★ Security headers
├── model/
│   ├── User.java
│   └── FileModel.java
├── service/
│   └── ConcurrentUploadService.java ★★ ReentrantLock, ExecutorService
├── servlet/
│   ├── LoginServlet.java      ★ Session fixation protection
│   ├── RegisterServlet.java   ★ Password policy
│   ├── UploadServlet.java     ★ Apache Tika validation
│   └── FileContentServlet.java ★ OWASP Encoder
└── util/
    ├── DatabaseUtil.java      ★ DB initialization
    └── ValidationUtil.java    ★ Input validation
```

**★** = Codice con security focus  
**★★** = Requisito critico RF3.8 (concorrenza esplicita)

---

## 🎯 Punti Chiave dell'Implementazione

### 1. Autenticazione Sicura
- **BCrypt** con salt automatico e work factor 12
- **Session fixation protection**: rigenerazione ID al login
- **Timeout**: 30 minuti configurabile

### 2. Protezione Injection
- **Tutti i query** usano `PreparedStatement`
- **Nessuna concatenazione** SQL
- **Output encoding** con OWASP Encoder

### 3. Upload Sicuro
- **Content-based validation** (Apache Tika)
- **Whitelist** estensioni (`.txt` only)
- **Storage fuori webroot** (`~/secure-app-uploads/`)

### 4. Gestione Concorrenza (RF3.8)
```java
// Thread pool
ExecutorService executorService = Executors.newFixedThreadPool(5);

// Naming thread-safe
AtomicLong fileCounter = new AtomicLong();

// Sezione critica protetta
ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    // Operazioni file system
} finally {
    lock.unlock();
}
```

### 5. Cookie Sicuri
```xml
<cookie-config>
    <http-only>true</http-only>
    <secure>false</secure>  <!-- true in produzione con HTTPS -->
    <same-site>Strict</same-site>
</cookie-config>
```

---

## 🛠️ Database (H2)

### Accesso Console H2
```
URL: jdbc:h2:~/secure-app-db
Driver: org.h2.Driver
User: sa
Password: (vuoto)
```

### Query Utili
```sql
-- Verificare utenti registrati
SELECT id, email, created_at FROM users;

-- Verificare hash BCrypt
SELECT email, LEFT(password_hash, 10) as hash_prefix FROM users;

-- Verificare file caricati
SELECT user_id, original_filename, stored_filename, upload_date FROM files;

-- Contare file per utente
SELECT user_id, COUNT(*) as file_count FROM files GROUP BY user_id;
```

---

## 📊 Matrice Conformità Requisiti

| ID | Requisito | Implementato | File Chiave |
|----|-----------|--------------|-------------|
| RF1 | Creazione account robusta | ✅ | `RegisterServlet.java`, `ValidationUtil.java` |
| RF2 | Autenticazione sicura | ✅ | `LoginServlet.java`, `UserDAO.java` |
| RF3 | Gestione sessione | ✅ | `AuthFilter.java`, `web.xml` |
| RF4 | Cookie sicuri | ✅ | `web.xml`, `LoginServlet.java` |
| RF5 | Upload controllato | ✅ | `UploadServlet.java` |
| RF6 | Visualizzazione sicura | ✅ | `FileContentServlet.java` |
| RF7 | Logout sicuro | ✅ | `LogoutServlet.java` |
| 3.1 | Gestione cookie | ✅ | `web.xml` |
| 3.2 | Gestione sessione HTTP | ✅ | `LoginServlet.java` |
| 3.3 | Caricamento sicuro file | ✅ | `UploadServlet.java` + Tika |
| 3.4 | Protezione injection | ✅ | Tutti i DAO + `FileContentServlet.java` |
| 3.6 | Gestione credenziali | ✅ | `UserDAO.java` + BCrypt |
| 3.7 | Programmazione difensiva | ✅ | Tutti i file |
| 3.8 | Concorrenza | ✅ | `ConcurrentUploadService.java` |

---

## 📝 Note per la Valutazione

### Analisi Statica
Vedere commenti dettagliati nel codice sorgente per:
- Scelte di sicurezza
- Motivazioni tecniche
- Riferimenti a linee guida CERT

### Analisi Dinamica
La guida `TESTING.md` contiene:
- 10 test d'uso (TU1-TU10)
- 8 test di abuso (TA1-TA8)
- Comportamenti attesi
- Comandi di verifica

### Documentazione Tecnica
- **README.md**: Panoramica progetto e deployment
- **walkthrough.md**: Walkthrough completo implementazione
- Codice commentato con focus su sicurezza

---

## 🔄 Ricompilazione (se necessario)

```powershell
# Windows (usando tools locali)
$env:JAVA_HOME = "C:\Users\anton\Desktop\progetto_SdA\tools\jdk"
cd C:\Users\anton\Desktop\progetto_SdA
.\tools\maven\bin\mvn.cmd clean package

# Output: target/secure-web-app.war
```

```bash
# Linux/Mac (con Maven installato)
cd /path/to/progetto_SdA
mvn clean package

# Output: target/secure-web-app.war
```

---

## ❓ Troubleshooting

### Problema: Errore "JAVA_HOME not set"
**Soluzione**: Impostare variabile ambiente prima di eseguire Maven
```powershell
$env:JAVA_HOME = "path\to\jdk"
```

### Problema: Database locked
**Soluzione**: Chiudere tutte le istanze dell'applicazione e cancellare lock file
```bash
rm ~/secure-app-db.lock.db
```

### Problema: Upload directory permission denied
**Soluzione**: Verificare permessi su `~/secure-app-uploads/`
```bash
chmod 755 ~/secure-app-uploads/
```

---

## 📞 Struttura Consegna

```
progetto_SdA/
├── target/
│   └── secure-web-app.war       ★ FILE PRINCIPALE DI CONSEGNA
├── src/                          ★ CODICE SORGENTE
├── README.md                     ★ DOCUMENTAZIONE PRINCIPALE
├── TESTING.md                    ★ GUIDA AI TEST
├── DEPLOY.md                     ★ QUESTA GUIDA
└── pom.xml                       ★ CONFIGURAZIONE MAVEN
```

---

**Applicazione sviluppata secondo le specifiche del corso "Sicurezza nelle Applicazioni"**  
**CDL Magistrale in Sicurezza Informatica - Università degli Studi di Bari Aldo Moro**  
**A.A. 2025/2026**
