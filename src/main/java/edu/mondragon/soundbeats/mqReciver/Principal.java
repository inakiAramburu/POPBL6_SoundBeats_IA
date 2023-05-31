package edu.mondragon.soundbeats.mqReciver;

import java.io.IOException;
import java.util.Base64;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;


import com.google.gson.Gson;
import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;
import java.util.Random;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class Principal {

	private static final String EXCHANGE_NAME = "diagnostico";
	private final static String EXCHANGE_RESULTADO = "categorias";
	String pythonInterpreter = "python"; //CAMBIAR AL INTERPRETE IMPORTADO
	String pythonFile = "../SoundBeats/src/main/java/edu/mondragon/soundbeats/IA/main.py";
	String outputFilePath = "src/resources/audios/";
	private ConnectionFactory factory;
	Consulta consulta;
	Gson gson;
	Random rand = new Random();

	public Principal() {
		factory = new ConnectionFactory();
		factory.setHost("soundbeatsnodered.duckdns.org");
		factory.setUsername("guest");
		factory.setPassword("guest");
		gson = new Gson();
	}

	public void subscribe() {
		try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {

			channel.exchangeDeclare(EXCHANGE_NAME, "direct", false);
			channel.exchangeDeclare(EXCHANGE_RESULTADO, "topic");
			String queueName = channel.queueDeclare().getQueue();
			channel.queueBind(queueName, EXCHANGE_NAME, "new-routing-key");

			MyConsumer consumer = new MyConsumer(channel);
			boolean autoAck = true;
			channel.basicConsume(queueName, autoAck, consumer);

			System.out.println("Esperando mensaje2. Pulsa return para terminar2");
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
			Consulta consulta = gson.fromJson(message, Consulta.class);

			System.out.println("Mensaje recibido (JSON)2: " + consulta.getPacienteID());
			System.out.println("Mensaje recibido (JSON): ha lleado " );
			try {
				// Decode the Base64 string
				byte[] decodedBytes = Base64.getDecoder().decode(consulta.getAudio());
				String nombreArchivo = consulta.getPacienteID() + ".wav";

				// Save the audio file temporarily
				FileOutputStream outputStream = new FileOutputStream(outputFilePath + nombreArchivo);
				outputStream.write(decodedBytes);
				outputStream.close();

				// Construir el comando de ejecución
				String[] command = new String[] { pythonInterpreter, pythonFile,
					outputFilePath + nombreArchivo };

				// Crear el proceso y ejecutar el comando
				ProcessBuilder processBuilder = new ProcessBuilder(command);
				Process process = processBuilder.start();

				// Leer la salida del proceso
				BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

				// Leer la salida estándar
				String line;
				Integer enfermedad = 0;
				System.out.println("Salida estándar del proceso:");
				while ((line = stdoutReader.readLine()) != null) {
					
					enfermedad = Integer.valueOf(line);
				}
				while ((line = stderrReader.readLine()) != null) {
					
				}
				String enfermedadNombre = (enfermedad == 0) ? "normal"
						: (enfermedad == 1) ? "murmur"
								: (enfermedad == 2) ? "extrastole"
										: (enfermedad == 3) ? "artifact" : (enfermedad == 4) ? "extrahls" : "error";
				consulta.setEnfermedad(enfermedadNombre);

				String gravedad = (enfermedad == 0) ? "normal" : (enfermedad == 3) ? "normal" : "grave";

				String topic = String.format("%s.%s.%s", "respuesta", consulta.getPacienteID(), gravedad);
				System.out.println(topic);

				this.getChannel().basicPublish(EXCHANGE_RESULTADO, topic, null,
						gson.toJson(consulta).getBytes("UTF-8"));

			} catch (IOException e) {
				System.err.println("Error while decoding Base64: " + e.getMessage());
			}
		}
	}

	public static void main(String[] args) {
		Principal subscriber = new Principal();
		subscriber.subscribe();
	}
}
