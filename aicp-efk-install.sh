alias k=kubectl

k create ns logging

kubens logging

helm repo add elastic https://helm.elastic.co

helm repo add kiwigrid https://kiwigrid.github.io

helm repo update

helm install elasticsearch elastic/elasticsearch -f elastic.yaml

helm install kibana elastic/kibana -f kibana.yaml

helm install fluentd kiwigrid/fluentd-elasticsearch -f fluentd.yaml 
