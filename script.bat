@echo off

REM Define database and collection names
set DB_NAME=amazon_reviews
set REVIEWS_COLLECTION=reviews
set METADATA_COLLECTION=metadata

REM Define file paths
set REVIEWS_FILE=Toys_and_Games.jsonl
set METADATA_FILE=meta_Toys_and_Games.jsonl

REM Check if MongoDB is running
echo Checking if MongoDB service is running...
sc query MongoDB | find "RUNNING" >nul 2>&1
if %errorlevel% neq 0 (
    echo MongoDB service is not running. Please start it and try again.
    pause
    exit /b
)

REM Import reviews data
echo Importing reviews data...
mongoimport --db %DB_NAME% --collection %REVIEWS_COLLECTION% --file %REVIEWS_FILE%
if %errorlevel% neq 0 (
    echo Failed to import reviews data. Please check the JSON file and try again.
    pause
    exit /b
)

REM Import metadata
echo Importing metadata...
mongoimport --db %DB_NAME% --collection %METADATA_COLLECTION% --file %METADATA_FILE%
if %errorlevel% neq 0 (
    echo Failed to import metadata. Please check the JSON file and try again.
    pause
   
