def setup(command, results_dir):
    return command[
        '-rd', results_dir,
        '-s', 1,
        '-l', 10,
        '--total-size', 100000,
        '-c', 200,
        '-pt', 'Local',
        '-da', 'SuperSeederChunkedSwarm',
        '-rs',
        '-u', 1000,
        '-su', 1000
    ]