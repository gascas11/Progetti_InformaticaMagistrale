"""
Subject: Multisensory Data Exploration

Project: Raincloud Sonification

@author: Danilo Dolce, Gaspare Casano
"""

import os
import socket
import time
import pandas as pd
import ptitprince as pt
import seaborn as sns
import numpy as np
import matplotlib.pyplot as plt

# Inizializzazione:
def inizializzazione():
    path_img = os.getcwd()+"/img"
    if (os.path.exists(path_img) is False):
        os.makedirs(path_img)

# Genera l'istogramma di un set di dati
def genera_istogramma(data):
    plt.figure(figsize = (10, 7)) 
    plt.hist(data, bins = 10, color = "blue") 
    plt.title("img/Histogram")
    plt.savefig("img/Histogram.png")
    plt.close()
    
# Genera il density plot e ritorna i punti delle coordinate x e y
def genera_density(data):
    plt.figure(figsize=(16,10), dpi= 80)
    density_seaborn = sns.kdeplot(data, shade=True, color="r", alpha=.7)
    x,y = density_seaborn.get_lines()[0].get_data()
    plt.title('Density Plot', fontsize=22)
    plt.savefig("img/DensityPlot.png")
    plt.close()
    return x, y

# Assesta i dati per essere inviati al relativo file Pure Data
def assestamento_dati(x, y):
    round_x = np.round(x, 2)
    round_y = np.round(y, 2)
    return round_x, round_y

# Genera i vari Raincloud plots
def genera_raincloud():
    
    # Plotting the clouds
    f, ax = plt.subplots(figsize=(7, 5))
    dy="variety"; dx="sepal.length"; ort="h"; pal = sns.color_palette(n_colors=1)
    ax=pt.half_violinplot( x = dx, y = dy, data = df, palette = pal,
    bw = .2, cut = 0.,scale = "area", width = .6, inner = None,
       orient = ort)
    plt.title("Raincloud with Clouds")
    plt.savefig("img/Raincloud_Clouds.png")
    plt.close()
    
    # Adding the rain
    f, ax = plt.subplots(figsize=(7, 5))
    ax=pt.half_violinplot( x = dx, y = dy, data = df, palette = pal,
    bw = .2, cut = 0.,scale = "area", width = .6, inner = None,
       orient = ort)
    ax=sns.stripplot( x = dx, y = dy, data = df, palette = pal,  
    edgecolor = "white",size = 3, jitter = 0, zorder = 0,  
    orient = ort)
    plt.title("Raincloud with Clouds and Rain")
    plt.savefig("img/Raincloud_Clouds_Rain.png")
    plt.close()
    
    # Adding jitter to the rain
    f, ax = plt.subplots(figsize=(7, 5))
    ax=pt.half_violinplot( x = dx, y = dy, data = df, palette = pal,
    bw = .2, cut = 0.,scale = "area", width = .6, inner = None,  
    orient = ort)
    ax=sns.stripplot( x = dx, y = dy, data = df, palette = pal,
       edgecolor = "white",size = 3, jitter = 1, zorder = 0,  
    orient = ort)
    plt.title("Raincloud with Clouds and Jitter rain")
    plt.savefig("img/Raincloud_Clouds_Rain_Jitter.png")
    
    # Adding the boxplot with quartiles
    f, ax = plt.subplots(figsize=(7, 5))
    ax=pt.half_violinplot( x = dx, y = dy, data = df, palette = pal, bw = .2, cut = 0.,
                          scale = "area", width = .6, inner = None, orient = ort)
    ax=sns.stripplot( x = dx, y = dy, data = df, palette = pal, edgecolor = "white",
                     size = 3, jitter = 1, zorder = 0, orient = ort)
    ax=sns.boxplot( x = dx, y = dy, data = df, color = "black", width = .15, zorder = 10,\
                showcaps = True, boxprops = {'facecolor':'none', "zorder":10},\
                showfliers=True, whiskerprops = {'linewidth':2, "zorder":10},\
                   saturation = 1, orient = ort)
    
    sns.boxplot( x = dx, y = dy, data = df, color = "black", width = .15, zorder = 10,\
                showcaps = True, boxprops = {'facecolor':'none', "zorder":10},\
                showfliers=True, whiskerprops = {'linewidth':2, "zorder":10},\
                   saturation = 1, orient = ort)
    plt.title("Raincloud with Boxplot")
    plt.savefig("img/Raincloud_Boxplot.png")
    plt.close()
    
    dx = "variety"; dy = "sepal.length"; ort = "h"; pal = "Set2"; sigma = .2
    f, ax = plt.subplots(figsize=(7, 5))
    ax=pt.RainCloud(x = dx, y = dy, data = df, palette = pal, bw = sigma,
                 width_viol = .6, ax = ax, orient = ort, move = .2)
    plt.title("Raincloud with Boxplot and Shifted Rain")
    plt.savefig("img/Raincloud_Boxplot_Shifted_Rain.png")
    plt.close()
    
# Inizializzazione
inizializzazione()

# Lettura del dataset
df = pd.read_csv("setosa_sepal_length.csv", sep= ",")

# Carico i dati di riferimento
data = df["sepal.length"]

# Genero l'istogramma dei dati
genera_istogramma(data)

# Ricavo il density plot e i punti della curva relativa
x,y = genera_density(data)

# Assestamento dei dati
new_x, new_y = assestamento_dati(x, y)

# Genero i raincloud plots
genera_raincloud()

# Invio dei dati a Pure Data per la generazione del suono
# Nota: il suono viene generato in tempo reale ma la registrazione deve essere avviata manualmente dal file Pure Data relativo
serverPort = 5010
serverSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
serverSocket.bind(('', serverPort))
print ("Il server è stato creato")
print("Invio dei dati in corso...")

clientAddress = ('127.0.0.1', 5009)

for i in range(len(new_y)):
    serverSocket.sendto(str(new_y[i]).encode('utf-8'), clientAddress)
    time.sleep(0.08)

print("Invio dei dati completato")

# Chiudo la socket
serverSocket.close()
