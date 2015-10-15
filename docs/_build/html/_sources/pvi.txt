========================
Parallel Value Iteration
========================

To reap the benefits of Julia's parallel computing framework for value iteration, we need a few more steps. The main issue we have to get around is code availability when we add processes. But we'll skip an in-depth explanation and just go straight to what we can do.

We consider a quick and dirty example of running the exact same code as in the MDP with *T(s, a)* type transition on PLite's parallel value iteration solver. First, we wrap our existing code under the module ``ExampleModule`` (you can name it whatever you want), and save it under the file name ``ExampleModule.jl``. As our naming scheme suggests, the module and file should share the same name. Below is what should be saved to the file.

::

  module ExampleModule

  export
    mdp,
    solver,
    solve,
    getpolicy

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
  solver = ParallelValueIteration()
  discretize_statevariable!(solver, "x", StepX)

  end

On top of the keyword arguments available to ``SerialValueIteration``, ``ParallelValueIteration`` has an additional ``nthreads`` keyword argument. The default value is ``CPU_CORES / 2``.

``CPU_CORES`` is a Julia standard library constant, and it defaults to the number of CPU cores in your system. But the number of cores given usually includes virtual cores (e.g., Intel processors), so we divide by two to obtain the number of physical cores. There isn't an issue with increasing the number of cores. But since we have the same number of cores doing the same number of work, there won't be an increase in efficiency. In fact, with greater number of threads there may be more overhead and runtime processes. As such, we recommend using as many threads as there are physical cores on the machine. In the case of the parallel solver, we can define

::

  solver = ParallelValueIteration(
    tol=1e-6,
    maxiter=10000,
    discount=0.999,
    verbose=false,
    nthreads=10)

As in the serial solver, PLite needs a definition of the discretization scheme.

Notice that there are two modifications to the code being wrapped (in addition to putting it in ``ExampleModule`` and using ``ParallelValueIteration``):

1. we removed the ``solve`` bit that generated the solution
2. we added the ``export`` keyword that makes the objects and functions available to the user (either in on the console or the Jupyter notebook)

::

  export
    mdp,
    solver,
    solve,
    getpolicy

On the console or Jupyter notebook, we then input the following.

::

  const NThreads = int(CPU_CORES / 2)
  addprocs(NThreads - 1)  # -1 to account for existing process

  using ExampleModule

  # generate results
  solution = solve(mdp, solver)

Notice we add the desired number of processes before loading the module. This sequence of code evaluation allows all processes to get the code on ExampleModule. We then call ``solve`` on the mdp and solver to obtain the solution.
