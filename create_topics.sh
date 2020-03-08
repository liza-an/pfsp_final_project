echo ""Number of selected partitions: $1"

if [ ! -z "$1" ];
    then
        docker run --net=host --rm confluentinc/cp-kafka:5.1.0 kafka-topics --create --topic news-data --partitions "$1" --replication-factor 1 --if-not-exists --zookeeper localhost:2181

        docker run --net=host --rm confluentinc/cp-kafka:5.1.0 kafka-topics --create --topic stocks-data --partitions "$1" --replication-factor 1 --if-not-exists --zookeeper localhost:2181

        docker run --net=host --rm confluentinc/cp-kafka:5.1.0 kafka-topics --create --topic twitter-data  --partitions "$1" --replication-factor 1 --if-not-exists --zookeeper localhost:2181

        docker run --net=host --rm confluentinc/cp-kafka:5.1.0 kafka-topics --create --topic output-topic  --partitions "$1" --replication-factor 1 --if-not-exists --zookeeper localhost:2181
else
    echo "You should add number of partitions as argument!!!"
fi
