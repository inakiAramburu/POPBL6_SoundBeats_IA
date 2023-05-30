package mqReciver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.Scanner;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;


public class principal2 {

    private static final String EXCHANGE_NAME = "direct1";
    private ConnectionFactory factory;

    public principal2() {
        factory = new ConnectionFactory();
        factory.setHost("soundbeatsnodered.duckdns.org");
        factory.setUsername("guest");
        factory.setPassword("guest");
    }

    public void subscribe() {
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, "direct", true);
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE_NAME, "new-routing-key");

            MyConsumer consumer = new MyConsumer(channel);
            boolean autoAck = true;
            channel.basicConsume(queueName, autoAck, consumer);

            System.out.println("Esperando mensaje. Pulsa return para terminar");
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();

        } catch (IOException | TimeoutException e) {
            ((Throwable) e).printStackTrace();
        }
    }

    public class MyConsumer extends DefaultConsumer {

        public MyConsumer(Channel channel) {
            super(channel);
        }

        @Override
        
        public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) {
            String message = new String(body, StandardCharsets.UTF_8);
            System.out.println("Mensaje recibido (JSON): " + message);
        }
    }

    public static void main(String[] args) {
        principal2 subscriber = new principal2();
        subscriber.subscribe();
    }
}