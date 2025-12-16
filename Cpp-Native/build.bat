@echo on
REM =========================
REM Build Script for campus_backend.dll
REM =========================

REM Set JAVA_HOME locally for this script
set JAVA_HOME=C:\Program Files\Java\jdk-19

echo === Starting build for campus_backend.dll ===

REM Check JAVA_HOME
if "%JAVA_HOME%"=="" (
    echo ERROR: JAVA_HOME is not set!
    pause
    exit /b 1
)

REM Check g++ compiler
g++ --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: g++ compiler not found in PATH!
    pause
    exit /b 1
)

REM Compile the native library
echo Compiling JNI backend (multiple C++ files)...
g++ -std=c++17 -O2 -shared -o campus_backend.dll ^
  -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" ^
  native_impl.cpp graph.cpp avl_tree.cpp heap_attendance.cpp utils_json.cpp

REM Check if compilation succeeded
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)

echo ==== Build Complete ====
pause
