#!/bin/bash
# ============================================================================
# Script di Deploy Automatico - Secure Web Application
# Progetto: Sicurezza nelle Applicazioni - Università di Bari
# Sistema: Linux/macOS
# ============================================================================

set -e  # Exit on error

# Configurazione
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_HOME_LOCAL="$PROJECT_DIR/tools/jdk"
MAVEN_HOME_LOCAL="$PROJECT_DIR/tools/maven"
WAR_FILE="$PROJECT_DIR/target/secure-web-app.war"
JETTY_RUNNER="$PROJECT_DIR/jetty-runner.jar"
JETTY_URL="https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-runner/9.4.53.v20231009/jetty-runner-9.4.53.v20231009.jar"

# Colori
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Funzioni output
log_success() { echo -e "${GREEN}✓${NC} $1"; }
log_error() { echo -e "${RED}✗${NC} $1"; }
log_info() { echo -e "${CYAN}ℹ${NC} $1"; }
log_warning() { echo -e "${YELLOW}⚠${NC} $1"; }

# Banner
show_banner() {
    echo ""
    echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║     SECURE WEB APPLICATION - DEPLOYMENT SCRIPT            ║${NC}"
    echo -e "${CYAN}║     Università degli Studi di Bari Aldo Moro             ║${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# Help
show_help() {
    echo ""
    echo -e "${YELLOW}USO: ./deploy.sh [ACTION] [OPTIONS]${NC}"
    echo ""
    echo -e "${CYAN}ACTIONS:${NC}"
    echo "  build          - Compila il progetto e genera WAR"
    echo "  run-jetty      - Compila e avvia con Jetty Runner (default)"
    echo "  deploy-tomcat  - Deploy su Tomcat esistente"
    echo "  clean          - Pulisce build artifacts"
    echo "  test           - Verifica build e struttura WAR"
    echo ""
    echo -e "${CYAN}OPTIONS:${NC}"
    echo "  --port <numero>        - Porta per Jetty (default: 9090)"
    echo "  --tomcat <path>        - Path installazione Tomcat"
    echo ""
    echo -e "${CYAN}ESEMPI:${NC}"
    echo "  ./deploy.sh build"
    echo "  ./deploy.sh run-jetty --port 8080"
    echo "  ./deploy.sh deploy-tomcat --tomcat /opt/tomcat"
    echo "  ./deploy.sh clean"
    echo ""
}

# Verifica prerequisiti
check_prerequisites() {
    log_info "Verifica prerequisiti..."
    
    # Java
    if [ -d "$JAVA_HOME_LOCAL" ] && [ -f "$JAVA_HOME_LOCAL/bin/java" ]; then
        log_info "Trovato JDK locale, imposto permessi di esecuzione..."
        chmod +x "$JAVA_HOME_LOCAL/bin/java" || true
        export JAVA_HOME="$JAVA_HOME_LOCAL"
        export PATH="$JAVA_HOME/bin:$PATH"
        JAVA_VERSION=$("$JAVA_HOME/bin/java" -version 2>&1 | head -n 1)
        log_success "Java trovato: $JAVA_VERSION"
    elif command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1)
        log_success "Java di sistema: $JAVA_VERSION"
    else
        log_error "Java non trovato! Installare JDK 8+"
        exit 1
    fi
    
    # Maven
    if [ -f "$MAVEN_HOME_LOCAL/bin/mvn" ]; then
        log_info "Trovato Maven locale, imposto permessi di esecuzione..."
        chmod +x "$MAVEN_HOME_LOCAL/bin/mvn" || true
        MVN_CMD="$MAVEN_HOME_LOCAL/bin/mvn"
        log_success "Maven locale trovato"
    elif command -v mvn &> /dev/null; then
        MVN_CMD="mvn"
        MVN_VERSION=$(mvn -version 2>&1 | head -n 1)
        log_success "Maven di sistema: $MVN_VERSION"
    else
        log_error "Maven non trovato!"
        exit 1
    fi
}

# Build progetto
build_project() {
    local clean_first=$1
    
    log_info "Building progetto..."
    
    if [ "$clean_first" = "true" ] && [ -d "target" ]; then
        log_info "Pulizia target directory..."
        rm -rf target
    fi
    
    log_info "Esecuzione: mvn clean package..."
    
    if $MVN_CMD clean package; then
        log_success "Build completata con successo!"
        
        if [ -f "$WAR_FILE" ]; then
            WAR_SIZE=$(du -h "$WAR_FILE" | cut -f1)
            log_success "WAR generato: secure-web-app.war ($WAR_SIZE)"
            return 0
        else
            log_error "WAR file non trovato dopo build!"
            return 1
        fi
    else
        log_error "Build fallita!"
        return 1
    fi
}

# Scarica Jetty Runner
download_jetty() {
    if [ ! -f "$JETTY_RUNNER" ]; then
        log_info "Jetty Runner non trovato, download in corso..."
        
        if command -v wget &> /dev/null; then
            wget -O "$JETTY_RUNNER" "$JETTY_URL"
        elif command -v curl &> /dev/null; then
            curl -L -o "$JETTY_RUNNER" "$JETTY_URL"
        else
            log_error "Né wget né curl sono disponibili!"
            return 1
        fi
        
        log_success "Jetty Runner scaricato con successo"
    else
        log_success "Jetty Runner già presente"
    fi
    
    return 0
}

