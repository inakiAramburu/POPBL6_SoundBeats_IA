import pandas as pd
import numpy as np
import tensorflow as tf
import keras
import sys

import librosa
import librosa.display

def cargar_mfcc (archivo, duracion=10, sr=22050):
    data = []
    y, sr = librosa.load(archivo, duration=duracion)
    mfcc = np.mean(librosa.feature.mfcc(y=y, sr=sr, n_mfcc=25).T,axis=0)
    feature = np.array(mfcc).reshape([-1,1])
    data.append(feature)
    return data

archivo = sys.argv[1]
#MODELO = 'SoundBeatsModel.h5'

MODELO = 'heartbeat_disease'

#model = tf.keras.models.load_model("../" + MODELO)
model = tf.keras.models.load_model(MODELO)

mfccs = cargar_mfcc(archivo = archivo)
muestra = np.concatenate((mfccs, mfccs))
pred = model(muestra)
pred = np.argmax(pred,axis=1)
print(pred[1])
