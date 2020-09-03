alias k=kubectl

k create ns kafka

kubens kafka

helm repo add bitnami https://charts.bitnami.com/bitnami

helm repo update

helm install kafka bitnami/kafka

kubectl run kafka-client --restart='Never' --image docker.io/bitnami/kafka:2.6.0-debian-10-r0 --namespace kafka --command -- sleep infinity

sleep 60000

kubectl exec --tty -i kafka-client --namespace kafka -- kafka-topics.sh --create --zookeeper kafka-zookeeper.kafka.svc.cluster.local:2181 --replication-factor 1  --partitions 2 --topic aicp-topic






