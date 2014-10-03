def setup(command, results_dir):
    return command[
        '-rd', results_dir,
        '-s', 1,
        '-l', 10,
        '--total-size', 2000000,
        '-c', 40,
        '--parts', 20,
        '-pt', 'Local',
        '-da', 'SuperSeederChunkedSwarm',
        '-rs',
        '-u', 1000000,
        '-su', 1000000
    ]