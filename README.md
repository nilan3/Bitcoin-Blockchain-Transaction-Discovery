
# Bitcoin-Blockchain-Transaction-Discovery
Detect new transactions for a range of deposit addresses using Shapeshift API

### Pipeline Architecture
![alt text](https://raw.githubusercontent.com/nilan3/Bitcoin-Blockchain-Transaction-Discovery/master/pipeline-architecture.png)
The pipeline consists of 2 phases:
- **Transaction collection**: Batch Spark job which takes a list of address and distributes them amongst its workers before querying the shapeshift API to obtain the latest transaction. This is done every interval and JSON response is sent into kafka topic `raw_shapeshift_blockchain_transactions_v1`.
	- the list of deposit addresses and collection interval can be configured in `configurations/transaction_collection.yml`.
- **Transaction discovery**: Structured Streaming job which reads from kafka topic `raw_shapeshift_blockchain_transactions_v1` as a JSON stream and checks for changes in transaction for each unique address. This is achieved with custom stateful processing using user defined `GroupState`. Any new transactions detected for a specific address is sent to the output kafka topic `completed_shapeshift_blockchain_transactions_v1`.
	- state timeout (when data for an address is not seen for a period of time, it's saved state will be removed) can be configured in `configurations/transaction_discovery.yml`
	- spark uses checkpointing to store kafka offsets in HDFS - allows the application to continue reading from where it left off at in the case of any failures.

### Local Environment
- Docker Engine v18.09.2 (tested with constraints `CPU: 6 cores` and `MEM: 8 GB`
- Docker Compose v1.23.2
- Kafkacat ([https://github.com/edenhill/kafkacat](https://github.com/edenhill/kafkacat))

### Usage
Tested on Mac OS X v10.14.3
```bash
git clone https://github.com/nilan3/Bitcoin-Blockchain-Transaction-Discovery.git
```
```bash
docker compose up -d
```
Wait for listener on port 9092 to become active.

View created kafka topics:
```bash
kafkacat -b localhost:9092 -L | grep topic
```
View messages arriving in `raw_shapeshift_blockchain_transactions_v1`:
```bash
kafkacat -C -b localhost:9092 -o -5 -t raw_shapeshift_blockchain_transactions_v1
```
View messages arriving in `completed_shapeshift_blockchain_transactions_v1`:
```bash
kafkacat -C -b localhost:9092 -o -5 -t completed_shapeshift_blockchain_transactions_v1
```
![alt text](https://raw.githubusercontent.com/nilan3/Bitcoin-Blockchain-Transaction-Discovery/master/kafkacat.png)

Access Kibana on [http://localhost:5601/](http://localhost:5601/) and create index patterns `raw_shapeshift_blockchain_transactions_v1-*` and `completed_shapeshift_blockchain_transactions_v1-*` with timestamp field `@timestamp` in order to visualise data from kafka topics.

Data from kafka topics are ingested into ElasticSearch via Logstash; index names can be changed in the pipeline config `logstash/pipeline/kafka-ingestion.conf`

![alt text](https://raw.githubusercontent.com/nilan3/Bitcoin-Blockchain-Transaction-Discovery/master/transaction_collection.png)
![alt text](https://raw.githubusercontent.com/nilan3/Bitcoin-Blockchain-Transaction-Discovery/master/transaction_discovery.png)

If you wish to include more deposit addresses for monitoring new transactions, update list in config `configurations/transaction_collection.yml` and restart transaction collection spark job

```bash
docker restart bitcoin-blockchain-transaction-discovery_spark_transaction_collection_1
```
