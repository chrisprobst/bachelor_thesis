def setup(command, results_dir):
    # Size and duration
    upload_rate = 1024 * 16
    time_per_transfer = 60 * 10

    # Peers
    super_seeders = 1
    peers = 192

    # Data info
    chunk_count_factor = 2
    parts = 1

    # Network and distribution
    meta_size = 1
    algorithm = "SuperSeederChunkedSwarm"

    return command[
        '-rd', str(results_dir),
        '-ss', str(super_seeders),
        '-sl', str(peers - super_seeders),
        '-s', str(upload_rate * time_per_transfer),
        '-cc', str((peers - super_seeders) * chunk_count_factor),
        '-mds', str(meta_size),
        '-p', str(parts),
        '-pt', 'Local',
        '-at', str(algorithm),
        '-rs',
        '-u', str(upload_rate),
        '-su', str(upload_rate),
    ]