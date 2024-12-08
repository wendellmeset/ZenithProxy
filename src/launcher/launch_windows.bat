@echo off
setlocal

REM Check if Python is already installed
if not exist python\python.exe (
    REM Download python
    echo Downloading Python...
    curl -L -o python.tar.gz "https://github.com/indygreg/python-build-standalone/releases/download/20241206/cpython-3.13.1+20241206-x86_64-pc-windows-msvc-shared-install_only_stripped.tar.gz"

    REM Extract tarball
    echo Extracting Python...
    tar -xf python.tar.gz

    REM Delete tarball
    del python.tar.gz
) else (
    echo Found existing Python installation.
)

REM Install pip requirements.txt
echo Verifying requirements...
python\python.exe -m pip install -r requirements.txt -qq --disable-pip-version-check --no-input

REM Run launcher-py.zip
echo Starting Launcher...
python\python.exe launcher-py.zip

endlocal
