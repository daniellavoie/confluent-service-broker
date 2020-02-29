# Confluent Service Broker Quickstart

### Pre-requisites

* Credentials for a Cloud Foundry environment (try https://run.pivotal.io).
* Access to an existing Kafka Cluster (try https://confluent.cloud).
* Java 11

### Clone the repository

```
$ git clone https://github.com/daniellavoie/confluent-service-broker
```

### Build the project

``` 
$ ./mvnw clean package
```

### Prepare a manifest

Prepare a manifest named `manifest.yml` that will be used to run the application in Cloud Foundry. Feel free to get inspiration from [sample-manifest.yml](sample-manifest.yml). Ensure that you are configuring your Cloud Foundry and Kafka credentials in all the sections of the manifest.

### Deploy the service broker to Cloud Foundry

```
$ cf push -d my-domain.com -n my-hostname -f PATH-TO-MANIFEST confluent-service-broker
```

### Register the service broker

``` 
$ cf create-service-broker confluent-service-broker username password https://my-hostname.my-domain.com --space-scoped
```

## Enable the service access

```
$ cf enable-service-access confluent-kafka -o my-org
```

### Create a service instance

```
$ cf create-service confluent-kafka preprovisioned-account my-kafka-service
```

### Bind an application to the service instance

During this binding operation. The service broker will look for you application name, space and org and will look for a matching `confluent.servicebroker.controlpane.cluster-providers.credentials.name` configuration. If none is found, an error will be returned and the application will be refused access to the Kafka cluster.

```
$ cf bind-service confluent-kafka my-application
```