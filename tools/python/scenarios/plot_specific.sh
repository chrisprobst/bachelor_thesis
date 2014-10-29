#!/bin/sh

echo $1
echo $2
echo $3

BASEDIR="../python/scenarios/results/"
SCENARIO=$1
DIR=$2
SCRIPT=$3
BASEDIR=$BASEDIR$SCENARIO'/'$DIR'/'

cd ../../gnuplot
pwd
echo $BASEDIR
gnuplot -e "dir='$BASEDIR'" "plot_specific_$SCRIPT.cfg"

open img