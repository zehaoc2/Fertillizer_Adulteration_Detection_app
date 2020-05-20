# Machine Learning to Detect Fertilizer Adulteration 

### Research Team 
The research is led by Professor Hope Michelson ([hopecm@illinois.edu](hopecm@illinois.edu)) at the Department of Agricultural and Consumer Economics (ACE) at the University of Illinois at Urbana-Champaign (UIUC). 

Current members include (in alphabetical order): 
| Members | Department | Email  | 
|:----------:|:------:|:--------------------------:| 
| Jiayu (Lily) Li | Computer Science | [jiayuli6@illinois.edu](jiayuli6@illinois.edu)  | 

Former members include (in alphabetical order): 
| Members | Department | Email  | 
|:----------:|:------:|:--------------------------:| 
| Zehao Chen | Department of Computer Science | zehaoc2@illinois.edu | 
| Subhankar Ghosh | Department of Statistics | [ghosh17@illinois.edu](ghosh17@illinois.edu) |
| Sreekanth Krishnaiah | Department of Statistics |[sk29@illinois.edu](sk29@illinois.edu) | 
| Ben Norton | Department of ACE | [bnorton2@illinois.edu](bnorton2@illinois.edu) | 
| Wei-Chen (Eric) Wang | Department of Computer Science | [wcwang2@illinois.edu](wcwang2@illinois.edu) | 
| Sihang Xu | Department of Computer Science | [sxu46@illinois.edu](sxu46@illinois.edu) | 

### Motivation 

The innovation aims to assist farmers in developing countries in detecting adulterated fertilizers. Our research group investigated fertilizer usages in Tanzania, and learned that farmers were reluctant to utilize fertilizers because they were concerned about the quality of fertilizers. Nevertheless, our study showed that most of the fertilizers were above legal standard, which aligned with the findings of prior literature. According to our survey, many farmers falsely associated clumped and discolored fertilizers with low quality. Such misconceptions led to a decreasing willingness to use fertilizers and a low level of agricultural productivity. Furthermore, it was difficult to differentiate pure fertilizers from adulterated ones with naked eyes, even for experienced government agents. As suspicion of fertilizer quality contributed significantly to its underusage, we sought to develop a mobile application to provide instant predictions of whether the fertilizer was adulterated. 

### Timeline 

- Fall 2018 and before - The research team collected 2000 images and investigated fertilizer usage in Tanzania. We found out that fertilizers underusage was a serious problem in Tanzania and many other developing countries, and it was largely due to the suspicion of fertilizer quality. Meanwhile, the research team on campus started literature review, and analyzed the images. Our findings show that most fertilizers in Tanzania were above legal standards, which is the same as the findings of many prior literatures. 
- Spring 2019 - We used a mixture of field (176 images) and lab (2428 images) samples to train a machine learning model, with varying degrees and types of adulterations. Images were pre-processed for a uniform dimension, and augumented to enlarge the training pool. RGB were extracted and fed into a support vector machine to perform feature extraction. The final augumented dataset includes 14714 augumented images, and we used PyTorch to obtain a convoluted neural network (CNN) model, with an accuracy rate around 93%. The model was then integrated to the server, which used Twilio to connect the model to the outside world. We were able to text the image to an Twilio account, then the prediction would be sent back to the phone within a 5 second delay. 
- Summer 2019 - Ben travelled to Tanzania to test out our model and its connectivity of texting an image to a US phone number and receiving the prediction. It was not until then did we realize that Twilio and all other cell cloud providers do not support services in Tanzania, and most parts of Africa. In addition, MMS services are very costly (for an average Tanzanian farmer) and unstable. Meanwhile, the model and the server was moved to a virtual machine so that the server can be up 24-7. The plan was soon abandoned due to numerous version conflicts, and the cell service limitations mentioned above. 
- Fall 2019 - Due to the limited internet coverage in Tanzania, our team decided to develop a mobile application which allowed farmers to download and get predictions without internet access. We intended to convert the pth model to h5, but soon realized a significantly lower accuracy rate when testing new images. We went back to see how the accuracy was measured previously, and realized that the dataset was massively augmented, and the model has observed most the the testing images beforehand, which resulted in a mistakenly high accuracy rate. After re-measuring the pth model, its accuracy dropped to around 60%. We also realized that all images in the dataset were extremely similar, with very little variations in background, terrain, lighting and shadows. As a result, we decided to re-generate lab photos which focused only on urea, and use a mixture of past images to train a new Keras model. 
- Spring 2020 - We started extracting the features from the images, but found very differences between pure and adulterated fertilizers. The result showed that pure fertilizers tend to have larger particles and more spacing in between, but the difference was not significant enough to yield a desired accuracy. Using a naive Keras model, nearly all images were classified as pure. After close inspection of misclassified adulterated images, we realized that adulterants tended to cluster and there could be up to 70% of an adulterated image that looked pure. We solved this problem by first regenerating some lab images with adulterants uniformly distributed. Second, we sliced the image into 12 pieces, and feed each piece into the model. An image was predicted as pure if and only if more than 8 pieces were classified as pure. The model was integrated to an Android mobile application. 

### File Descriptions 
The works of each semester were organized in its corresponding folder (such as FA18). Since we have changed our initial plan in FA19, you probably do not need anything from previous folders (FA18 and SP19), but they are still in the Box folders for reference. There is one README.md file in the app and model folder for further information. 