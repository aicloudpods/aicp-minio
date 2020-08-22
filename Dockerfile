FROM aicp/aicp-spark-prometheus:2.4.6

COPY prometheus-config.yml /opt/spark/examples/prometheus-config.yml
COPY metrics.properties /opt/spark/examples/metrics.properties
COPY target/sparkonminio.jar /opt/spark/examples/jars/sparkonminio.jar