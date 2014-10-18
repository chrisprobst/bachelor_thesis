def setup(command, results_dir):
    return command[
        '-rd', str(results_dir),
        '-ss', 1,
        '-sl', str(100),
        '-s', str(600000),
        '-cc', str(200),
        '-p', str(1),
        '-pt', 'Local',
        '-at', 'SuperSeederChunkedSwarm',
        '-rs',
        '-u', str(10000),
        '-su', str(10000),
    ]