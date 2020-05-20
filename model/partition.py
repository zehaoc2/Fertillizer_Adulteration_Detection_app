import cv2
import glob
import random


def main():
    d = {}
    k = 40

    for file in glob.glob('preprocessed\\train\\clean\\*.jpg'):
        filename = file[file.rfind('\\') + 1:]
        d[filename] = 0

    for file in glob.glob('preprocessed\\train\\adulterate\\*.jpg'):
        filename = file[file.rfind('\\') + 1:]
        d[filename] = 1

    def extract(img):
        start = img.find('/')
        end = img.rfind('_')
        return img[start + 1: end]

    keys = list(d.keys())
    keys = [extract(i) for i in keys]
    keys = list(dict.fromkeys(keys))
    random.seed(2020)
    random.shuffle(keys)

    each = int(len(keys) / k)
    partitions = [keys[i * each: (i + 1) * each] for i in range(k)]

    for i in range(k):
        for j in range(each):
            for pieces in range(12):
                file = partitions[i][j] + '_' + str(pieces) + '.jpg'
                if d[file] == 0:  # clean
                    image = cv2.imread('preprocessed\\train\\clean\\' + file)
                else:  # adulterate
                    image = cv2.imread('preprocessed\\train\\adulterate\\' + file)
                for part in range(k):
                    goto = 'ks\\k' + str(part)
                    if i == part:
                        goto += '\\test\\'
                    else:
                        goto += '\\train\\'
                    if d[file] == 0:
                        goto += 'clean\\'
                    else:
                        goto += 'adulterate\\'
                    goto += file
                    cv2.imwrite(goto, image)
        print("Finished part " + str(i))


if __name__ == "__main__":
    main()
