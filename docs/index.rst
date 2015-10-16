.. Discora documentation master file

=========================================
Discora - Distributed Conflict Resolution
=========================================

Discora (Distributed Conflict Resolution Architecture) is a distributed computing architecture used to implement the conflict avoidance algorithm detailed `here <http://web.stanford.edu/~haoyi/projects/short-term-conf-reso.pdf>`_ on a practical scale.

Discora is implemented using Apache Spark and Apache Kafka in Scala, but we've made it so you don't have to worry too much about these APIs. In fact, the architecture is designed to be modular such that you can swap out our conflict avoidance algorithm for your own. In this documentation, we provide a convenient entry point for users to replace our algorithm and test their own work using Discora.

In Depth Documentation
======================

.. toctree::
  :maxdepth: 2

  Installation <installation>
  Running Kafka <kafka>
  Running Spark <spark>
  Policy Entry Point <policy>
