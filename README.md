# Discra

This repository contains an implementation of the conflict resolution portion of the UAS Traffic Management project using Discra (Distributed Conflict Resolution Architecture). While this work uses ideas developed in the paper titled "Short-Term Conflict Avoidance for Unmanned Aircraft Traffic Management" by Hao Yi Ong and Mykel J. Kochenderfer (see [our paper](http://haoyi.io/projects/short-term-conf-reso.pdf)), the focus here is to create an efficient system to handle a large volume of conflict resolution requests in a distributed streaming setting.

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
