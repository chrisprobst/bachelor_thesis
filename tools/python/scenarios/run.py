import argparse
import importlib
from sys import stdout as stdout
import os.path as path
from shutil import rmtree

from tools import trans
from plumbum import local


def start_process(process):
    for line in process.stderr:
        stdout.write(line.decode('utf-8'))
    for line in process.stdout:
        stdout.write(line.decode('utf-8'))
    process.wait()


def main():
    parser = argparse.ArgumentParser(description='Runs benchmark scenarios')
    parser.add_argument('--simulate', action='store_true', dest='simulate')
    parser.add_argument('-s', required=True, dest='s')
    parser.add_argument('-n', required=True, dest='n', type=int)
    parser.add_argument('-p', dest='p', action='store_true')
    parser.set_defaults(p=False)
    parser.set_defaults(simulate=False)
    args = parser.parse_args()

    number, script, parallel = args.n, args.s, args.p
    assert number > 0

    if not args.simulate:
        mod = importlib.import_module(script)
        command = local['java'][
            '-Dorg.slf4j.simpleLogger.defaultLogLevel=warn',
            '-XX:+AggressiveHeap',
            '-XX:+UseParallelGC',
            '-XX:+AggressiveOpts',
            '-jar', '../../../build/libs/benchmark-1.0.jar'
        ]
        make_app = lambda idx: mod.setup(command, path_maker(idx))
    else:
        make_app = lambda idx: None

    def path_maker(k):
        return path.join('.', 'results', script, str(k))

    apps = [(path_maker(i), make_app(i)) for i in range(number)]

    if not args.simulate:
        try:
            # Make sure that all old results are removed
            rmtree(path.join('.', 'results', script))
        except FileNotFoundError:
            pass

        if parallel:
            print('Started all iterations in parallel')
            for idx, (k, process_handle) in enumerate([(k, app.popen()) for k, app in apps]):
                print('Waiting for %i. iteration at %s' % (idx, k))
                start_process(process_handle)
        else:
            for idx, (k, app) in enumerate(apps):
                print('Running %i. iteration at %s' % (idx, k))
                start_process(app.popen())

    #######################
    ## CREATE MISSING FILES
    #######################

    #### Sorted peer bandwidth

    # Read the total matrices
    total_uploaded_matrices = trans.read_all_matrices((k for k, _ in apps),
                                                      lambda x: x == 'TotalUploadedBandwidth.csv')
    total_downloaded_matrices = trans.read_all_matrices((k for k, _ in apps),
                                                        lambda x: x == 'TotalDownloadedBandwidth.csv')

    # Create sorted total uploaded
    for run, subresults in total_uploaded_matrices.items():
        for inputpath, matrix in subresults.items():
            column_data = [['SortedPeers', 'TotalUploaded']] + list(
                enumerate(sorted(map(float, matrix[-1][1:]), reverse=True)))
            trans.write_matrix(column_data, path.join(run, 'Sorted' + inputpath))

    # Create sorted total downloaded
    for run, subresults in total_downloaded_matrices.items():
        for inputpath, matrix in subresults.items():
            column_data = [['SortedPeers', 'TotalDownloaded']] + list(
                enumerate(sorted(map(float, matrix[-1][1:]), reverse=True)))
            trans.write_matrix(column_data, path.join(run, 'Sorted' + inputpath))


    #### Sorted peer completion time
    chunk_completion_matrices = trans.read_all_matrices((k for k, _ in apps),
                                                        lambda x: x == 'ChunkCompletion.csv')

    # Create sorted chunk completion
    for run, subresults in chunk_completion_matrices.items():
        for inputpath, matrix in subresults.items():
            max_completion = float(matrix[-1][1])

            column_data = []
            done = set()
            for i in range(1, len(matrix)):
                for j in range(1, len(matrix[i])):
                    if j not in done and float(matrix[i][j]) == max_completion:
                        done.add(j)
                        column_data.append(int(round(float(matrix[i][0]))))

            column_data = [['SortedPeers', 'Time']] + list(zip(range(len(column_data)), reversed(column_data)))
            trans.write_matrix(column_data, path.join(run, 'Sorted' + inputpath))


    ############################
    ## START THE MEAN-CALC CHAIN
    ############################

    # Read all files as mean
    results = trans.read_all_mean(k for k, _ in apps)

    # Write single matrices
    for run, subresults in results.items():
        for inputpath, matrix in subresults.items():
            trans.write_matrix(matrix, path.join(run, trans.GENERATED_PREFIX + 'Mean' + inputpath))

    if len(results) > 1:
        d = next(iter(results.values()))
        for key in d:
            matrix = trans.get_mean_of_results(results, key)
            trans.write_matrix(matrix, path.join('.', 'results', trans.GENERATED_PREFIX + 'Mean' + key))
    else:
        print("Only one run, no mean values will be calculated")


if __name__ == '__main__':
    main()