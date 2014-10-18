def setup(command, results_dir):
    return command[
        '-rd', str(results_dir),
        '-ss', 1,
        '-sl', str(50),
        '-s', str(600000),
        '-cc', str(100),
        '-p', str(12),
        '-pt', 'Local',
        '-at', 'SuperSeederChunkedSwarm',
        '-rs',
        '-u', str(10000),
        '-su', str(10000),
    ]