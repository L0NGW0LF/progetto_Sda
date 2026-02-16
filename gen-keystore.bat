@echo off
set "JAVA_EXE=%~dp0tools\jdk\bin\keytool.exe"
"%JAVA_EXE%" -genkeypair -alias jetty -keyalg RSA -keystore keystore.p12 -storetype PKCS12 -storepass password -keypass password -dname "CN=localhost, OU=Dev, O=SecureApp, L=City, ST=State, C=IT" -ext "SAN=dns:localhost,ip:127.0.0.1" -validity 3650
