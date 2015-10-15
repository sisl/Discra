================
Solver selection
================

PLite is intended to provide several solvers for any MDP problem, ranging from classic dynamic programming methods such as value iteration, policy iteration, and policy evaluation to approximate, online, and direct policy search methods. Eventually, we'll include learning algorithms, including cooler ones like `deep reinforcement learning with double Q-learning <http://arxiv.org/abs/1509.06461>`_ with support for `distributed computing <http://arxiv.org/abs/1507.04296>`_.

Until then, we just have to play with good o' value iteration in both its serial and parallel flavors, as well as serial implementation of the finite Monte-Carlo tree search algorithm.

Solvers
=======

.. toctree::
  :maxdepth: 1

  Serial Value Iteration <svi>
  Parallel Value Iteration <pvi>
  Serial Monte-Carlo Tree Search <smcts>
