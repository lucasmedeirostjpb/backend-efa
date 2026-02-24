@echo off
setlocal enabledelayedexpansion
chcp 65001 > nul

echo =======================================================
echo     TESTE DE SEGURANCA BIPARTIDA - SISTEMA POLVO
echo =======================================================
echo.

:: 1. TENTANDO ACESSAR A ROTA PUBLICA
echo [1/3] Testando acesso a rota PUBLICA (Portal de Transparencia)...
echo Comando: curl -s -w "\nHTTP Code: %%{http_code}" -X GET "http://localhost:8081/api/public/teste"
curl -s -w "\nHTTP Code: %%{http_code}" -X GET "http://localhost:8081/api/public/teste"
echo.
echo.

:: 2. TENTANDO ACESSAR A ROTA PRIVADA SEM TOKEN
echo [2/3] Testando acesso a rota PRIVADA (Gestao) SEM TOKEN...
echo Comando: curl -s -o NUL -w "\nHTTP Code: %%{http_code}" -X GET "http://localhost:8081/api/gestao/teste"
curl -s -o NUL -w "HTTP Code: %%{http_code}" -X GET "http://localhost:8081/api/gestao/teste"
echo  (Esperado: 401 Unauthorized)
echo.
echo.

:: 3. OBTENDO TOKEN E ACESSANDO A ROTA PRIVADA
echo [3/3] Testando acesso a rota PRIVADA (Gestao) COM TOKEN (Role COORDENADOR)...
echo Gerando novo token JWT no Keycloak...

:: Fazendo o request para o Keycloak e extraindo apenas o access_token usando PowerShell inline
for /f "usebackq tokens=*" %%a in (`powershell -NoProfile -Command "$response = Invoke-RestMethod -Uri 'http://localhost:8080/realms/tjpb-polvo/protocol/openid-connect/token' -Method POST -Body @{ client_id='polvo-app'; username='joao123'; password='joao123'; grant_type='password' }; $response.access_token"`) do (
    set "TOKEN=%%a"
)

if "%TOKEN%"=="" (
    echo [ERRO] Nao foi possivel obter o token do Keycloak. Verifique se o container esta rodando e as credenciais.
    goto :fim
)

echo [OK] Token obtido com sucesso!
echo.
echo Acessando rota protegida...
echo Comando: curl -i -s -w "\nHTTP Code: %%{http_code}" -X GET "http://localhost:8081/api/gestao/teste" -H "Authorization: Bearer <TOKEN>"
echo.
echo --- RESPOSTA DA API ---
curl -i -s -w "\nHTTP Code: %%{http_code}" -X GET "http://localhost:8081/api/gestao/teste" -H "Authorization: Bearer %TOKEN%"
echo.
echo -----------------------

:fim
echo.
echo =======================================================
echo                    TESTES FINALIZADOS
echo =======================================================
pause
