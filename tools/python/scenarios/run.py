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

    mod = importlib.import_module(script)
    log_level = 'org.slf4j.simpleLogger.defaultLogLevel=warn'
    command = local['java']['-D' + log_level, '-jar', '../../../out/artifacts/benchmark_jar/benchmark.jar']

    def path_maker(k):
        return path.join('.', 'results', script, str(k))

    apps = [(path_maker(i), mod.setup(command, path_maker(i))) for i in range(number)]

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

    # Read the total matrices
    total_uploaded_matrices = trans.read_all_matrices((k for k, _ in apps),
                                                      lambda x: x == 'TotalUploadedBandwidth.csv')
    total_downloaded_matrices = trans.read_all_matrices((k for k, _ in apps),
                                                        lambda x: x == 'TotalDownloadedBandwidth.csv')

    # Create sorted total uploaded
    for run, subresults in total_uploaded_matrices.items():
        for inputpath, matrix in subresults.items():
            column_data = list(enumerate(sorted(map(float, matrix[-1][1:]), reverse=True)))
            trans.write_matrix(column_data, path.join(run, 'Sorted' + inputpath))

    # Create sorted total downloaded
    for run, subresults in total_downloaded_matrices.items():
        for inputpath, matrix in subresults.items():
            column_data = list(enumerate(sorted(map(float, matrix[-1][1:]), reverse=True)))
            trans.write_matrix(column_data, path.join(run, 'Sorted' + inputpath))

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