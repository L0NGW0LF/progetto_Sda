# Checklist Consegna Progetto - Sicurezza nelle Applicazioni

## 📦 Artefatti Software da Consegnare

### 1. File WAR
- [ ] `target/secure-web-app.war` presente e funzionante
- [ ] Dimensione ragionevole (~10-20 MB con dipendenze)
- [ ] Testato su Jetty/Tomcat

**Verifica**:
```powershell
Test-Path "target/secure-web-app.war"
# Output: True
```

### 2. Codice Sorgente
- [ ] Directory `src/` completa
- [ ] Tutti i file `.java` presenti
- [ ] File `.jsp` presenti
- [ ] `web.xml` configurato

**Struttura Minima Richiesta**:
```
src/
├── main/
│   ├── java/com/secureapp/
│   │   ├── dao/
│   │   │   ├── UserDAO.java ✓
│   │   │   └── FileDAO.java ✓
│   │   ├── filter/
│   │   │   ├── AuthFilter.java ✓
│   │   │   ├── SecurityHeadersFilter.java ✓
│   │   │   └── SameSiteCookieFilter.java ✓
│   │   ├── model/
│   │   │   ├── User.java ✓
│   │   │   └── FileModel.java ✓
│   │   ├── service/
│   │   │   └── ConcurrentUploadService.java ✓ (CRITICO RF3.8)
│   │   ├── servlet/
│   │   │   ├── LoginServlet.java ✓
│   │   │   ├── RegisterServlet.java ✓
│   │   │   ├── DashboardServlet.java ✓
│   │   │   ├── UploadServlet.java ✓
│   │   │   ├── FileContentServlet.java ✓
│   │   │   └── LogoutServlet.java ✓
│   │   └── util/
│   │       ├── DatabaseUtil.java ✓
│   │       └── ValidationUtil.java ✓
│   └── webapp/
│       ├── WEB-INF/
│       │   ├── web.xml ✓
│       │   └── views/*.jsp ✓
│       └── index.jsp ✓
└── pom.xml ✓
```

### 3. Configurazioni
- [ ] `pom.xml` con tutte le dipendenze (Servlet API, JSTL, H2, BCrypt, Tika, OWASP Encoder)
- [ ] `web.xml` con:
  - [ ] `<session-timeout>30</session-timeout>`
  - [ ] `<cookie-config>` (HttpOnly, Secure, SameSite)
  - [ ] Mapping servlets
  - [ ] Mapping filtri

### 4. Commenti Codice
Verificare che i file **critici per la sicurezza** abbiano commenti significativi:

- [ ] `ConcurrentUploadService.java` - Spiegazione lock e thread pool
- [ ] `SameSiteCookieFilter.java` - Perché è necessario
- [ ] `LoginServlet.java` - Session fixation protection
- [ ] `UserDAO.java` - BCrypt e PreparedStatements
- [ ] `UploadServlet.java` - Tika validation
- [ ] `FileContentServlet.java` - Output encoding

---

## 📄 Documentazione Tecnica (.docx)

### Sezione 5.1 - Analisi Statica

#### 5.1.1 Gestione Cookie
- [ ] Descrizione configurazione (`web.xml` + filtro custom)
- [ ] Snippet codice `SameSiteCookieFilter.java`
- [ ] Motivazione: Prevenzione XSS (HttpOnly) e CSRF (SameSite)

#### 5.1.2 Gestione Sessioni HTTP
- [ ] Descrizione session fixation protection
- [ ] Snippet `LoginServlet.java` (invalidate + new session)
- [ ] Snippet `AuthFilter.java`
- [ ] Motivazione: Prevenzione session hijacking

#### 5.1.3 SQL Injection
- [ ] Descrizione PreparedStatements
- [ ] Snippet da `UserDAO.java` o `FileDAO.java`
- [ ] Motivazione: Input mai concatenato in SQL

#### 5.1.4 Validazione Input
- [ ] Descrizione whitelist approach
- [ ] Snippet `ValidationUtil.java` (regex email/password)
- [ ] Motivazione: Defense in depth

#### 5.1.5 Caricamento File
- [ ] Descrizione validazione Apache Tika
- [ ] Snippet `UploadServlet.java` (extension check + Tika)
- [ ] Snippet `ConcurrentUploadService.java` (TOCTOU prevention)
- [ ] Motivazione: Prevenzione upload malware

#### 5.1.6 XSS
- [ ] Descrizione output encoding
- [ ] Snippet `FileContentServlet.java` (OWASP Encoder)
- [ ] Snippet JSP con `fn:escapeXml`
- [ ] Motivazione: Impedire esecuzione script utente

#### 5.1.7 Password
- [ ] Descrizione BCrypt
- [ ] Snippet `UserDAO.java` (hashpw + salt)
- [ ] Motivazione: Resistenza rainbow tables

#### 5.1.8 Programmazione Difensiva
- [ ] Descrizione scope, access modifiers
- [ ] Esempi da Model classes
- [ ] Motivazione: Principle of least privilege

#### 5.1.9 Concorrenza (CRITICO)
- [ ] Descrizione architettura thread pool
- [ ] Snippet `ConcurrentUploadService.java`:
  - [ ] ExecutorService
  - [ ] ReentrantLock
  - [ ] AtomicLong
- [ ] Diagramma di flusso (opzionale)
- [ ] Motivazione: Race conditions, TOCTOU

### Sezione 5.2 - Analisi Dinamica

#### 5.2.1 Test d'Uso (TU1 - TU10)
Per ogni test:
- [ ] Descrizione scenario
- [ ] Input fornito
- [ ] Comportamento atteso
- [ ] **Screenshot come evidenza**
- [ ] Risultato (PASS/FAIL)

