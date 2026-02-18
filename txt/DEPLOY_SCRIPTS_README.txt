# 🚀 Script di Deployment Automatico

Questo progetto include script automatici per semplificare il build e il deployment dell'applicazione.

## 📁 File Disponibili

- **`deploy.ps1`** - Script PowerShell per Windows
- **`deploy.sh`** - Script Bash per Linux/macOS

## 💻 Windows (PowerShell)

### Prerequisiti
- PowerShell 5.0+ (incluso in Windows 10/11)
- JDK e Maven (possono essere i tool locali nella cartella `tools/`)

### Comandi Disponibili

#### 1. Build del Progetto
```powershell
.\deploy.ps1 build
```
Compila il progetto e genera il file WAR.

#### 2. Avvia con Jetty (Modalità Sviluppo)
```powershell
.\deploy.ps1 run-jetty
```
Compila e avvia automaticamente l'applicazione su http://localhost:9090/

**Con porta custom:**
```powershell
.\deploy.ps1 run-jetty -Port 8080
```

#### 3. Deploy su Tomcat
```powershell
.\deploy.ps1 deploy-tomcat -TomcatHome "C:\apache-tomcat-9.0"
```
Copia il WAR nella directory webapps di Tomcat.

#### 4. Pulisci Progetto
```powershell
.\deploy.ps1 clean
```
Elimina i file compilati e opzionalmente database/upload.

#### 5. Test
```powershell
.\deploy.ps1 test
```
Verifica che il build sia corretto e che il WAR abbia la struttura attesa.

### Esempio Workflow
```powershell
# Prima volta:
.\deploy.ps1 run-jetty

# Per ricompilare dopo modifiche:
.\deploy.ps1 clean
.\deploy.ps1 run-jetty
```

## 🐧 Linux/macOS (Bash)

### Prerequisiti
- Bash shell
- JDK e Maven installati (o nella cartella `tools/`)
- Permessi di esecuzione sullo script

### Prima Esecuzione
```bash
chmod +x deploy.sh
```

### Comandi Disponibili

#### 1. Build del Progetto
```bash
./deploy.sh build
```

#### 2. Avvia con Jetty
```bash
./deploy.sh run-jetty
```

**Con porta custom:**
```bash
./deploy.sh run-jetty --port 8080
```

#### 3. Deploy su Tomcat
```bash
./deploy.sh deploy-tomcat --tomcat /opt/tomcat
```

#### 4. Pulisci Progetto
```bash
./deploy.sh clean
```

#### 5. Test
```bash
./deploy.sh test
```

## 🎯 Funzionalità degli Script

### ✅ Controlli Automatici
- Verifica presenza Java e Maven
- Utilizza automaticamente i tool locali in `tools/` se disponibili
- Scarica automaticamente Jetty Runner se necessario
- Verifica struttura del WAR generato

### 🔧 Build Ottimizzato
- Pulizia automatica della directory `target/`
- Compilazione con `mvn clean package`
- Feedback colorato e chiaro

### 🚀 Deploy Facile
- **Jetty**: Avvia il server con un solo comando
- **Tomcat**: Copia automatica nella directory webapps
- Configurazione porta personalizzabile

### 🧹 Pulizia Intelligente
- Elimina build artifacts
- Opzione per eliminare database locale
- Opzione per eliminare file caricati
- Conferma interattiva prima di eliminare dati utente

## 📊 Output Script

Gli script forniscono feedback visivo con:
- ✓ **Verde**: Operazione completata con successo
- ✗ **Rosso**: Errore
- ℹ **Cyan**: Informazione
- ⚠ **Giallo**: Warning

## 🐛 Troubleshooting

### "Execution Policy" Error (Windows)
Se vedi errori relativi alla policy di esecuzione:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### "Permission Denied" (Linux/macOS)
```bash
chmod +x deploy.sh
```

### Java/Maven Non Trovati
Gli script cercano prima nella cartella `tools/` del progetto, poi nel PATH di sistema. Assicurati che almeno uno dei due sia disponibile.

### Porta Già in Uso
Se la porta 9090 è occupata:
```powershell
# Windows
.\deploy.ps1 run-jetty -Port 8080

# Linux/macOS
./deploy.sh run-jetty --port 8080
```

## 📝 Note

- Gli script creano automaticamente il file `jetty-runner.jar` se non presente
- Il database H2 viene creato automaticamente in `~/secure-app-db.mv.db`
- I file caricati vanno in `~/secure-app-uploads/`
- Per fermare Jetty, premere **Ctrl+C**

## 🎓 Per il Progetto Universitario

### Deployment Rapido per Test
```powershell
# Windows - Test completo
.\deploy.ps1 build
.\deploy.ps1 test
.\deploy.ps1 run-jetty
```

```bash
# Linux/macOS - Test completo
./deploy.sh build
./deploy.sh test
./deploy.sh run-jetty
```

### Preparazione Consegna
```powershell
# Rebuild completo pulito
.\deploy.ps1 clean
.\deploy.ps1 build
.\deploy.ps1 test

# Verifica WAR in target/secure-web-app.war
```

---

**Suggerimento**: Per vedere tutte le opzioni disponibili:
```powershell
# Windows
.\deploy.ps1 -Action help

# Linux/macOS
./deploy.sh help
```
