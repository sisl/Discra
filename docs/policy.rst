==================
Policy Entry Point
==================

A key point of the Discra architecture is to provide a convenient framework for implementing and testing different conflict resolution algorithms. In fact, the same architecture could be used for other "derived" UTM services, such as trajectory planning and traffic flow management services for clients that might not have flight optimization capabilities.

Policy in Discra
================

``Policy`` is a class in Discra that implements the conflict resolution algorithm. Once instantiated, the ``Policy`` object is broadcasted by the advisor server's Spark driver to its worker nodes and used to resolve conflicts.

Interaction points::

  Policy.defaultPolicy(): Policy
  Policy.advisories(conflict: Array[(gufi: String, drone: DroneGlobalState, advisory: Double)]): List[jsonAdvisory :String]
  searchPolicy(drones: Array[DroneGlobalState]): (Array[Double], Double, Int)

