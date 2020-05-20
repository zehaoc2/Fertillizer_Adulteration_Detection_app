import cv2
import glob


def main():
    folders = ['clump', 'clump_maize', 'discolor', 'maize', 'normal']
    for folder in folders:
        foldername = 'database\\a_' + folder + '\\*.'
        for filename in glob.glob(foldername + 'jpg') + glob.glob(foldername + 'JPG'):
            preprocess(filename, 'preprocessed\\adulterate\\')
        foldername = 'database\\p_' + folder + '\\*.'
        for filename in glob.glob(foldername + 'jpg') + glob.glob(foldername + 'JPG'):
            preprocess(filename, 'preprocessed\\clean\\')
        print('Finished preprocessing ' + folder)


def preprocess(filename, output):
    file = filename[filename.rfind('\\') + 1: filename.find('.')]
    image = cv2.imread(filename)
    if len(image) != 4032:
        image = cv2.rotate(image, cv2.ROTATE_90_CLOCKWISE)
    image = cv2.resize(image, (400, 300))
    pieces = []
    for row in range(3):
        for col in range(4):
            pieces.append(image[100 * row: 100 * (row + 1),
                          100 * col: 100 * (col + 1)])
    for i in range(len(pieces)):
        directory = output + file + '_' + str(i) + '.jpg'
        cv2.imwrite(directory, pieces[i])


if __name__ == "__main__":
    main()
