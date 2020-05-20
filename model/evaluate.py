import csv
import glob
import numpy as np
import matplotlib.pyplot as plt


def main():
    predictions = {}
    with open("predictions.csv", newline='\n') as f:
        reader = csv.reader(f, delimiter=',')
        for row in reader:
            predictions[row[0]] = int(row[1])

    real = [{}, {}, {}, {}, {}]
    types = ['clump', 'clump_maize', 'discolor', 'maize', 'normal']

    for i in range(len(types)):
        adulterate = 'database\\a_' + types[i] + '\\*.jpg'
        clean = 'database\\p_' + types[i] + '\\*.jpg'
        for file in glob.glob(adulterate) + glob.glob(adulterate.replace('jpg', 'JPG')):
            filename = file[file.rfind('\\') + 1: file.rfind('.')]
            real[i][filename] = 0
        for file in glob.glob(clean) + glob.glob(clean.replace('jpg', 'JPG')):
            filename = file[file.rfind('\\') + 1: file.rfind('.')]
            real[i][filename] = 1

    percentages = np.zeros([6, 12])
    for i in range(12):
        print('\nWhen threshold is %d: ' % i)
        misclassified = 0
        for image_type in range(len(types)):
            a, b, c, d = 0, 0, 0, 0
            for file in real[image_type].keys():
                if real[image_type][file] == 0 and predictions[file] <= i:
                    a += 1
                elif real[image_type][file] == 0 and predictions[file] > i:
                    b += 1
                elif real[image_type][file] == 1 and predictions[file] <= i:
                    c += 1
                else:
                    d += 1
            percentages[image_type][i] = round((a + d) / (a + b + c + d), 3)
            percentages[5][i] += a + d
            print('%s confusion matrix: %d, %d, %d, %d' % (types[image_type], a, b, c, d))
            misclassified += (b + c)
        percentages[5][i] /= 1000
        print('%d images misclassified' % misclassified)

    for accuracy in percentages:
        print(accuracy)
    x = np.arange(12)
    plt.plot(x, percentages[0])
    plt.plot(x, percentages[1])
    plt.plot(x, percentages[2])
    plt.plot(x, percentages[3])
    plt.plot(x, percentages[4])
    plt.plot(x, percentages[5])
    types.append('Average')
    plt.legend(types, loc='best')
    plt.show()


if __name__ == "__main__":
    main()
