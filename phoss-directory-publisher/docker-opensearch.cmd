@REM
@REM Copyright (C) 2015-2025 Philip Helger (www.helger.com)
@REM philip[at]helger[dot]com
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM         http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

@echo off
docker pull opensearchproject/opensearch:latest
docker pull opensearchproject/opensearch-dashboards:latest

:: Password requires a minimum of 8 characters and must contain at least one uppercase letter, one lowercase letter, one digit, and one special character. Password strength can be tested here: https://lowe.github.io/tryzxcvbn
docker run -d -p 9200:9200 -p 9600:9600 -e "discovery.type=single-node" -e "OPENSEARCH_INITIAL_ADMIN_PASSWORD=Passwd123!" -name opensearch opensearchproject/opensearch:latest

:: curl https://localhost:9200 -ku admin:Passwd123!
