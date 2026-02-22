# Guida Screenshot per Documentazione Tecnica

Questa guida elenca **esattamente** quali screenshot catturare per documentare ciascun test nella sezione 5.2 della relazione.

## Setup Iniziale

1. **Pulisci il Database**: 
   - Elimina `~/secure-app-db.mv.db` per partire da zero
   - Al riavvio dell'app, il database sarà rigenerato

2. **Browser DevTools**:
   - Apri sempre DevTools (F12)
   - Tieni aperta la tab "Network" per monitorare le richieste
   - Tieni aperta "Application → Cookies" per verificare cookie

3. **Risoluzione Screenshot**:
   - Assicurati che gli screenshot siano leggibili
   - Evidenzia le parti rilevanti con rettangoli rossi se necessario

---

## 5.2.1 Test d'Uso (TU1 - TU10)

### TU1 - Creazione Account Valido

**Step**:
1. Vai su http://localhost:9090/
2. Click "Register"
3. Inserisci: `test1@example.com` / `SecurePass123!`
4. Click "Register"

**Screenshot Necessari**:
- **Screenshot 1**: Form di registrazione compilato
- **Screenshot 2**: Messaggio di successo o redirect a login
- **Screenshot 3**: Database - Query `SELECT * FROM users WHERE email='test1@example.com'`
  - Mostra che `password_hash` inizia con `$2a$` (BCrypt)

### TU2 - Login Credenziali Corrette

**Step**:
1. Login con `test1@example.com` / `SecurePass123!`

**Screenshot Necessari**:
- **Screenshot 1**: Dashboard dopo login
- **Screenshot 2**: DevTools → Application → Cookies
  - Mostra JSESSIONID con `HttpOnly ✓` e `SameSite: Strict`
- **Screenshot 3**: DevTools → Network → Richiesta POST /login → Response Headers
  - Mostra header `Set-Cookie` con tutti gli attributi

### TU3 - Login Credenziali Errate

**Step**:
1. Logout
2. Tentare login con `test1@example.com` / `WrongPassword`

**Screenshot Necessari**:
- **Screenshot 1**: Messaggio di errore generico "Invalid credentials"
  - **Importante**: Il messaggio NON deve rivelare se l'email esiste

### TU4 - Accesso Area Riservata con Sessione Valida

**Step**:
1. Login valido
2. Navigare direttamente a http://localhost:9090/dashboard

