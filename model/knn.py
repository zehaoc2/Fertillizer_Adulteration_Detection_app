from keras.preprocessing.image import ImageDataGenerator
from keras.models import Sequential
from keras.layers import Conv2D, MaxPooling2D
from keras.layers import Activation, Dropout, Flatten, Dense
from keras import backend as K
import numpy as np
from PIL import Image
import glob


def main():
    def load(file):
        np_image = Image.open(file)
        np_image = np.array(np_image).astype('float32') / 255
        np_image = np.expand_dims(np_image, axis=0)
        return np_image

    def get_model(shape):
        m = Sequential()
        m.add(Conv2D(32, (3, 3), input_shape=shape))
        m.add(Activation('relu'))
        m.add(MaxPooling2D(pool_size=(2, 2)))

        m.add(Conv2D(32, (3, 3)))
        m.add(Activation('relu'))
        m.add(MaxPooling2D(pool_size=(2, 2)))

        m.add(Conv2D(64, (3, 3)))
        m.add(Activation('relu'))
        m.add(MaxPooling2D(pool_size=(2, 2)))

        m.add(Flatten())
        m.add(Dense(64))
        m.add(Activation('relu'))
        m.add(Dropout(0.5))
        m.add(Dense(1))
        m.add(Activation('sigmoid'))

        m.compile(loss='binary_crossentropy',
                  optimizer='rmsprop',
                  metrics=['accuracy'])
        return m

    k = 40
    predictions = {}

    for i in range(k):
        prefix = 'ks\\k' + str(i) + '\\'
        img_width, img_height = 100, 100
        train_data_dir = prefix + 'train\\'
        validation_data_dir = prefix + 'test\\'
        nb_train_samples = 11700
        nb_validation_samples = 300
        epochs = 10
        batch_size = 15

        if K.image_data_format() == 'channels_first':
            input_shape = (3, img_width, img_height)
        else:
            input_shape = (img_width, img_height, 3)
        model = get_model(input_shape)
        train_datagen = ImageDataGenerator(
            rescale=1. / 255,
            shear_range=0.2,
            zoom_range=0.2,
            horizontal_flip=True)
        test_datagen = ImageDataGenerator(rescale=1. / 255)

        train_generator = train_datagen.flow_from_directory(
            train_data_dir,
            target_size=(img_width, img_height),
            batch_size=batch_size,
            class_mode='binary')

        validation_generator = test_datagen.flow_from_directory(
            validation_data_dir,
            target_size=(img_width, img_height),
            batch_size=batch_size,
            shuffle=False,
            class_mode='binary')

        model.fit_generator(
            train_generator,
            steps_per_epoch=nb_train_samples // batch_size,
            epochs=epochs,
            validation_data=validation_generator,
            validation_steps=nb_validation_samples // batch_size)

        adulterates = sorted(glob.glob(prefix + 'test\\adulterate\\*.jpg'))
        pics = int(len(adulterates) / 12)
        for p in range(pics):
            cropped = adulterates[p * 12: (p + 1) * 12]
            acc = 0
            for piece in cropped:
                prediction = model.predict(load(piece))[0][0]
                if prediction > 0.5:
                    acc += 1
            filename = cropped[0]
            filename = filename[filename.rfind('\\') + 1: filename.rfind('_')]
            predictions[filename] = acc

        cleans = sorted(glob.glob(prefix + 'test\\clean\\*.jpg'))
        pics = int(len(cleans) / 12)
        for p in range(pics):
            cropped = cleans[p * 12: (p + 1) * 12]
            acc = 0
            for piece in cropped:
                prediction = model.predict(load(piece))[0][0]
                if prediction > 0.5:
                    acc += 1
            filename = cropped[0]
            filename = filename[filename.rfind('\\') + 1: filename.rfind('_')]
            predictions[filename] = acc
        print('Finished part ' + str(i + 1))

    print(predictions)

    with open('predictions.csv', 'w') as f:
        for key in predictions.keys():
            f.write("%s,%s\n" % (key, predictions[key]))


if __name__ == "__main__":
    main()
