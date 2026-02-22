# Testing Guide - Secure Web Application

Questa guida documenta i test richiesti dalla specifica del progetto (sezione 5.2).

## Test d'Uso (Funzionalità Corrette)

### TU1 – Creazione account con credenziali valide

**Input:**
- Email: `test@example.com`
- Password: `SecurePass123!`

**Comportamento Atteso:**
- Account creato con successo
- Password hashata con BCrypt nel database
- Redirect a login con messaggio "Registration successful"

**Verifica:**
```sql
SELECT email, password_hash FROM users WHERE email = 'test@example.com';
-- password_hash dovrebbe iniziare con $2a$ (BCrypt hash)
```

---

### TU2 – Login con credenziali corrette

**Input:**
- Email: `test@example.com`
- Password: `SecurePass123!`

**Comportamento Atteso:**
- Autenticazione riuscita
- Nuova sessione creata con ID rigenerato
- Cookie JSESSIONID impostato con HttpOnly, SameSite=Strict
- Redirect a `/dashboard`

**Verifica:**
- Ispezionare cookie in Browser DevTools → Application → Cookies
- Verificare attributi: `HttpOnly`, `SameSite=Strict`

---

### TU3 – Login con credenziali errate

**Input:**
- Email: `test@example.com`
- Password: `WrongPassword`

**Comportamento Atteso:**
- Autenticazione fallita
- Messaggio generico: "Invalid credentials"
- Nessuna informazione se l'email esiste o meno (previene user enumeration)

---

### TU4 – Accesso ad area riservata con sessione valida

**Prerequisito:** Utente autenticato

**Input:**
- Navigare a `/dashboard`

**Comportamento Atteso:**
- Accesso consentito
- Visualizzazione dashboard con lista file

---

### TU5 – Caricamento contenuto testuale valido (.txt)

**Prerequisito:** Utente autenticato

**Input:**
- File: `test.txt` con contenuto "Hello, World!"

**Comportamento Atteso:**
- Upload completato con successo
- File salvato in `~/secure-app-uploads/file_<timestamp>.txt`
- Metadata salvato nel database
- Messaggio: "File uploaded successfully!"

**Verifica:**
```sql
SELECT original_filename, stored_filename, file_size FROM files;
```

---

### TU6 – Visualizzazione sicura dei contenuti caricati

**Prerequisito:** File caricato

**Input:**
- Click su "View" nella tabella files

**Comportamento Atteso:**
- Nuova pagina con Content-Type: `text/plain`
- Contenuto visualizzato ma non eseguito
- Output encoded (anche se contiene `<script>`)

---

### TU7 – Scadenza della sessione (timeout)

**Prerequisito:** Utente autenticato

**Input:**
- Attendere 31 minuti senza attività

**Comportamento Atteso:**
- Sessione scaduta
- Accesso a `/dashboard` rediretto a `/login?error=session_expired`

**Test Rapido:**
Modificare temporaneamente `web.xml`:
```xml
<session-timeout>1</session-timeout>  <!-- 1 minuto -->
```

---

### TU8 – Logout corretto

**Prerequisito:** Utente autenticato

**Input:**
- Click su "Logout"

**Comportamento Atteso:**
- Sessione invalidata server-side
- Cookie JSESSIONID rimosso
- Redirect a `/login?logout=true`

---

### TU9 – Tentativo di accesso post-logout (negato)

**Prerequisito:** Appena effettuato logout

**Input:**
- Tentare di accedere a `/dashboard` usando il pulsante "Indietro" del browser

**Comportamento Atteso:**
- Accesso negato
- Redirect a `/login?error=session_expired`

---

### TU10 – Caricamento concorrente di contenuti testuali

**Prerequisito:** 2+ utenti autenticati in tab/browser diversi

**Input:**
- User1 carica `file1.txt` contemporaneamente a User2 che carica `file2.txt`

**Comportamento Atteso:**
- Entrambi i file caricati correttamente
- Nessuna sovrascrittura
- `stored_filename` univoci (grazie ad AtomicLong)
- Database consistente

**Verifica:**
```sql
SELECT stored_filename FROM files ORDER BY upload_date DESC LIMIT 2;
-- Devono essere entrambi presenti con nomi diversi
```

**Test Automatizzato (Opzionale):**
```java
// Simulare upload concorrente con thread
ExecutorService executor = Executors.newFixedThreadPool(5);
for (int i = 0; i < 10; i++) {
    executor.submit(() -> {
        // Effettuare upload
    });
}
```

---

## Test di Abuso (Attacchi Simulati)

### TA1 – Tentativo di SQL Injection nel campo e-mail del login

**Input:**
- Email: `admin' OR '1'='1`
- Password: `anything`