**Screenshot Necessari**:
- **Screenshot 1**: Dashboard accessibile (mostra che non c'è redirect)

### TU5 - Caricamento File .txt Valido

**Step**:
1. Creare file `test.txt` con contenuto "Hello, World!"
2. Dashboard → Upload file
3. Selezionare `test.txt` e caricare

**Screenshot Necessari**:
- **Screenshot 1**: Messaggio successo upload
- **Screenshot 2**: Tabella file con il nuovo file elencato
- **Screenshot 3**: Database - Query `SELECT * FROM files`
  - Mostra metadata del file (original_filename, stored_filename, user_id)
- **Screenshot 4**: File system `~/secure-app-uploads/`
  - Mostra che il file è stato salvato con nome univoco

### TU6 - Visualizzazione Sicura Contenuti

**Step**:
1. Click "View" sul file caricato

**Screenshot Necessari**:
- **Screenshot 1**: Contenuto mostrato come testo
- **Screenshot 2**: DevTools → Network → Response Headers della richiesta `/file-content`
  - Mostra `Content-Type: text/plain`
  - Mostra `X-Content-Type-Options: nosniff`

### TU7 - Session Timeout

**Opzione A (Modifica Temporanea)**:
1. Ferma server
2. Modifica `web.xml`: `<session-timeout>1</session-timeout>`
3. Riavvia server
4. Login
5. Aspetta 2 minuti
6. Tenta accesso a `/dashboard`

**Opzione B (Screenshot Concettuale)**:
- Screenshot del codice `web.xml` che mostra `<session-timeout>30</session-timeout>`

**Screenshot Necessari**:
- **Screenshot 1**: Redirect a `/login?error=session_expired`

### TU8 - Logout Corretto

**Step**:
1. Login
2. Click "Logout"

**Screenshot Necessari**:
- **Screenshot 1**: Redirect a login con messaggio "logout successful"
- **Screenshot 2**: DevTools → Application → Cookies
  - Mostra che JSESSIONID è stato rimosso (o tabella vuota)

### TU9 - Accesso Post-Logout Negato

**Step**:
1. Dopo logout, click pulsante "Indietro" del browser
2. O naviga direttamente a http://localhost:9090/dashboard

**Screenshot Necessari**:
- **Screenshot 1**: Redirect a `/login?error=session_expired`

### TU10 - Caricamento Concorrente

**Setup**:
1. Aprire 2 finestre browser (o 2 tab in incognito separate)
2. Login con 2 account diversi (crea `user1@example.com` e `user2@example.com`)
3. Preparare 2 file: `file1.txt` e `file2.txt`

**Step**:
1. In entrambe le finestre, selezionare il rispettivo file
2. Click "Upload" **quasi simultaneamente** (entro 1-2 secondi)

**Screenshot Necessari**:
- **Screenshot 1**: Entrambi i dashboard mostrano successo upload
- **Screenshot 2**: Database - Query `SELECT * FROM files ORDER BY upload_date DESC LIMIT 2`
  - Mostra che `stored_filename` sono DIVERSI (es: `file_123.txt` e `file_124.txt`)
  - I timestamp sono molto vicini (pochi secondi di differenza)
- **Screenshot 3**: File system `~/secure-app-uploads/`
  - Mostra entrambi i file fisici presenti

---

## 5.2.2 Test di Abuso (TA1 - TA8)

### TA1 - SQL Injection nel Login

**Step**:
1. Login form
2. Email: `admin' OR '1'='1`
3. Password: `anything`
4. Submit

**Screenshot Necessari**:
- **Screenshot 1**: Form con payload SQL injection visibile
- **Screenshot 2**: Messaggio "Invalid credentials" (attacco fallito)
- **Screenshot 3**: Log server (se disponibile) che non mostra errori SQL

**Codice da Citare nella Relazione**:
```java
// UserDAO.java - Prepared Statement previene SQL Injection
String sql = "SELECT * FROM users WHERE email = ?";
pstmt.setString(1, email); // Input trattato come literal string
```

### TA2 - Bypass Autenticazione

**Step**:
1. **Senza** fare login, navigare direttamente a:
   - http://localhost:9090/dashboard
   - http://localhost:9090/upload
   - http://localhost:9090/file-content?file=file_1.txt

**Screenshot Necessari**:
- **Screenshot 1**: URL bar che mostra il tentativo di accesso diretto
- **Screenshot 2**: Redirect a `/login?error=session_expired` per ogni risorsa

### TA3 - Upload Estensione Vietata

**Step**:
1. Login
2. Tentare upload di file `.exe`, `.pdf`, `.jpg`
3. (Su Windows, puoi semplicemente rinominare un file qualsiasi)

**Screenshot Necessari**:
- **Screenshot 1**: Form upload con file `.exe` selezionato
- **Screenshot 2**: Messaggio errore "Only .txt files are allowed"

### TA4 - Upload Contenuto Malevolo (Fake Extension)

**Setup**:
1. Prendi un file binario (es. notepad.exe)
2. Rinominalo in `fake.txt`

**Step**:
1. Tenta upload di `fake.txt`

**Screenshot Necessari**:
- **Screenshot 1**: File "fake.txt" nel file selector
- **Screenshot 2**: Messaggio errore "File content validation failed"

**Codice da Citare**:
```java
// UploadServlet.java - Apache Tika content detection
String detectedType = tika.detect(fileContent);
if (!isTextFile(detectedType)) {
    // Blocca upload indipendentemente dall'estensione
}
```

### TA5 - Stored XSS

**Setup**:
1. Creare file `xss.txt` con contenuto:
```html
<script>alert('XSS')</script>
<img src=x onerror=alert('XSS')>
```

**Step**:
1. Upload `xss.txt`
2. Click "View"

**Screenshot Necessari**:
- **Screenshot 1**: Contenuto file mostrato come TESTO (script visibile ma non eseguito)
  - **Critico**: NON deve apparire un alert popup
- **Screenshot 2**: View Source della pagina che mostra `&lt;script&gt;` (encoding)
- **Screenshot 3**: DevTools → Elements
  - Il contenuto è dentro un tag `<pre>` o `<div>` ma NON è interpretato come HTML

### TA6 - Accesso Risorse Senza Sessione

**Step**:
1. Browser in modalità incognito (senza login)
2. Tentare accesso a http://localhost:9090/dashboard

**Screenshot Necessari**:
- **Screenshot 1**: Redirect a login

### TA7 - Riutilizzo Sessione Scaduta

**Step**:
1. Login
2. DevTools → Application → Cookies → Copia valore JSESSIONID
3. Logout
4. DevTools → Application → Cookies → Crea manualmente cookie JSESSIONID con il vecchio valore
5. Tenta accesso a /dashboard

**Screenshot Necessari**:
- **Screenshot 1**: DevTools con cookie vecchio reimpostato manualmente
- **Screenshot 2**: Redirect a login (sessione non valida lato server)

### TA8 - Esecuzione File Caricati

**Step**:
1. Caricare file `test.txt`
2. Notare il nome stored (es. `file_123.txt`)
3. Tentare accesso diretto: http://localhost:9090/file_123.txt
4. Tentare: http://localhost:9090/uploads/file_123.txt

**Screenshot Necessari**:
- **Screenshot 1**: Errore 404 Not Found
  - Dimostra che i file NON sono accessibili via URL diretto (fuori webroot)

**Spiegazione da Includere**: I file sono salvati in `~/secure-app-uploads` (fuori da `webapp/`) e serviti SOLO tramite `FileContentServlet` che richiede autenticazione.

---

## Checklist Finale Screenshot

Prima di completare la relazione, verifica di avere:

### Test d'Uso (10 test)
- [ ] TU1 - 3 screenshot
- [ ] TU2 - 3 screenshot  
- [ ] TU3 - 1 screenshot
- [ ] TU4 - 1 screenshot
- [ ] TU5 - 4 screenshot
- [ ] TU6 - 2 screenshot
- [ ] TU7 - 1 screenshot
- [ ] TU8 - 2 screenshot
- [ ] TU9 - 1 screenshot
- [ ] TU10 - 3 screenshot

### Test di Abuso (8 test)
- [ ] TA1 - 3 screenshot
- [ ] TA2 - 2 screenshot
- [ ] TA3 - 2 screenshot
- [ ] TA4 - 2 screenshot
- [ ] TA5 - 3 screenshot
- [ ] TA6 - 1 screenshot
- [ ] TA7 - 2 screenshot
- [ ] TA8 - 1 screenshot

**Totale**: ~30-35 screenshot richiesti
