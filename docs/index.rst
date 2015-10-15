.. Discora documentation master file

=========================================
Discora - Distributed Conflict Resolution
=========================================

Discora (Distributed Conflict Resolution Architecture) is a massively distributed computing architecture used to implement the conflict avoidance algorithm detailed `here <http://web.stanford.edu/~haoyi/projects/short-term-conf-reso.pdf>`_ on a practical scale. The architecture is designed to work with different conflict avoidance algorithms (or policies), and we provide a simple and convenient entry point for users to modify our code and test their own work.

In Depth Documentation
======================

.. toctree::
  :maxdepth: 2

  Installation <installation>
  Running Kafka <kafka>
  Running Spark <spark>
  Policy Entry Point <policy>
