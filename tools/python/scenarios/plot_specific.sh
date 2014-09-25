#!/bin/sh

BASEDIR="../python/scenarios/results/"
SCENARIO=$1
DIR=$2
BASEDIR=$BASEDIR$SCENARIO'/'$DIR'/'

cd ../../gnuplot
pwd
echo $BASEDIR
gnuplot -e "dir='$BASEDIR'" plot.cfg