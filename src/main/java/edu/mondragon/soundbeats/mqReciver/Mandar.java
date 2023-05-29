package edu.mondragon.soundbeats.mqReciver;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;

public class Mandar {
    final static String EXCHANGE_NAME = "diagnostico";
	final static String EXCHANGE_RESULTADO = "categorias";
	final static int LIMITE = 100;
	ConnectionFactory factory;
	Channel channel;

	public Mandar() {
		factory = new ConnectionFactory();
		factory.setHost("localhost");
		factory.setUsername("guest");
		factory.setPassword("guest");
	}

    public void conectar() {

		try (Connection connection = factory.newConnection()) {

			channel = connection.createChannel();

			channel.exchangeDeclare(EXCHANGE_NAME, "direct");
			channel.exchangeDeclare(EXCHANGE_RESULTADO, "topic");

            mandarJson();
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

    public void mandarJson(){		
		try {
			channel.basicPublish(EXCHANGE_NAME, "new-routing-key", null, "sonido".getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public static void main(String[] args) {
		Mandar suscriber = new Mandar();		
		suscriber.conectar();		
	}
}
