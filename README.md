# Hogwild
Hogwild! implementation : "A Lock-Free Approach to Parallelizing Stochastic Gradient Descent"
Team : Grégoire Clément, Maxime Delisle, Sylvain Beaud

## Description
Data should be placed in the root of the project in a "data" folder.
At the moment, as we are working on our machines (laptops), we are only loading a subset of the data in memory (which is in the file "samples.dat").
Otherwise, keep the files (vectors and labels) named as on the website linked below.

We have added a JPG file which simply represents our design choices so far. 
This sketch does NOT fully detail our implementation but gives a quick overview of it.
(Design_Milestone1.jpeg)

## How to run the project
1. Use SBT to build and compile the project
2. Run the Master
3. Wait for the Master to be ready (std_output)
4. Run as many Slaves as you want


![Design_Milestone1.jpeg][design]
[Dataset available here][dataset]

[design]: https://imgur.com/R7I3OYk.jpg
[dataset]: http://www.ai.mit.edu/projects/jmlr/papers/volume5/lewis04a/lyrl2004_rcv1v2_README.htm