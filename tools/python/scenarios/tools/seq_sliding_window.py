__author__ = 'chrisprobst'

import csv
import statistics

def read_matrix(inputpath):
    sliding_window = 10
    with open(inputpath, 'r') as infile:
        # Read matrix of values
        reader = csv.reader(infile, delimiter=' ')
        mat = [list(filter(lambda x: x is not None and x != '', row[:2])) for row in reader]
        rmat = mat[1:]
        n = list()
        l = len(rmat) // sliding_window

        for i in range(l):
            row_set = rmat[i*sliding_window:(i+1)*sliding_window]
            c1 = [float(r[0]) for r in row_set]
            c2 = [float(r[1]) for r in row_set]
            n.append([statistics.mean(c1), statistics.mean(c2)])

        if len(rmat) != (l * sliding_window):
            row_set = rmat[(l * sliding_window):]
            c1 = [float(r[0]) for r in row_set]
            c2 = [float(r[1]) for r in row_set]
            n.append([statistics.mean(c1), statistics.mean(c2)])

        n = [mat[0]] + n

        return n

def write_matrix(outputpath, mat):
    with open(outputpath, 'w') as outfile:
        writer = csv.writer(outfile, delimiter=' ')
        for row in mat:
            writer.writerow(row)


# download
write_matrix('results/scenario_2_seq/0/GeneratedMeanCurrentDownloadBandwidth2.csv',
             read_matrix('results/scenario_2_seq/0/GeneratedMeanCurrentDownloadBandwidth.csv'))
# ssupload
write_matrix('results/scenario_2_seq/0/GeneratedMeanCurrentSuperSeederUploadBandwidth2.csv',
             read_matrix('results/scenario_2_seq/0/GeneratedMeanCurrentSuperSeederUploadBandwidth.csv'))