# Welcome to ACES-UIUC-Fertilizer-Group!

Our research group aims to assist farmers and government agents in developing countries in detecting adulterations in fertilizers. We investigated fertilizer usage in Tanzania, and learned that farmers were reluctant to utilize fertilizers because they were concerned about the quality of fertilizers. Nevertheless, our study showed that most of the fertilizer qualities were above legal standard, which aligned with the findings of prior literature. Therefore, we developed an android application for farmers and government to determine fertilizer authenticity. 

To use the mobile application, users will download the APK and create an account, which keeps track of user submission. When the user uploads an image, geolocation and other information are extracted and recorded in the database. Input images are preprocessed, including rotating and resizing, to match the model’s expected dimension and format. Users can also obtain a summary of past submission and predictions at each local store. 

# Installation

To install the app, please download the [apk file](https://drive.google.com/file/d/1coMGOr7_yWZFNHD-UJpgEIvTEqXdlNJ8/view?usp=sharing) and install it on your android phone.

# App Documentaion

## Prediction

### MainActivity

This activity allows users to upload a photo by choosing a photo from the gallery, or by taking a photo from the phone's camera.

### InstructionActivity

This activity instructs users on how to take good-quality photos for better prediction accuracy.

### InfoActivity

This activity allows users to enter user-specific metadata, including note, store, village, and district.

### PredictActivity

This activity predicts the input photo as adulterated or pure fertilizer and provides the options of saving the result and generating reports.

## UserProfile


# Credits

For questions, please contact us via email at aceuiucfertilizer@gmail.com.
