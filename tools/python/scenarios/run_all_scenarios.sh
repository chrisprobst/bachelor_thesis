#!/bin/sh

# Start all scenarios
# ------------------------------------------------------
# Duration:
# Each scenario takes about 10 - 20 minutes and repeats 10 times.
# Since there are 5 scenarios, the whole benchmark takes about
# 500 - 1000 minutes, which is about 8 - 16 hours.
#
# ------------------------------------------------------
# In case of failure:
# Failures should not occur, if so, there is something heavily
# broken. But IF a failure occurs, the previously collected results
# are not gone.
#
python run.py -s scenario_1_default         -n 10
python run.py -s scenario_3_chunk_count_1   -n 10
python run.py -s scenario_4_meta_data_0     -n 10
python run.py -s scenario_5_parts_20        -n 10
python run.py -s scenario_2_peer_count_200  -n 10


