#!/bin/bash

# Variables
nombre_contenedor="iaJavaContenedor"  # Reemplaza <nombre_del_contenedor> con el nombre deseado para el contenedor
nombre_imagen="iaimagen"  # Reemplaza <nombre_de_la_imagen> con el nombre deseado para la imagen
ruta_servicio=/etc/systemd/system/docker-IA-JAVA.service

# Detener y eliminar todos los contenedores
echo "Deteniendo y eliminando todos los contenedores..."
sudo docker stop $(sudo docker ps -aq)
sudo docker rm $(sudo docker ps -aq)

# Eliminar todas las imágenes de Docker
echo "Eliminando todas las imágenes de Docker..."
sudo docker rmi $(sudo docker images -a -q)

# Construir la imagen a partir del Dockerfile
echo "Construyendo la imagen a partir del Dockerfile..."
sudo docker build -t "$nombre_imagen" .

# Iniciar el nuevo contenedor
echo "Iniciando el nuevo contenedor..."
sudo docker run -d --name "$nombre_contenedor" "$nombre_imagen"

# Obtener el ID del contenedor en ejecución
container_id=$(sudo docker ps --format '{{.ID}}')

# Actualizar el archivo de servicio
echo "Actualizando el archivo de servicio..."
sudo sed -i "s|ExecStart=/usr/bin/docker start.*|ExecStart=/usr/bin/docker start $container_id|" "$ruta_servicio"
sudo sed -i "s|ExecStop=/usr/bin/docker stop -t.*|ExecStop=/usr/bin/docker stop -t $container_id|" "$ruta_servicio"

# Habilitar el servicio
echo "Habilitando el servicio..."
sudo systemctl enable docker-IA-JAVA.service

echo "¡Proceso completado!"