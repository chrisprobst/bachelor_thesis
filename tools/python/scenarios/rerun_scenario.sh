#!/bin/sh
echo $1



a=$(find results/scenario_$1_*/mean/* -maxdepth 0)
b=$(find ../../gnuplot/img/*eps -maxdepth 0)
c=$(find results/scenario_$1_*/plots -maxdepth 0)

echo "From "$a" to results"
echo "From "$b" to "$c
cp $a "results" && ./plot.sh && cp $b $c