**Comportamento Atteso:**
- Autenticazione fallita
- Messaggio: "Invalid credentials"
- PreparedStatement previene SQL injection

**Verifica Log:**
Nessun errore SQL nel log del server

---

### TA2 – Tentativo di bypass dell'autenticazione

**Input:**
- Accedere direttamente a `/dashboard` senza login

**Comportamento Atteso:**
- AuthFilter blocca richiesta
- Redirect a `/login?error=session_expired`

---

### TA3 – Upload di file con estensione vietata (.exe, .pdf)

**Input:**
- File: `malware.exe` o `document.pdf`

**Comportamento Atteso:**
- Upload rifiutato
- Messaggio: "Only .txt files are allowed"
- File non salvato

---

### TA4 – Upload di file con estensione lecita ma contenuto malevolo

**Input:**
- File rinominato: `malware.exe` → `malware.txt`
- Contenuto: binario eseguibile

**Comportamento Atteso:**
- Apache Tika rileva tipo reale (non text/plain)
- Upload rifiutato
- Messaggio: "File content validation failed"

**Test:**
```bash
# Rinominare file binario
copy notepad.exe fake.txt
# Caricare fake.txt
```

---

### TA5 – Upload di contenuto testuale con script per stored XSS

**Input:**
- File: `xss.txt` con contenuto:
  ```html
  <script>alert('XSS')</script>
  ```

**Comportamento Atteso:**
- Upload accettato (è text/plain)
- Visualizzazione: script mostrato come testo, NON eseguito
- OWASP Encoder converte `<` in `&lt;`
- Content-Type: `text/plain` previene parsing HTML

**Verifica:**
- View file non deve mostrare alert popup
- View source deve mostrare `&lt;script&gt;`

---

### TA6 – Accesso a risorse protette senza sessione valida

**Input:**
- Aprire browser in modalità incognito
- Navigare a `/dashboard`

**Comportamento Atteso:**
- AuthFilter intercetta richiesta
- Redirect a `/login`

---

### TA7 – Riutilizzo di cookie o sessione scaduta

**Input:**
1. Login normale
2. Copiare valore cookie JSESSIONID
3. Logout
4. Tentare di reimpostare cookie con valore copiato (using DevTools)
5. Accedere a `/dashboard`

**Comportamento Atteso:**
- Sessione invalidata non può essere riutilizzata
- Redirect a `/login?error=session_expired`

---

### TA8 – Tentativo di esecuzione di file caricati

**Input:**
- Tentare di accedere direttamente al file fisico:
  `http://localhost:8080/secure-app-uploads/file_123.txt`

**Comportamento Atteso:**
- Errore 404 Not Found
- Upload directory NON è sotto `webapp/` quindi non accessibile da HTTP

**Verifica:**
- File salvati in `~/secure-app-uploads/` (fuori da webroot)
- Accesso solo tramite `/file-content?file=...` (servlet con controllo sessione)

---

## Strumenti di Test Consigliati

### Browser DevTools
- **Cookies**: Application → Storage → Cookies
- **Network**: Monitor request/response headers
- **Console**: Verificare assenza errori JavaScript

### Burp Suite / OWASP ZAP
- Intercettare e modificare richieste HTTP
- Test SQL injection automatici

### Database Client (H2 Console)
```
URL: jdbc:h2:~/secure-app-db
User: sa
Password: (vuoto)
```

## Checklist Test

- [ ] TU1 - Registrazione utente
- [ ] TU2 - Login valido
- [ ] TU3 - Login invalido
- [ ] TU4 - Accesso dashboard autenticato
- [ ] TU5 - Upload file .txt
- [ ] TU6 - Visualizzazione file sicura
- [ ] TU7 - Session timeout
- [ ] TU8 - Logout
- [ ] TU9 - Accesso post-logout negato
- [ ] TU10 - Upload concorrente
- [ ] TA1 - SQL Injection
- [ ] TA2 - Bypass autenticazione
- [ ] TA3 - Upload estensione vietata
- [ ] TA4 - Upload contenuto malevolo
- [ ] TA5 - Stored XSS
- [ ] TA6 - Accesso senza sessione
- [ ] TA7 - Riutilizzo sessione scaduta
- [ ] TA8 - Esecuzione file caricato

## Screenshot Necessari per Documentazione

1. Registrazione con password policy warning
2. Login con cookie HttpOnly/SameSite in DevTools
3. Upload file .txt con successo
4. Visualizzazione file con Content-Type text/plain
5. Upload rifiutato per estensione .exe
6. Upload rifiutato per contenuto binario (Tika check)
7. XSS tentato ma visualizzato come testo
8. SQL injection fallito
9. Accesso negato senza sessione
10. Database con password BCrypt hash
