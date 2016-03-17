# Discra

This repository contains an implementation of the conflict resolution portion of the UAS Traffic Management project using Discra (Distributed Conflict Resolution Architecture). While this work uses ideas developed in the paper titled "Short-Term Conflict Avoidance for Unmanned Aircraft Traffic Management" by Hao Yi Ong and Mykel J. Kochenderfer (see [our paper](http://haoyi.io/projects/short-term-conf-reso.pdf)), the focus here is to create an efficient system to handle a large volume of conflict resolution requests in a distributed streaming setting.

## Abstract

Ensuring safety and providing timely conflict alerts to small unmanned aircraft is important to their integration into civil airspace. We propose several algorithms for short-term conflict avoidance as part of an automated traffic management system. The goal is to balance aircraft safety and efficiency subject to environmental and aircraft uncertainty. The proposed controllers generate advisories for each aircraft tofollow, and are based on decomposing a large Markov decision process and fusing their solutions. We separate the problem into pairwise encounters that are solved offline. The solutions to these encounters are then combined online to produce a locally optimal solution using an iterative search technique. As a result, the methods scale well and the global problem can be solved efficiently. We demonstrate the controllers by evaluating them against baseline algorithms in simulation.

## Documentation

Detailed documentation for Discra and instructions on how to modify the conflict resolution algorithm implemented in the architecture can be found [here](http://discra.readthedocs.org/en/latest/).

This implementation is completely local and does not include code that talks to the UTM client server. If that is the intent of the user, please contact the authors.

# Citing this work

If you use Discra for published work, we encourage you to cite the software using the following BibTex citation:

    @InProceedings{ong2015,
        Title = {Short-term conflict resolution for unmanned aircraft traffic management},
        Author = {Ong, Hao Yi and Kochenderfer, Mykel J.},
        Booktitle = {IEEE/AIAA Digital Avionics Systems Conference},
        Year = {2015}
    }
