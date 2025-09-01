# Docker images

* docker pull opensearchproject/opensearch:3
* docker pull opensearchproject/opensearch-dashboards:3
* docker network create os-net

# Running locally

## OpenSearch

* docker run -d --name opensearch-node -p 9200:9200 -p 9600:9600 --network os-net -e "discovery.type=single-node" -e 'OPENSEARCH_INITIAL_ADMIN_PASSWORD=12PASSword!!' opensearchproject/opensearch:3
* curl https://localhost:9200 -ku 'admin:12PASSword!!'

## Dashboard

* docker run -d --name opensearch-dashboard -p 5601:5601 --network os-net -v "$(pwd)/docs/opensearch_dashboards.yml:/usr/share/opensearch-dashboards/config/opensearch_dashboards.yml" opensearchproject/opensearch-dashboards:3
