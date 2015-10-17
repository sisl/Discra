============
Kafka Server
============

Kafka in Discra
===============

The advisories publish-subscribe server and the internal conflict message relay server between the ingestor and advisor servers are implemented using Kafka. Designed as a unified, high-throughput, low-latency platform for handling real-time data feeds, Kafka is an open-source project with active industry use. In a distributed message broker, producers publish data tagged by *topics* and consumers subscribe to data of a particular topic.

This abstraction facilitates simple separation of data for different processes running in parallel. In our advisories publish-subscribe server, GUFI-advisory pair messages are tagged to airspace regions—the relevant topic on which clients listen to. (An equally valid alternative is tagging advisory messages to unique client identifiers.) The internal conflict message relay server simply uses a single topic from which all advisor worker nodes draw formatted JSON conflict messages from.

For this repository's example, the simulator server publishes to the `status` topic and listens to the `advisory` topic, the ingestor server publishes to the `conflict` topic and listens to the `status` topic, and the advisor server publishes the `advisory` topic and listens to the `conflict` topic. We thus run only three topics: `status`, `conflict`, and `advisory`.

Running Kafka
=============

We assume you are starting fresh and have no existing Kafka or ZooKeeper data. Kafka uses ZooKeeper so start a ZooKeeper server if you don't already have one. On Mac OSX, there is a set of convenient scripts packaged with Kafka that we can use to run a quick-and-dirty single-node ZooKeeper instance. We provide the instructions for Mac OSX here, but there are equivalent scripts for Windows located under a subdirectory in the binaries folder.

ZooKeeper
---------

From the Kafka root, run

::

  $ bin/zookeeper-server-start.sh config/zookeeper.properties

Kafka
-----

Now, start the Kafka server by running

::

  $ bin/kafka-server-start.sh config/server.properties

Topics
------

If the topic to consume from has not been initialized, create a topic named "test" with a single partition and only one replica as follows.

::

  $ bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test

Console Consumer
----------------

Kafka has a command line consumer that will dump out messages to standard output. You can input the following to verify that messages are being sent. Remember to replace `<topic>` with the topic name you want to listen to.

::

  $ bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic <topic>
