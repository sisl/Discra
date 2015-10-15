==================
Problem definition
==================

MDP
===

In a Markov decision process (MDP), an agent chooses action *a* based on observing state *s*. The agent then receives a reward *r*. The state evolves stochastically based on the current state and action taken by the agent. Note that the next state depends only on the current state and action, and not on any prior state or action. This assumption is known as the Markov assumption.

An MDP is typically defined by the tuple *(S, A, T, R)*, where

* *S* is the state space
* *A* is the action space
* *T(s, a, s')* is the transition function that gives the probability of reaching state *s*' from state s by taking action *a*
* *R(s, a)* is the reward function that gives a scalar value for taking action *a* at state *s*.

To define an MDP, simply type the following.

::

  mdp = MDP()

State space
===========

The state space *S* is the set of all states, and it can be continuous and thus infinite. PLite provides the ability to define states simply as a set of all states or in a factored representation.

To define *S* the latter way, simply define the range or the array of possible values for the state variable. For instance, to define range of values for the continuous state variable *x* from 0 to 100, we simply type the following.

::

  statevariable!(mdp, "x", 0, 100)

Note that we stringified *x* as ``"x"``. Internally, this syntax allows ``mdp`` to keep an internal representation of the variable.

At this point, we may be tempted to discretize the variable and use something like value iteration to solve the MDP. The reason we don't provide the discretization option is because discretization is an approximation technique that should be considered together with the solution method. Yes, you may decide to discretize it an input the discretized values as an array input (see below). But we emphasize that at this point, we're only worried about defining a mathematical formulation of the problem.

To define a set of discrete values for a discrete state variable *direction* that can take on the values *north*, *south*, *east*, and *west*, we simply type the following.

::

  statevariable!(mdp, "direction", ["north", "south", "east", "weast"])

If you made a mistake, as we did above with the spelling of *west*, we can override the existing definition of the state variable *direction* by redefining it as follows.

::

  statevariable!(mdp, "direction", ["north", "south", "east", "west"])

PLite will provide a warning whenever you redefine a previously named variable.

To define *S* as a set of all states, we can simply define a single discrete state variable and type the following.

::

  statevariable!(mdp, "S", ["v1", "v2", "v3", "v4", "v5", "v6", "v7"])

Note that ``"S"`` is not a special keyword. It's just a state variable like any other, and you'll have to take care to treat it as a state variable in defining the transition and reward functions. The nice thing is PLite allows you to work with both factored and unfactored MDP state space representations.

Action space
============

The action space *A* is the set of all actions. Like *S*, it can be continuous and thus infinite. The definition of action variables follow that of state variables, which means that PLite allows both factored and unfactored action space definitions.

To define a continuous and a discrete action variable *bankangle* and *move*, respectively, we type

::

  actionvariable!(mdp, "bankangle", -180, 180)
  actionvariable!(mdp, "move", ["up", "down", "left", "right"])

Transition
==========

PLite provides two ways to define a transition model. Depending on the problem, it may be easier to define the transition model one way or another.

*T(s, a, s')* type transition
-----------------------------

The first method follows the standard definition that returns a probability value between 0 and 1. Suppose we have the following definition of an MDP.

::

  # constants
  const MinX = 0
  const MaxX = 100

  # mdp definition
  mdp = MDP()

  statevariable!(mdp, "x", MinX, MaxX)  # continuous
  statevariable!(mdp, "goal", ["yes", "no"])  # discrete

  actionvariable!(mdp, "move", ["W", "E", "stop"])  # discrete

We want to define a transition function that takes a state-action-next state triplet *(s, a, s')* and returns the probability of starting in state *s*, taking action *a*, and ending up in state *s*'. Internally, PLite needs to match the defined state and action variables with the corresponding arguments for the transition function. To do this, we need to pass in an array of the argument names in the order they are to be input to the defined transition function.

An example of a *T(s, a, s')* type transition function is as follows. Here, the state variables are named ``"x"`` and ``"goal"``, and the action variable is named ``"move"``. Note that although *s*' is a different variable from *s*, they share the variable names ``"x"`` and ``"goal"``. So even though the ``mytransition`` function signature is

::

  function mytransition(
      x::Float64,
      goal::String,
      move::String,
      xp::Float64,
      goalp::String)

the array of (ordered) argument names is ``["x", "goal", "move", "x", "goal"]`` rather than ``["x", "goal", "move", "xp", "goalp"]``. Below is the full listing that defines the transition for ``mdp``.

::

  transition!(mdp,
    ["x", "goal", "move", "x", "goal"],  # note |xp| is an "x" variable
                                         # note (s,a,s') order
    function mytransition(
        x::Float64,
        goal::String,
        move::String,
        xp::Float64,
        goalp::String)

      function internaltransition(x::Float64, goal::String, move::String)
        function isgoal(x::Float64)
          if abs(x - MaxX / 2) < StepX
            return "yes"
          else
            return "no"
          end
        end

        if isgoal(x) == "yes" && goal == "yes"
          return [([x, isgoal(x)], 1.0)]
        end

        if move == "E"
          if x >= MaxX
            return [
              ([x, isgoal(x)], 0.9),
              ([x - StepX, isgoal(x - StepX)], 0.1)]
          elseif x <= MinX
            return [
              ([x, isgoal(x)], 0.2),
              ([x + StepX, isgoal(x + StepX)], 0.8)]
          else
            return [
              ([x, isgoal(x)], 0.1),
              ([x - StepX, isgoal(x - StepX)], 0.1),
              ([x + StepX, isgoal(x + StepX)], 0.8)]
          end
        elseif move == "W"
          if x >= MaxX
            return [
              ([x, isgoal(x)], 0.1),
              ([x - StepX, isgoal(x - StepX)], 0.9)]
          elseif x <= MinX
            return [
            ([x, isgoal(x)], 0.9),
            ([x + StepX, isgoal(x + StepX)], 0.1)]
          else
            return [
              ([x, isgoal(x)], 0.1),
              ([x - StepX, isgoal(x - StepX)], 0.8),
              ([x + StepX, isgoal(x + StepX)], 0.1)]
          end
        elseif move == "stop"
          return [([x, isgoal(x)], 1.0)]
        end
      end

      statepprobs = internaltransition(x, goal, move)
      for statepprob in statepprobs
        if xp == statepprob[1][1] && goalp == statepprob[1][2]
          return statepprob[2]
        end
      end
      return 0

    end
  )

*T(s, a)* type transition
-------------------------

The second way to define a transition model is to take in a state-action pair and return the set of all possible next states with their corresponding probabilities. Again, we need to pass an array of argument names in the order the *(s, a)* pair is defined to the transition function. Below is the full listing that defines the transition this way. It is mathematically equivalent to the *T(s, a, s')* type transition defined above.

::

  transition!(mdp,
    ["x", "goal", "move"],
    function mytransition(x::Float64, goal::AbstractString, move::AbstractString)
      function isgoal(x::Float64)
        if abs(x - MaxX / 2) < StepX
          return "yes"
        else
          return "no"
        end
      end

      if isgoal(x) == "yes" && goal == "yes"
        return [([x, isgoal(x)], 1.0)]
      end

      if move == "E"
        if x >= MaxX
          return [
            ([x, isgoal(x)], 0.9),
            ([x - StepX, isgoal(x - StepX)], 0.1)]
        elseif x <= MinX
          return [
            ([x, isgoal(x)], 0.2),
            ([x + StepX, isgoal(x + StepX)], 0.8)]
        else
          return [
            ([x, isgoal(x)], 0.1),
            ([x - StepX, isgoal(x - StepX)], 0.1),
            ([x + StepX, isgoal(x + StepX)], 0.8)]
        end
      elseif move == "W"
        if x >= MaxX
          return [
            ([x, isgoal(x)], 0.1),
            ([x - StepX, isgoal(x - StepX)], 0.9)]
        elseif x <= MinX
          return [
          ([x, isgoal(x)], 0.9),
          ([x + StepX, isgoal(x + StepX)], 0.1)]
        else
          return [
            ([x, isgoal(x)], 0.1),
            ([x - StepX, isgoal(x - StepX)], 0.8),
            ([x + StepX, isgoal(x + StepX)], 0.1)]
        end
      elseif move == "stop"
        return [([x, isgoal(x)], 1.0)]
      end
    end
  )

Reward
======

The reward function takes in a state-action pair *(s, a)* and returns a scalar value indicating the expected reward received when executing action *a* from state *s*. We assume that the reward function is a deterministic function of *s* and *a*.

The process of defining the reward function is similar to that for the *T(s, a)* type transition function. We need to pass in an ordered array of variable names for PLite's internal housekeeping.

::

  reward!(mdp,
    ["x", "goal", "move"],  # note (s,a) order
                            # note consistency of variables order with transition
    function myreward(x::Float64, goal::String, move::String)
      if goal == "yes" && move == "stop"
        return 1
      else
        return 0
      end
    end
  )
