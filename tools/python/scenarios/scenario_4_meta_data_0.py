def setup(command, results_dir):
    upload_rate = 10000
    time_per_transfer = 60 * 10
    peers = 100
    chunk_count_factor = 2
    meta_size = 0
    parts = 1

    return command[
        '-rd', str(results_dir),
        '-ss', 1,
        '-sl', str(peers),
        '-s', str(upload_rate * time_per_transfer),
        '-cc', str(peers * chunk_count_factor),
        '-mp', str(meta_size),
        '-p', str(parts),
        '-pt', 'Local',
        '-at', 'SuperSeederChunkedSwarm',
        '-rs',
        '-u', str(upload_rate),
        '-su', str(upload_rate),
    ]