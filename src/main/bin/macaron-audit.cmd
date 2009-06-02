@echo off
REM
REM Copyright 2009 Philippe Prados. 
REM http://macaron.googlecode.com
REM 
REM Licensed under the Apache License, Version 2.0 (the "License");
REM you may not use this file except in compliance with the License.
REM You may obtain a copy of the License at
REM 
REM      http://www.apache.org/licenses/LICENSE-2.0
REM 
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.
REM 

if "%OS%" == "Windows_NT"  setlocal enableDelayedExpansion

Rem calculate MACARON_HOME
set MACARON_HOME=.\
if "%OS%" == "Windows_NT" set MACARON_HOME=%~dp0%
pushd %MACARON_HOME%..
set MACARON_HOME=%CD%
popd

set JAVA_PRG=%JAVA_HOME%\bin\java.exe
if "%JAVA_HOME%" == "" set JAVA_PRG=java.exe

rem echo ---------
rem echo JAVA_HOME=%JAVA_HOME%
rem echo JAVA_OPTS=%JAVA_OPTS%
rem echo MACARON_HOME=%MACARON_HOME%
rem echo ---------

for %%I in (%MACARON_HOME%\lib\audit-*.jar) do set JAR=%%I
"%JAVA_PRG%" %JAVA_OPTS% -jar "%JAR%" %*