# Avvia Jetty
start_jetty() {
    local port=$1
    
    log_info "Avvio server Jetty sulla porta $port..."
    
    if [ ! -f "$WAR_FILE" ]; then
        log_error "WAR file non trovato! Eseguire prima './deploy.sh build'"
        return 1
    fi
    
    if ! download_jetty; then
        return 1
    fi
    
    # Determina Java
    if [ -f "$JAVA_HOME_LOCAL/bin/java" ]; then
        JAVA_CMD="$JAVA_HOME_LOCAL/bin/java"
    else
        JAVA_CMD="java"
    fi
    
    log_success "Server in avvio..."
    log_info "URL: http://localhost:$port/"
    log_warning "Premi Ctrl+C per fermare il server"
    echo ""
    
    $JAVA_CMD -jar "$JETTY_RUNNER" --port "$port" "$WAR_FILE"
}

# Deploy su Tomcat
deploy_tomcat() {
    local tomcat_path=$1
    
    if [ -z "$tomcat_path" ]; then
        log_error "Path Tomcat non specificato!"
        log_info "Uso: ./deploy.sh deploy-tomcat --tomcat /path/to/tomcat"
        return 1
    fi
    
    if [ ! -d "$tomcat_path" ]; then
        log_error "Directory Tomcat non trovata: $tomcat_path"
        return 1
    fi
    
    local webapps_dir="$tomcat_path/webapps"
    
    if [ ! -d "$webapps_dir" ]; then
        log_error "Directory webapps non trovata in Tomcat"
        return 1
    fi
    
    if [ ! -f "$WAR_FILE" ]; then
        log_error "WAR file non trovato! Eseguire prima './deploy.sh build'"
        return 1
    fi
    
    log_info "Copia WAR in $webapps_dir..."
    cp "$WAR_FILE" "$webapps_dir/secure-web-app.war"
    
    log_success "Deploy completato!"
    log_info "Avviare Tomcat e accedere a: http://localhost:8080/secure-web-app/"
    
    return 0
}

# Pulizia
clean_project() {
    log_info "Pulizia progetto..."
    
    if [ -d "target" ]; then
        rm -rf target
        log_success "Directory target eliminata"
    fi
    
    # Database
    local db_file="$HOME/secure-app-db.mv.db"
    if [ -f "$db_file" ]; then
        read -p "Eliminare anche il database? (s/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Ss]$ ]]; then
            rm -f "$db_file"
            log_success "Database eliminato"
        fi
    fi
    
    # Uploads
    local uploads_dir="$HOME/secure-app-uploads"
    if [ -d "$uploads_dir" ]; then
        read -p "Eliminare anche i file caricati? (s/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Ss]$ ]]; then
            rm -rf "$uploads_dir"
            log_success "File caricati eliminati"
        fi
    fi
    
    log_success "Pulizia completata"
    return 0
}

# Test
test_application() {
    log_info "Test rapido dell'applicazione..."
    
    if [ ! -f "$WAR_FILE" ]; then
        log_error "WAR file non trovato!"
        return 1
    fi
    
    log_success "Build test: OK"
    
    # Verifica contenuto WAR
    log_info "Verifica struttura WAR..."
    
    if command -v unzip &> /dev/null; then
        local temp_dir=$(mktemp -d)
        unzip -q "$WAR_FILE" -d "$temp_dir"
        
        local required_files=(
            "WEB-INF/web.xml"
            "WEB-INF/classes/com/secureapp/servlet/LoginServlet.class"
            "WEB-INF/classes/com/secureapp/service/ConcurrentUploadService.class"
        )
        
        local all_found=true
        for file in "${required_files[@]}"; do
            if [ -f "$temp_dir/$file" ]; then
                log_success "Trovato: $file"
            else
                log_error "Mancante: $file"
                all_found=false
            fi
        done
        
        rm -rf "$temp_dir"
        
        if [ "$all_found" = true ]; then
            log_success "Struttura WAR: OK"
            return 0
        else
            log_error "Struttura WAR incompleta!"
            return 1
        fi
    else
        log_warning "unzip non disponibile, skip verifica struttura"
        return 0
    fi
}

# ============================================================================
# MAIN
# ============================================================================

show_banner

# Parse arguments
ACTION="${1:-run-jetty}"
PORT=9090
TOMCAT_PATH=""

shift || true
while [[ $# -gt 0 ]]; do
    case $1 in
        --port)
            PORT="$2"
            shift 2
            ;;
        --tomcat)
            TOMCAT_PATH="$2"
            shift 2
            ;;
        *)
            log_error "Opzione sconosciuta: $1"
            show_help
            exit 1
            ;;
    esac
done

# Verifica prerequisiti
check_prerequisites
echo ""

# Esegui azione
case $ACTION in
    build)
        build_project true
        echo ""
        log_success "Build completata! WAR pronto per il deploy."
        ;;
        
    run-jetty)
        build_project false
        echo ""
        start_jetty "$PORT"
        ;;
        
    deploy-tomcat)
        build_project false
        echo ""
        deploy_tomcat "$TOMCAT_PATH"
        ;;
        
    clean)
        clean_project
        ;;
        
    test)
        build_project false
        echo ""
        test_application
        echo ""
        log_success "Tutti i test superati!"
        ;;
        
    help|--help|-h)
        show_help
        ;;
        
    *)
        log_error "Azione sconosciuta: $ACTION"
        show_help
        exit 1
        ;;
esac
