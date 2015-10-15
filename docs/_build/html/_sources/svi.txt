======================
Serial Value iteration
======================

PLite implements the value iteration algorithm for infinite horizon problems with discount, but we demonstrate how to hack the existing algorithm to solve finite horizon problems with no discount.

Infinite horizon with discount
==============================

To initialize the serial value iteration solver, simply type the following.

::

  solver = SerialValueIteration()

In PLite, value iteration requires all variables to be discretized. In the above problem, we need to discretize ``"x"``, so we write

::

  const StepX = 20
  discretize_statevariable!(solver, "x", StepX)

Note that the solver uses the ``GridInterpolations.jl`` package for multilinear interpolation to approximate the values between the discretized state variable values if the *T(s, a)* type transition is defined. In the *T(s, a, s')* type transition, PLite assumes that for any *(s, a, s')* triplet the transition function will return a valid probability. In this case, the user is assumed to have defined a consistent MDP problem and no approximation is done on the part of PLite.

In any case, to solve the problem, simply pass both ``mdp`` and ``solver`` to the ``solve`` function.

::

  solution = solve(mdp, solver)

Finite horizon without discount
===============================

Notice that both the serial and parallel value iteration solvers are built with infinite horizon problems in mind. It's easy, however, to modify it to solve finite horizon problems by simply changing the parameters of the solvers.

For an MDP with a horizon of 40 and no discounting, we can define the solver as follows.

::

  solver = SerialValueIteration(maxiter=40, discount=1)

There are three parameters for the ``SerialValueIteration`` solver: ``maxiter``, ``tol``, and ``discount``. As their names suggest, these parameters correspond to the maximum number of iterations the value iteration algorithm will run before timing out, the L-infinity tolerance for Q-value convergence between iterations, and the discount factor, respectively.

The default parameters are

* ``maxiter = 1000``
* ``tol = 1e-4``
* ``discount = 0.99``
* ``verbose = true``.

To change these parameters, we use keyword arguments when instantiating the solver object. For instance, we can define the following.

::

  solver = SerialValueIteration(
    tol=1e-6,
    maxiter=10000,
    discount=0.999,
    verbose=false)

Note that because we're using keyword arguments (i.e., ``keyword=value`` type arguments), we can input the arguments in any way we want.

Example
-------

Below are two comprehensive listings that define the simple MDP example given above, select their solvers, and extract their solutions in the form of policy functions.

  * MDP with *T(s, a, s')* type transition

::

  using PLite

  # constants
  const MinX = 0
  const MaxX = 100
  const StepX = 20

  # mdp definition
  mdp = MDP()

  statevariable!(mdp, "x", MinX, MaxX)  # continuous
  statevariable!(mdp, "goal", ["yes", "no"])  # discrete

  actionvariable!(mdp, "move", ["W", "E", "stop"])  # discrete

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

  # solver options
  solver = SerialValueIteration()
  discretize_statevariable!(solver, "x", StepX)

  # generate results
  solution = solve(mdp, solver)

  * MDP with *T(s, a)* type transition

::

  using PLite

  # constants
  const MinX = 0
  const MaxX = 100
  const StepX = 20

  # mdp definition
  mdp = MDP()

  statevariable!(mdp, "x", MinX, MaxX)  # continuous
  statevariable!(mdp, "goal", ["no", "yes"])  # discrete

  actionvariable!(mdp, "move", ["W", "E", "stop"])  # discrete

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

  reward!(mdp,
    ["x", "goal", "move"],
    function myreward(x::Float64, goal::String, move::String)
      if goal == "yes" && move == "stop"
        return 1
      else
        return 0
      end
    end
  )

  # solver options
  solver = SerialValueIteration()
  discretize_statevariable!(solver, "x", StepX)

  # generate results
  solution = solve(mdp, solver)
