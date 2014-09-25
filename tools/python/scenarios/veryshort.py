def setup(command, results_dir):
    return command[
        '-rd', results_dir,
        '-s', 1,
        '-l', 4,
        '--total-size', 2000,
        '-c', 8,
        '-pt', 'Local',
        '-da', 'SuperSeederChunkedSwarm',
        '-rs',
        '-u', 1000,
        '-su', 1000
    ]