# Confluent Service Broker

## Supported platform

* Cloud Foundry
* Kubernetes Service Catalog (work in progress)

## Quick start

A quickstart guide is available [here](doc/quickstart.md).

## Run integration tests

### Build

```
$ ./mvnw clean package 
```

### Stand up the Kafka infrastrcture

```
$ ./docker-compose/local/scripts/start.sh
```

### Run the service broker for some integration tests

```
$ java -jar service-broker/target/confluent-service-broker.jar \
    --spring.profiles.active=integration-tests
```

## Run the integration tests against the service broker

```
$ ./mvnw -f integration-tests/pom.xml test
```

## Concepts

### Cluster Providers

TODO

### Account Providers

TODO

### Service

TODO

### Plan