import argparse
import importlib
from sys import stdout as stdout
import os.path as path

from plumbum import local


def start_process(process):
    for line in process.stderr:
        stdout.write(line.decode('utf-8'))
    for line in process.stdout:
        stdout.write(line.decode('utf-8'))
        process.wait()


def main():
    parser = argparse.ArgumentParser(description='Runs benchmark scenarios')
    parser.add_argument('-s', required=True, dest='s')
    parser.add_argument('-n', required=True, dest='n', type=int)
    parser.add_argument('-p', dest='p', action='store_true')
    parser.set_defaults(p=False)
    args = parser.parse_args()

    number, script, parallel = args.n, args.s, args.p
    mod = importlib.import_module(script)
    log_level = 'org.slf4j.simpleLogger.defaultLogLevel=warn'
    command = local['java']['-D' + log_level, '-jar', '../../../out/artifacts/benchmark_jar/benchmark.jar']
    apps = [mod.setup(command, path.join('.', 'results', script, str(i))) for i in range(number)]

    if parallel:
        print('Started all iterations in parallel')
        for idx, process_handle in enumerate([app.popen() for app in apps]):
            print('Waiting for %i. iteration' % idx)
            start_process(process_handle)
    else:
        for idx, app in enumerate(apps):
            print('Running %i. iteration' % idx)
            start_process(app.popen())


if __name__ == '__main__':
    main()