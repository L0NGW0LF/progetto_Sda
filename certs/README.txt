# Keystore Setup for AES Encryption

## ⚠️ IMPORTANT: Security Notice

This directory contains ONLY example/placeholder files.  
**NEVER commit real keystores, certificates, or passwords to the repository.**

---

## Generating AES Keystore

### Step 1: Generate AES Secret Key

Use `keytool` to create a PKCS12 keystore with AES-256 key:

#### Windows PowerShell:
```powershell
keytool -genseckey `
  -alias aes-encryption-key `
  -keyalg AES `
  -keysize 256 `
  -keystore keystore.p12 `
  -storetype PKCS12 `
  -storepass YOUR_STRONG_PASSWORD `
  -keypass YOUR_STRONG_PASSWORD
```

#### Linux/macOS:
```bash
keytool -genseckey \
  -alias aes-encryption-key \
  -keyalg AES \
  -keysize 256 \
  -keystore keystore.p12 \
  -storetype PKCS12 \
  -storepass YOUR_STRONG_PASSWORD \
  -keypass YOUR_STRONG_PASSWORD
```

**Important**: Replace `YOUR_STRONG_PASSWORD` with a strong password (min 16 chars, mix of letters, numbers, symbols).

### Step 2: Move Keystore to Secure Location

DO NOT keep the keystore in the project directory!

#### Windows:
```powershell
mkdir C:\SecureApp\config
move keystore.p12 C:\SecureApp\config\
# Set permissions (only current user can read)
icacls C:\SecureApp\config\keystore.p12 /inheritance:r /grant:r "$env:USERNAME`:F"
```

#### Linux/macOS:
```bash
sudo mkdir -p /opt/secure-app/config
sudo mv keystore.p12 /opt/secure-app/config/
sudo chmod 600 /opt/secure-app/config/keystore.p12
sudo chown $USER:$USER /opt/secure-app/config/keystore.p12
```

### Step 3: Set Environment Variables

#### Windows (PowerShell - Session):
```powershell
$env:KEYSTORE_PATH = "C:\SecureApp\config\keystore.p12"
$env:KEYSTORE_PASSWORD = "your-strong-password"
$env:KEY_PASSWORD = "your-strong-password"
```

#### Windows (Persistent - System Environment Variables):
1. Open System Properties → Advanced → Environment Variables
2. Add New User Variables:
   - `KEYSTORE_PATH`: `C:\SecureApp\config\keystore.p12`
   - `KEYSTORE_PASSWORD`: `your-strong-password`
   - `KEY_PASSWORD`: `your-strong-password`

#### Linux/macOS (.bashrc or .profile):
```bash
export KEYSTORE_PATH=/opt/secure-app/config/keystore.p12
export KEYSTORE_PASSWORD=your-strong-password
export KEY_PASSWORD=your-strong-password
```

Then reload: `source ~/.bashrc`

### Step 4: Verify Setup

Run the application and check logs for:
```
AES encryption initialized with keystore: C:\SecureApp\config\keystore.p12
AES key alias: aes-encryption-key
```

If you see these messages, encryption is configured correctly!

---

## Security Best Practices

### Password Requirements
- **Minimum 16 characters**
- Mix of uppercase, lowercase, digits, special characters
- Use a password manager to generate and store
- Never share passwords via email or chat

### File Permissions
- Keystore file should be readable ONLY by the application user
- Use `chmod 600` (Linux/macOS) or `icacls` (Windows)
- Never commit keystore to version control

### Environment Variables
- Store passwords in environment variables, NOT in properties files
- Use `.env` file for local development (add to `.gitignore`)
- Use secret management systems for production (Azure Key Vault, AWS Secrets Manager, etc.)

---

## Key Rotation Procedure

### When to Rotate
- **Every 12 months** (recommended for compliance)
- **Immediately** if key compromise is suspected
- Before major version upgrades

### How to Rotate
1. Generate new keystore with different alias (e.g., `aes-encryption-key-2026-02`)
   ```bash
   keytool -genseckey -alias aes-encryption-key-2026-02 -keyalg AES -keysize 256 \
     -keystore keystore-new.p12 -storetype PKCS12 \
     -storepass NEW_PASSWORD -keypass NEW_PASSWORD
   ```

2. Update `application.properties` with new alias:
   ```properties
   keystore.aes.key.alias=aes-encryption-key-2026-02
   ```

3. **Migration** (requires custom script):
   - Read all encrypted files with OLD key
   - Decrypt with OLD key
   - Re-encrypt with NEW key
   - Save back to disk
   
4. Securely delete old keystore:
   ```bash
   # Windows
   cipher /w:C:\SecureApp\config\keystore-old.p12
   
   # Linux
   shred -u /opt/secure-app/config/keystore-old.p12
   ```

> **Note**: Full automatic key rotation implementation is outside initial scope.  
> This documentation provides the design and manual procedure.

---

## Troubleshooting

### Error: "Keystore configuration incomplete"
**Solution**: Set `KEYSTORE_PATH` and `KEYSTORE_PASSWORD` environment variables.

### Error: "FileNotFoundException: keystore.p12"
**Solution**: Verify `KEYSTORE_PATH` points to existing file. Check file permissions.

### Error: "AES key with alias 'xxx' not found"
**Solution**: Verify keystore contains key with correct alias. List keys:
```bash
keytool -list -keystore keystore.p12 -storetype PKCS12
```

### Error: "Wrong password"
**Solution**: Verify `KEYSTORE_PASSWORD` and `KEY_PASSWORD` are correct.

---

## Example .env File (local development)

Create `.env` file in project root (DO NOT COMMIT):
```
KEYSTORE_PATH=C:/SecureApp/config/keystore.p12
KEYSTORE_PASSWORD=MyStr0ngP@ssw0rd!2026
KEY_PASSWORD=MyStr0ngP@ssw0rd!2026
```

Add to `.gitignore`:
```
.env
```

---

## Compliance Notes

This implementation satisfies GEMINI.md requirement 4.8:
- ✅ AES-256-GCM for authenticated encryption
- ✅ Random IV for each encryption (never reused)
- ✅ Keystore external to repository
- ✅ No hardcoded keys
- ✅ Key rotation design documented
