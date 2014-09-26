import csv
import statistics
from os import listdir
from os.path import join
import math

header = [['Time', 'Mean', 'Min', 'Max', 'StDev', 'ConfidenceInterval']]

# confidence_niveau: 0.90  # 0.90 -> CONF # 1.6 + 0.05 = 1.65
# confidence_niveau: 0.95  # 0.95 -> CONF # 1.9 + 0.06 = 1.96
confidence_Z = 1.96

GENERATED_PREFIX = "Generated"


def file_input_filter(x):
    return not x.startswith(GENERATED_PREFIX)


def get_confidence_interval(stdev, n):
    return confidence_Z * stdev / math.sqrt(n)


def write_matrix(results, outputpath):
    with open(outputpath, 'w') as outfile:
        writer = csv.writer(outfile, delimiter=' ')
        for row in results:
            writer.writerow(row)


def read_matrix(inputpath):
    with open(inputpath, 'r') as infile:
        # Read matrix of values
        reader = csv.reader(infile, delimiter=' ')
        return [list(filter(lambda x: x is not None and x != '', row)) for row in reader]


def read_all_matrices(inputdirs, name_filter):
    results = {}
    for inputdir in inputdirs:
        subresults = results[inputdir] = {}
        inputpaths = listdir(inputdir)
        for inputpath in filter(name_filter, filter(file_input_filter, inputpaths)):
            subresults[inputpath] = read_matrix(join(inputdir, inputpath))
    return results


def get_mean_of_row(row):
    assert len(row) > 0
    if len(row) == 1:
        x, = row
        return [x, x, x, 0, 0]

    mean = statistics.mean(list(map(float, row)))
    stdev = statistics.stdev(list(map(float, row)))
    min_val = min(map(float, row))
    max_val = max(map(float, row))
    confval_val = get_confidence_interval(stdev, len(list(row)))
    return [
        mean,
        min_val,
        max_val,
        stdev,
        confval_val,
    ]


def get_mean_of_matrix(matrix):
    results = [[int(round(float(row[0])))] + get_mean_of_row(row[1:]) for row in matrix[1:]]
    return header + results


def read_mean(inputpath):
    return get_mean_of_matrix(read_matrix(inputpath))


def read_all_mean(inputdirs):
    results = {}
    for inputdir in inputdirs:
        subresults = results[inputdir] = {}
        inputpaths = listdir(inputdir)
        for inputpath in filter(file_input_filter, inputpaths):
            subresults[inputpath] = read_mean(join(inputdir, inputpath))
    return results


def get_mean_of_results(results, filename):
    # Extract matrices for the given filename
    result_matrices = [result[filename][1:] for result in results.values()]
    result = []

    # Make sure every matrix has the same length
    # If not: Repeat the last element
    max_value = max(len(mat) for mat in result_matrices)
    for mat in result_matrices:
        diff = max_value - len(mat)
        assert diff >= 0
        if diff > 0:
            for _ in range(diff):
                last_time, last_row = mat[-1][0], mat[-1][1:]
                mat.append([last_time + 1] + last_row)
        assert len(mat) == max_value

    # Now calculate the mean of all
    for i in range(max_value):
        row = [mat[i][1] for mat in result_matrices]
        time, mean = [result_matrices[0][i][0]], get_mean_of_row(row)
        result.append(time + mean)

    return header + result