**Test Critici** (da evidenziare):
- [ ] **TU2**: Screenshot cookie JSESSIONID con attributi HttpOnly/SameSite
- [ ] **TU5**: Screenshot database query che mostra metadata file
- [ ] **TU6**: Screenshot Content-Type text/plain
- [ ] **TU10**: Screenshot che dimostra upload concorrente senza race condition

#### 5.2.2 Test di Abuso (TA1 - TA8)
Per ogni test:
- [ ] Descrizione attacco
- [ ] Input malevolo fornito
- [ ] Comportamento atteso (blocco)
- [ ] **Screenshot come evidenza**
- [ ] Contromisura applicata (riferimento al codice)

**Test Critici**:
- [ ] **TA1**: Screenshot SQLi bloccato
- [ ] **TA4**: Screenshot Tika che blocca fake.txt
- [ ] **TA5**: Screenshot XSS non eseguito

---

## ✅ Requisiti di Qualità

### Codice
- [ ] **Nessuna vulnerabilità intenzionale** presente
- [ ] Codice compila senza errori (`mvn clean package`)
- [ ] Nessun warning critico
- [ ] Formattazione consistente

### Test
- [ ] Tutti i 10 test d'uso documentati
- [ ] Tutti gli 8 test di abuso documentati
- [ ] Ogni test ha almeno 1 screenshot
- [ ] Gli attacchi sono stati **realmente tentati** (non solo descritti)

### Documentazione
- [ ] Linguaggio chiaro e professionale
- [ ] Ogni misura di sicurezza è:
  - [ ] Descritta
  - [ ] Motivata
  - [ ] Collegata al codice
- [ ] Screenshot leggibili e pertinenti
- [ ] Coerenza tra documentazione e codice
- [ ] Nessun riferimento a implementazioni vulnerabili

---

## 🎯 Pre-Consegna: Test Finale

### 1. Rebuild Completo
```powershell
cd C:\Users\anton\Desktop\progetto_SdA
Remove-Item -Recurse -Force target
$env:JAVA_HOME = "C:\Users\anton\Desktop\progetto_SdA\tools\jdk"
.\tools\maven\bin\mvn.cmd clean package
```
Verifica output: `BUILD SUCCESS`

### 2. Deploy Test
```powershell
$env:JAVA_HOME = "C:\Users\anton\Desktop\progetto_SdA\tools\jdk"
& "$env:JAVA_HOME\bin\java.exe" -jar jetty-runner.jar --port 9090 target/secure-web-app.war
```
Verifica: http://localhost:9090/ carica correttamente

### 3. Test Rapido Funzionalità
- [ ] Registrazione nuovo utente funziona
- [ ] Login funziona
- [ ] Upload .txt funziona
- [ ] Visualizzazione file funziona
- [ ] Logout funziona

### 4. Test Rapido Sicurezza
- [ ] Upload .exe è bloccato
- [ ] SQL injection è bloccato
- [ ] Accesso senza login è bloccato

---

## 📦 Pacchetto Finale di Consegna

Creare una cartella con:

```
Cognome_Nome_Progetto_SdA/
├── src/                          (Codice sorgente completo)
├── target/
│   └── secure-web-app.war        (File deployable)
├── pom.xml
├── README.md                     (Istruzioni deployment)
├── Cognome_Nome_Relazione.docx   (Documentazione tecnica)
└── screenshot/                   (Cartella con tutte le immagini usate)
    ├── TU1_registrazione.png
    ├── TU2_cookie_devtools.png
    ├── ...
    └── TA8_file_accesso_negato.png
```

### Compressione Finale
```powershell
Compress-Archive -Path "C:\Users\anton\Desktop\progetto_SdA" -DestinationPath "C:\Users\anton\Desktop\Cognome_Nome_Progetto_SdA.zip"
```

---

## 📋 Checklist Finale Prima della Consegna

### Artefatti Software
- [ ] WAR file funzionante
- [ ] Codice sorgente leggibile e commentato
- [ ] Configurazioni complete

### Documentazione
- [ ] Tutte le sezioni 5.1.x compilate
- [ ] Tutti i test 5.2.x documentati con screenshot
- [ ] Riferimenti codice corretti
- [ ] Nessun errore di formattazione

### Qualità
- [ ] Ortografia e grammatica verificate
- [ ] Screenshot leggibili
- [ ] Codice testato e funzionante
- [ ] Nessuna contraddizione tra doc e codice

### Formato File
- [ ] Documentazione in formato .docx
- [ ] Archivio compresso (.zip)
- [ ] Dimensione ragionevole (<50 MB)
- [ ] Nome file: `Cognome_Nome_Progetto_SdA.zip`

---

## 🚀 Invio

**Modalità**: Secondo le istruzioni del docente (es. piattaforma Moodle, email, etc.)

**Oggetto Email** (se applicabile):
```
[Sicurezza Applicazioni] Consegna Progetto - Cognome Nome - Matricola 12345
```

**Corpo Email**:
```
Buongiorno Prof.,

Allego la consegna del progetto "Applicazione Web Sicura" per il corso di 
Sicurezza nelle Applicazioni (A.A. 2025/2026).

L'archivio contiene:
- Codice sorgente completo
- File WAR deployable
- Documentazione tecnica (Analisi Statica e Dinamica)

Cordiali saluti,
[Nome Cognome]
Matricola: [12345]
```

---

**Ultimo Controllo**: Scarica l'archivio appena inviato e verifica che sia integro e contenga tutti i file!
