=======================
Monte-Carlo Tree Search
=======================

The Monte-Carlo tree search (MCTS) algorithm relies on the same problem definition framework as the value iteration algorithms. Like value iteration, MCTS works by keeping an internal approximation of the *Q(s, a)* state-action utility and chooses the action that maximizes this state-action utility. Unlike value iteration, however, MCTS is an online algorithm. This means that the MCTS policy may start off poor, but it gets better the more it interacts with the MDP simulator/environment.

Note that a key assumption is that both the action space and the state space are finite. Otherwise, we will keep selecting unexplored actions, and no node of depth higher than one would be added. Thus, after the state and action spaces are discretized, the algorithm only works with these discrete states and actions. For the transition function in our implementation, we will map the given current state and action pair to the closest discrete state and action. Additionally, the next state transitioned into (via sampling) will also be mapped to the closest discrete state.

The main advantage to MCTS is its ability to give a good approximation of the state-action utility function despite not needing an expensive value iteration-type computation. We recommend using this for problems with large state and/or action spaces.

The syntax for using a serial MCTS solver is similar to that of the serial value iteration solver. We still need to discretize continuous variables since our solver implements the finite MCTS. Otherwise, the only difference is having to initialize a different type of solver.

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
  solver = SerialMCTS()
  discretize_statevariable!(solver, "x", StepX)

  # generate results
  solution = solve(mdp, solver)

There are four keyword arguments we can use to instantiate the solver: ``niter``, ``maxdepth``, ``exex``, and ``discount``. These parameters correspond to the number of iterations during each action selection when querying the MCTS policy object (see more in the [Solution](#solution) section), the maximum depth of the search tree used in MCTS, a constant that varies the exploration-exploitation preference, and the simulation/rollout discount factor, respectively.

The default parameters are

* ``niter = 50``
* ``maxdepth = 20``
* ``exex = 3.0``
* ``discount = 0.99``.