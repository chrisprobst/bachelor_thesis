import csv
import statistics
import argparse


def mean(matrix):
    print('Time', 'Mean', 'StDev')
    for row in matrix[1:]:
        head, rest = row[0], list(map(float, row[1:]))
        print(head, statistics.mean(rest), statistics.stdev(rest))


def transform(matrix):
    mean(matrix)


def main():
    parser = argparse.ArgumentParser(description='Transforms a csv file')
    parser.add_argument('-f', required=True, dest='file')
    args = parser.parse_args()

    with open(args.file) as csvfile:
        reader = csv.reader(csvfile, delimiter=' ', skipinitialspace=True)
        matrix = [list(filter(lambda x: x is not None and x != '', row)) for row in reader]
        transform(matrix)


if __name__ == '__main__':
    main()
