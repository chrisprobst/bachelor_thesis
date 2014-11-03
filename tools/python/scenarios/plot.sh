#!/bin/sh

cd ../../gnuplot
pwd

gnuplot -e "dir='../python/scenarios/results/'" plot.cfg

