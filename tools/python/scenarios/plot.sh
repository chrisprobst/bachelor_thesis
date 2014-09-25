#!/bin/sh

cd ../../gnuplot
pwd
gnuplot -e "prefix='SuperSeederChunkedSwarmBenchmark'; dir='../python/scenarios/results/'" plot.cfg