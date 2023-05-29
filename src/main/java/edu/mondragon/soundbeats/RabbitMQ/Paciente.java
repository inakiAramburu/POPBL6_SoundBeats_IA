package edu.mondragon.soundbeats.RabbitMQ;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;


import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class Paciente {
	final static String EXCHANGE_GRABACION = "grabaciones";
	final static String EXCHANGE_RESULTADO = "resultado";
	ConnectionFactory factory;
	String nombreColaRespuesta;
	Channel channel;
	String id;
	PacienteCorazon paciente;

	public Paciente(String id) {
		this.id = id;

		factory = new ConnectionFactory();
		factory.setHost("soundbeatsnodered.duckdns.org");
		factory.setUsername("guest");
		factory.setPassword("guest");
	}

	public void conectar() {
		try (Connection connection = factory.newConnection()) {

			channel = connection.createChannel();
			channel.exchangeDeclare(EXCHANGE_GRABACION, "fanout");
			channel.exchangeDeclare(EXCHANGE_RESULTADO, "direct");

			String nombreColaAlarma = channel.queueDeclare().getQueue();
			channel.queueBind(nombreColaAlarma, EXCHANGE_RESULTADO, id);
			MiConsumer consumer = new MiConsumer(channel);
			channel.basicConsume(nombreColaAlarma, true, consumer);

			paciente = new PacienteCorazon();
			paciente.start();
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			channel.close();
		} catch (IOException | TimeoutException e) {
			e.printStackTrace();
		}
	}

	public void mandarAudio(){		
		try {
			Path path = Paths.get("C:/Users/lamaa/OneDrive/Escritorio/SoundBeats/file_example_WAV_1MG.wav");
			Sonido sonido;
			sonido = new Sonido(path);

			System.out.println("resultado: ");

			channel.basicPublish(EXCHANGE_GRABACION, "", null, sonido.sonidoAbytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public class PacienteCorazon extends Thread {

		@Override
		public void run() {
			try {
				Path path = Paths.get("C:/Users/lamaa/OneDrive/Escritorio/SoundBeats/SoundBeats/file_example_WAV_1MG.wav");
				Sonido sonido = new Sonido(path);
				String clave = id + "SEPARACION" + sonido.getSonidoString();
	
				channel.basicPublish(EXCHANGE_GRABACION, "", null, clave.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public class MiConsumer extends DefaultConsumer {
		public MiConsumer(Channel channel) {
			super(channel);
		}

		public void handleDelivery(String consumerTag, Envelope envelope,
				AMQP.BasicProperties properties, byte[] body) throws IOException {
			String mensaje = new String(body, "UTF-8");
			System.out.println("resultado: " + mensaje);
		}
	}

	public synchronized void stop() {
		paciente.interrupt();
		this.notify();
	}
	public static void main(String[] args) {
		Scanner teclado = new Scanner(System.in);
		System.out.print("Id: ");
		String id = teclado.nextLine();
		System.out.println("Pulse return para parar");
		Paciente publisher = new Paciente(id);

		publisher.conectar();
	}
}
