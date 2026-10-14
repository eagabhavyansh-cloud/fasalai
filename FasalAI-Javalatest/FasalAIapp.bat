@echo off
title FasalAI
set APPDIR=C:\FasalAI-Java
cd /d "%APPDIR%"
if errorlevel 1 (
    echo Could not find %APPDIR%
    echo Edit this .bat and fix the APPDIR line above to wherever FasalAIApp.class is.
    pause
    exit /b 1
)

if not exist FasalAIApp.class (
    echo FasalAIApp.class not found in %APPDIR% - run the build once from a Command Prompt:
    echo     javac CropModel.java CropInfo.java UIKit.java FasalAIApp.java
    pause
    exit /b 1
)

start "" javaw -cp . FasalAIApp
