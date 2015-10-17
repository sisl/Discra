==================
Policy Entry Point
==================

A key point of the Discora architecture is to provide a convenient framework for implementing and testing different conflict resolution algorithms. In fact, the same architecture could be used for other "derived" UTM services, such as trajectory planning and traffic flow management services for clients that might not have flight optimization capabilities.

``Policy`` in Discora
=====================

``Policy`` is a class in Discora that implements the conflict resolution algorithm. Once instantiated, the ``Policy`` object is broadcasted by the advisor server's Spark driver to its worker nodes and used to resolve conflicts.

Interaction points::

  * Policy.defaultPolicy()
  * Policy.advisories(conflict: Array[(gufi: String, drone: DroneGlobalState, advisory: Double)])
