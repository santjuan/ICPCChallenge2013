ICPCChallenge2013
=================

Implementation of the ICPC Challenge player that got 3rd place this year (http://icpc.baylor.edu/challenge/).


The most important part of the player was the game simulator, which allowed us to build a brute force defense system. What that brute force system did was basically trying all possible moves up to a certain depth to see if the player was in danger, and if it was it tried to find a saving move.
The other part of the system was the gold advisor, which decided in which order the player would take the golds. Several algorithms were tried and the one that was submitted at the end was one which computed the TSP of the N nearest golds, with N depending on the distance to the nearest gold (so if the nearest gold was too far, the TSP would be computed with a lot of golds, and if it was close, with only 3 golds).
On the last days before the deadline, we implemented a machine learning system using neural networks. Since there was little time, the training was done on an already existing octave code. Despite being very promising, it did not manage to beat the TSP system on maps which were not part of the traning, so we didn't submit it. 
In the actual competition, and contrary to our expectations, the training maps were used in almost all matches, so it would have been very good for the machine learning system :(

The machine learning classes are those starting with ML. There are several implementations of gold advisors in the source code.

The Java code was written by me (Santiago Gutierrez), and the other two members of the team contributed with ideas about the implementation and the octave machine learning code (which is in the learning folder).

The code is not very organized because it was all written in a period of 15 days, so I had to code fast. I hope you can understand it!
