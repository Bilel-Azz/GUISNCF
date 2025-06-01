@echo off
setlocal

:: === CONFIGURATION ===
set APP_NAME=AnalyseTram
set MAIN_JAR=demo-1.0-SNAPSHOT.jar
set MAIN_CLASS=org.sncf.gui.AnalyseTram
set ICON=app.ico
set VERSION=1.0.0
set RUNTIME=runtime
set DIST_DIR=dist

:: === ÉTAPE 1 - Compilation Maven ===
echo [1/4] Compilation Maven...
call mvn clean package

if errorlevel 1 (
    echo ❌ Échec de la compilation. Script interrompu.
    pause
    exit /b 1
)

:: === ÉTAPE 2 - (Re)générer le runtime avec jlink ===
echo [2/4] Génération de l’image Java optimisée...
rmdir /s /q %RUNTIME% 2>nul
jlink --add-modules java.base,java.desktop,java.sql --output %RUNTIME%

:: === ÉTAPE 3 - Nettoyage de l’ancien MSI ===
echo [3/4] Nettoyage du répertoire %DIST_DIR%...
rmdir /s /q %DIST_DIR% 2>nul
mkdir %DIST_DIR%

:: === ÉTAPE 4 - Génération du MSI avec jpackage ===
echo [4/4] Création du MSI...
jpackage ^
  --name %APP_NAME% ^
  --input target ^
  --main-jar %MAIN_JAR% ^
  --main-class %MAIN_CLASS% ^
  --type msi ^
  --icon %ICON% ^
  --java-options "-Xmx512m" ^
  --dest %DIST_DIR% ^
  --app-version %VERSION% ^
  --runtime-image %RUNTIME% ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut ^
  --win-menu-group "SNCF Tools"

echo ✅ MSI généré : %DIST_DIR%\%APP_NAME%-%VERSION%.msi
pause
endlocal
