#Reactive Web Monitoring
Harici kurulması gereken sistemler:
- Apache Kafka(kafka_2.12-2.3.0)
- Redis 5.0.5

Harici sistemlerin başlatılması için yardımcı olması amacıyla scripts klasörü oluşturulmuştur.
1) `sh start-zookeeper.sh`
2) `sh start-kafka.sh`
3) `sh start-redis.sh`
---
Publisher modülünü başlatmak için
1) `sh startProducerApp.sh`
---
Subscriber modülünü başlatmak için `DigestionKafka` main metodu çalıştırır gibi çalıştırılır.

#Extra
## Start akka actor as main program
```
$ sbt "runMain akka.Main sample.Main"
```

### Redis commands
```
127.0.0.1:6379> KEYS '*'
127.0.0.1:6379> GET "https://jsonblob.com/api/jsonBlob/24846a60-d2fc-11e9-8bb6-8b4a440e0096"
```

### Consumer json parsing related libraries
- https://www.baeldung.com/guide-to-jayway-jsonpath
- https://github.com/dfilatov/jspath
