def setup(command, results_dir):
    return command[
        '-rd', results_dir,
        '-s', 1,
        '-l', 16,
        '--total-size', 5000,
        '-c', 1,
        '-pt', 'Local',
        '-da', 'Logarithmic',
        '-rs',
        '-u', 1000,
        '-su', 1000
    ]