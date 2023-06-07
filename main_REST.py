from flask import Flask,request, url_for, redirect, render_template

app = Flask(__name__)

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


MODELO = 'SoundBeatsModel.h5'
model = tf.keras.models.load_model(MODELO)

@app.route('/predict',methods=['POST','GET'])
def predict():
    archivo = request.data.decode('utf-8')
    print("RUTA: "+archivo)
    mfccs = cargar_mfcc(archivo = archivo)
    muestra = np.array(mfccs)
    pred = model(muestra)
    pred = np.argmax(pred,axis=1)
    print(pred)
    return str(pred)

if __name__ == '__main__':
    app.run()