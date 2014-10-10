def setup(command, results_dir):
    return command[
        '-rd', results_dir,
        '-ss', 1,
        '-sl', 10,
        '-s', 40000,
        '-cc', 200,
        '-pt', 'Local',
        '-at', 'SuperSeederChunkedSwarm',
        '-rs',
        '-u', 1000,
        '-su', 1000
    ]