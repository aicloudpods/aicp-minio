alias k=kubectl

k create ns minio

kubens minio

helm install minio stable/minio -f minio.yaml

