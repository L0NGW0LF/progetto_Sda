param(
    [string]$Action = "run-jetty",
    [int]$Port = 9090,
    [string]$TomcatHome = ""
)

$PROJECT_DIR = $PSScriptRoot
$JAVA_HOME_LOCAL = Join-Path $PROJECT_DIR "tools\jdk"
$MAVEN_HOME_LOCAL = Join-Path $PROJECT_DIR "tools\maven"
$WAR_FILE = Join-Path $PROJECT_DIR "target\secure-web-app.war"
$JETTY_RUNNER = Join-Path $PROJECT_DIR "jetty-runner.jar"

function Write-Success {
    param($msg)
    Write-Host "OK: $msg" -ForegroundColor Green
}

function Write-Error-Custom {
    param($msg)
    Write-Host "ERROR: $msg" -ForegroundColor Red
}

function Write-Info {
    param($msg)
    Write-Host "INFO: $msg" -ForegroundColor Cyan
}

function Build-Project {
    param([bool]$Clean = $false)
    
    Write-Info "Building project..."
    
    if (Test-Path $JAVA_HOME_LOCAL) {
        $env:JAVA_HOME = $JAVA_HOME_LOCAL
    }
    
    $mvnCmd = "mvn"
    $mvnLocalPath = Join-Path $MAVEN_HOME_LOCAL "bin\mvn.cmd"
    if (Test-Path $mvnLocalPath) {
        $mvnCmd = $mvnLocalPath
    }
    
    try {
        if ($Clean) {
            Write-Info "Cleaning target directory..."
            if (Test-Path "target") {
                Remove-Item -Recurse -Force "target" -ErrorAction SilentlyContinue | Out-Null
            }
        }
        
        Write-Info "Running: mvn clean package"
        
        $proc = Start-Process -FilePath $mvnCmd -ArgumentList "clean", "package" -NoNewWindow -PassThru -Wait
        
        if ($proc.ExitCode -eq 0) {
            Write-Success "Build successful!"
            
            if (Test-Path $WAR_FILE) {
                Write-Success "WAR generated: secure-web-app.war"
                return $true
            }
            else {
                Write-Error-Custom "WAR file not found after build!"
                return $false
            }
        }
        else {
            Write-Error-Custom "Build failed"
            return $false
        }
    }
    catch {
        Write-Error-Custom "Build error: $_"
        return $false
    }
}

function Get-JettyRunner {
    if (-not (Test-Path $JETTY_RUNNER)) {
        Write-Info "Downloading Jetty Runner..."
        $jettyUrl = "https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-runner/9.4.53.v20231009/jetty-runner-9.4.53.v20231009.jar"
        
        try {
            Invoke-WebRequest -Uri $jettyUrl -OutFile $JETTY_RUNNER -UseBasicParsing
            if (Test-Path $JETTY_RUNNER) {
                Write-Success "Jetty Runner downloaded"
                return $true
            }
            else {
                return $false
            }
        }
        catch {
            Write-Error-Custom "Download failed: $_"
            return $false
        }
    }
    else {
        return $true
    }
}

function Start-JettyServer {
    param([int]$ServerPort = 9090)
    
    if (-not (Get-JettyRunner)) { return $false }
    
    if (-not (Test-Path $WAR_FILE)) {
        Write-Error-Custom "WAR missing! Run build first."
        return $false
    }
    
    $javaExe = "java"
    if (Test-Path $JAVA_HOME_LOCAL) {
        $javaExe = Join-Path $JAVA_HOME_LOCAL "bin\java.exe"
    }
    
    Write-Success "Starting server..."
    Write-Info "URL: http://localhost:$ServerPort/"
    Write-Info "Press Ctrl+C to stop"
    
    & $javaExe -jar $JETTY_RUNNER --port $ServerPort $WAR_FILE
}

function Deploy-ToTomcat {
    param([string]$TomcatPath)
    
    if ([string]::IsNullOrEmpty($TomcatPath)) {
        Write-Error-Custom "Tomcat path not specified!"
        return $false
    }
    
    $webappsDir = Join-Path $TomcatPath "webapps"
    if (-not (Test-Path $webappsDir)) {
        Write-Error-Custom "webapps directory not found"
        return $false
    }
    
    try {
        $destFile = Join-Path $webappsDir "secure-web-app.war"
        Copy-Item -Path $WAR_FILE -Destination $destFile -Force
        Write-Success "Deploy successful!"
        return $true
    }
    catch {
        Write-Error-Custom "Deploy error: $_"
        return $false
    }
}

function Clean-Project {
    Write-Info "Cleaning project..."
    if (Test-Path "target") {
        Remove-Item -Recurse -Force "target" -ErrorAction SilentlyContinue | Out-Null
        Write-Success "Target directory removed"
    }
    return $true
}

# MAIN
try {
    Write-Host "Action: $Action"
    
    if ($Action -eq "run") { $Action = "run-jetty" }

    if ($Action -eq "build") {
        Build-Project -Clean $true
    }
    elseif ($Action -eq "run-jetty") {
        if (Build-Project -Clean $false) {
            Start-JettyServer -ServerPort $Port
        }
    }
    elseif ($Action -eq "deploy-tomcat") {
        if (Build-Project -Clean $false) {
            Deploy-ToTomcat -TomcatPath $TomcatHome
        }
    }
    elseif ($Action -eq "clean") {
        Clean-Project
    }
    else {
        Write-Info "Usage: .\deploy.ps1 -Action [build|run|deploy-tomcat|clean]"
    }
}
catch {
    Write-Error "Script error: $_"
}
