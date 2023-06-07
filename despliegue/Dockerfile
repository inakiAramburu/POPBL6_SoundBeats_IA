# Utiliza una imagen base con Java 8
FROM openjdk:8-jdk

# Instala Python 3, Git, Maven y otras dependencias
RUN apt-get update && \
    apt-get install -y python3 python3-pip git maven && \
    rm -rf /var/lib/apt/lists/*

# Establece la versión predeterminada de Python a Python 3
RUN ln -s /usr/bin/python3 /usr/bin/python

# Actualiza PIP a la última versión
RUN pip3 install --upgrade pip 

# Clona el repositorio de Git
RUN git clone https://github.com/inakiAramburu/POPBL6_SoundBeats_IA.git /app

# Cambia el directorio de trabajo al repositorio clonado
WORKDIR /app

# Expone los puertos para acceder desde cualquier IP
EXPOSE 15672 5672 5673 10050

# Instala las dependencias de Python
RUN pip install pandas tensorflow librosa flask matplotlib

# Compila el programa usando Maven
RUN mvn -DskipTests package

# Ejecuta el archivo JAR generado
CMD java -jar target/mqReciver-0.0.1-SNAPSHOT.jar & python main_REST.py