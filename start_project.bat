@echo off
title Orquestrador - Sistema Polvo
color 0A

echo =======================================================
echo   ORQUESTRADOR DE AMBIENTE: SISTEMA POLVO
echo =======================================================
echo.

:: 1. SUBINDO O KEYCLOAK
echo [1/2] Iniciando o Servidor de Autenticacao (Keycloak)...
:: COLOQUE O CAMINHO DA PASTA BIN DO SEU KEYCLOAK AQUI
cd "C:\Users\10350476403\Downloads\keycloak-26.5.4\keycloak-26.5.4\bin>"
start "Keycloak Server" cmd /k "kc.bat start-dev"

:: 2. O TEMPO DE RESPIRAÇÃO
echo.
echo Aguardando 15 segundos para o Keycloak ficar online...
echo (O Spring Boot precisa dele ligado para baixar as chaves)
timeout /t 15 /nobreak

:: 3. SUBINDO A API SPRING BOOT
echo.
echo [2/2] Iniciando a API Spring Boot...
:: COLOQUE O CAMINHO DA PASTA DO SEU PROJETO SPRING BOOT AQUI
cd /d "C:\Users\10350476403\Downloads\dev\polvo-api"
start "Polvo API (Spring Boot)" cmd /k ".\mvnw spring-boot:run"

echo.
echo =======================================================
echo   TUDO PRONTO! Pode fechar esta janela principal.
echo   O Keycloak e a API estao rodando nas outras janelas.
echo =======================================================
pause > nul