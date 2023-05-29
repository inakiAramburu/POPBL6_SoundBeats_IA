package edu.mondragon.soundbeats.RabbitMQ;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class Suscriber {
	final static String EXCHANGE_GRABACION = "grabaciones";
	final static String EXCHANGE_RESULTADO = "resultado";
	final static int LIMITE = 100;
	ConnectionFactory factory;
	Channel channel;
	GestorPacientes gestorPacientes;

	public Suscriber() {
		factory = new ConnectionFactory();
		factory.setHost("soundbeatsnodered.duckdns.org");
		factory.setUsername("guest");
		factory.setPassword("guest");
		gestorPacientes = new GestorPacientes();
	}

	public void conectar() {

		try (Connection connection = factory.newConnection()) {

			channel = connection.createChannel();

			channel.exchangeDeclare(EXCHANGE_GRABACION, "fanout");
			channel.exchangeDeclare(EXCHANGE_RESULTADO, "direct");

			String nombreCola = channel.queueDeclare().getQueue();
			channel.queueBind(nombreCola, EXCHANGE_GRABACION, "");
			MiConsumer consumer = new MiConsumer(channel);
			channel.basicConsume(nombreCola, true, consumer);

			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			channel.close();
		} catch (IOException | TimeoutException e) {
			e.printStackTrace();
		}
	}

	public synchronized void stop() {
		this.notify();
	}

	public class MiConsumer extends DefaultConsumer {

		public MiConsumer(Channel channel) {
			super(channel);
		}

		public void handleDelivery(String consumerTag, Envelope envelope,
				AMQP.BasicProperties properties, byte[] body) throws IOException {

			String clave = new String(body);
			String valores[] = clave.split("SEPARACION");
			Sonido sonido = new Sonido(valores[1].getBytes(), valores[2], Long.valueOf(valores[3]));

			System.out.println("Reproduciendo audio");
			sonido.play();
			System.out.println("Finalizado");
			
			channel.basicPublish(EXCHANGE_RESULTADO, valores[0], null, "te va a dar un ataque".getBytes());
		}
	}

	public static void main(String[] args) {
		Scanner teclado = new Scanner(System.in);
		Suscriber suscriber = new Suscriber();
		System.out.println(" Esperando mensaje. Pulsa return para terminar");
		
		suscriber.conectar();

		/*new Thread(() -> {
			teclado.nextLine();
			suscriber.stop();
			teclado.close();
		}).start();*/
		
	}
}
