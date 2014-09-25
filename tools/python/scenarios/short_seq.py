def setup(command, results_dir):
    return command[
        '-rd', results_dir,
        '-s', 1,
        '-l', 2,
        '--total-size', 10000,
        '-c', 1,
        '-pt', 'Local',
        '-da', 'Sequential',
        '-rs',
        '-u', 1000,
        '-su', 1000
    ]