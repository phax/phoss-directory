@echo off
docker pull opensearchproject/opensearch:latest
docker pull opensearchproject/opensearch-dashboards:latest

:: Password requires a minimum of 8 characters and must contain at least one uppercase letter, one lowercase letter, one digit, and one special character. Password strength can be tested here: https://lowe.github.io/tryzxcvbn
docker run -d -p 9200:9200 -p 9600:9600 -e "discovery.type=single-node" -e "OPENSEARCH_INITIAL_ADMIN_PASSWORD=Passwd123!" -name opensearch opensearchproject/opensearch:latest

:: curl https://localhost:9200 -ku admin:Passwd123!
