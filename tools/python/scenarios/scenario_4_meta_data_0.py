def setup(command, results_dir):
    upload_rate = 10000
    time_per_transfer = 60 * 10
    peers = 50
    chunk_count_factor = 2
    meta_size = 0
    parts = 1

    return command[
        '-rd', results_dir,
        '-ss', 1,
        '-sl', peers,
        '-s', upload_rate * time_per_transfer,
        '-cc', peers * chunk_count_factor,
        '-mp', meta_size,
        '-p', parts,
        '-pt', 'Local',
        '-at', 'SuperSeederChunkedSwarm',
        '-rs',
        '-u', upload_rate,
        '-su', upload_rate
    ]