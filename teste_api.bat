@echo off
setlocal enabledelayedexpansion
chcp 65001 > nul

echo =======================================================
echo     TESTE COMPLETO DE SEGURANCA - SISTEMA POLVO
echo =======================================================
echo.

:: =====================================================
:: TESTES DE ROTAS ORIGINAIS
:: =====================================================

:: 1. ROTA PUBLICA
echo [1/7] Testando acesso a rota PUBLICA (Portal de Transparencia)...
echo Comando: GET /api/public/teste
curl -s -w "\nHTTP Code: %%{http_code}" -X GET "http://localhost:8081/api/public/teste"
echo.
echo.

:: 2. ROTA PRIVADA (GESTAO) SEM TOKEN
echo [2/7] Testando acesso a rota PRIVADA (Gestao) SEM TOKEN...
echo Comando: GET /api/gestao/teste
curl -s -o NUL -w "HTTP Code: %%{http_code}" -X GET "http://localhost:8081/api/gestao/teste"
echo  (Esperado: 401 Unauthorized)
echo.
echo.

:: =====================================================
:: TESTES DE ROTAS DE METAS
:: =====================================================

:: 3. LISTAR METAS (GET PUBLICO)
echo [3/7] Testando GET /api/metas (acesso PUBLICO - listar metas)...
echo Comando: GET /api/metas
curl -s -w "\nHTTP Code: %%{http_code}" -X GET "http://localhost:8081/api/metas"
echo.
echo  (Esperado: 200 OK)
echo.

:: 4. CRIAR META SEM TOKEN (DEVE FALHAR)
echo [4/7] Testando POST /api/metas SEM TOKEN (deve ser bloqueado)...
echo Comando: POST /api/metas (sem Authorization)
curl -s -o NUL -w "HTTP Code: %%{http_code}" -X POST "http://localhost:8081/api/metas" -H "Content-Type: application/json" -d "{\"titulo\":\"Meta nao autorizada\",\"descricao\":\"Teste sem token\"}"
echo  (Esperado: 401 Unauthorized)
echo.
echo.

:: =====================================================
:: OBTENDO TOKEN DO KEYCLOAK
:: =====================================================

echo [5/7] Obtendo token JWT do Keycloak (usuario joao123 - Role COORDENADOR)...

for /f "usebackq tokens=*" %%a in (`powershell -NoProfile -Command "$response = Invoke-RestMethod -Uri 'http://localhost:8080/realms/tjpb-polvo/protocol/openid-connect/token' -Method POST -Body @{ client_id='polvo-app'; username='joao123'; password='joao123'; grant_type='password' }; $response.access_token"`) do (
    set "TOKEN=%%a"
)

if "%TOKEN%"=="" (
    echo [ERRO] Nao foi possivel obter o token do Keycloak. Verifique se o container esta rodando e as credenciais.
    goto :fim
)

echo [OK] Token obtido com sucesso!
echo.

:: 5. ROTA PRIVADA GESTAO COM TOKEN
echo [5/7] Testando GET /api/gestao/teste COM TOKEN...
echo Comando: GET /api/gestao/teste (com Authorization Bearer)
echo.
echo --- RESPOSTA ---
curl -s -w "\nHTTP Code: %%{http_code}" -X GET "http://localhost:8081/api/gestao/teste" -H "Authorization: Bearer %TOKEN%"
echo.
echo ----------------
echo.

:: 6. CRIAR META COM TOKEN (POST)
echo [6/7] Testando POST /api/metas COM TOKEN (criar nova meta)...
echo Comando: POST /api/metas (com Authorization Bearer)
echo.
echo --- RESPOSTA ---
curl -s -w "\nHTTP Code: %%{http_code}" -X POST "http://localhost:8081/api/metas" -H "Content-Type: application/json" -H "Authorization: Bearer %TOKEN%" -d "{\"titulo\":\"Reduzir tempo de tramitacao\",\"descricao\":\"Diminuir o tempo medio de tramitacao processual em 15%%\"}"
echo.
echo ----------------
echo  (Esperado: 200 OK com a meta criada)
echo.

:: 7. ATUALIZAR META COM TOKEN (PUT)
echo [7/7] Testando PUT /api/metas/1 COM TOKEN (atualizar meta id=1)...
echo Comando: PUT /api/metas/1 (com Authorization Bearer)
echo.
echo --- RESPOSTA ---
curl -s -w "\nHTTP Code: %%{http_code}" -X PUT "http://localhost:8081/api/metas/1" -H "Content-Type: application/json" -H "Authorization: Bearer %TOKEN%" -d "{\"titulo\":\"Reduzir tempo de tramitacao (Atualizada)\",\"descricao\":\"Diminuir o tempo medio de tramitacao processual em 20%%\",\"concluida\":true}"
echo.
echo ----------------
echo  (Esperado: 200 OK com a meta atualizada)

:fim
echo.
echo =======================================================
echo                    TESTES FINALIZADOS
echo =======================================================
pause
