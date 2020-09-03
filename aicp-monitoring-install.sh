alias k=kubectl

k create ns monitoring

kubens monitoring

helm repo add googlecharts https://kubernetes-charts.storage.googleapis.com/

helm repo update

helm install prometheus googlecharts/prometheus

k apply -f  grafana-config.yml

helm install grafana googlecharts/grafana -f grafana-values.yaml
