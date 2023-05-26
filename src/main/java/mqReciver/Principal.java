package mqReciver;

import java.io.IOException;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


public class Principal {

	private static final String EXCHANGE_NAME = "diagnostico";
	private final static String EXCHANGE_RESULTADO = "categorias";
	private final static Double probabilidad = 0.20;
	private final static Integer MAX_INTENTOS = 20;
	final static String QUEUE_NAME = "numeros";
	final static String DLX_NAME = "el_que_quiera";
	String pythonInterpreter = "C:/Users/svequ/AppData/Local/Programs/Python/Python311/python.exe";
	String pythonFile = "C:/Users/svequ/Desktop/IA_Simulator.py";
	String outputFilePath = "src/resources/audios/"; // Replace with the desired output file path
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
			channel.exchangeDeclare(DLX_NAME, "fanout");
			
			boolean durable = false;
			boolean exclusive = false;
			boolean autodelete = false;
			Map<String,Object> arguments = new HashMap<>();
			arguments.put("x-dead-letter-exchange", DLX_NAME);
			
			channel.queueDeclare(QUEUE_NAME, durable, exclusive,autodelete, arguments); //Declara la cola de la cual va a consumir el suscribir
			
			//String queueName = channel.queueDeclare().getQueue();
			channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "new-routing-key");
			

			channel.basicQos(1);
			MyConsumer consumer = new MyConsumer(channel);
			boolean autoAck = false;
			channel.basicConsume(QUEUE_NAME, autoAck, consumer);
			

			/*synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			channel.basicCancel(tag);
			channel.close();*/

			System.out.println("Esperando mensaje");
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();

		} catch (IOException | TimeoutException e) {
			((Throwable) e).printStackTrace();
		}
	}

	public class MyConsumer extends DefaultConsumer {
		
		ConcurrentMap<String, Integer> contadores;

		public MyConsumer(Channel channel) {
			
			super(channel);
			contadores = new ConcurrentHashMap<>();
		}

		@Override
		public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {
			
			String message = new String(body, StandardCharsets.UTF_8);
			Consulta consulta;
			byte[] decodedBytes;
			String decodedString;
			String[] command;
			ProcessBuilder processBuilder;
			Process process;
			BufferedReader stdoutReader;
			BufferedReader stderrReader;
			String line;
			int exitCode;

			try {
				
				consulta = gson.fromJson(message, Consulta.class);
				System.out.println("Mensaje recibido (JSON): " + consulta.getAudio() + consulta.getPacienteID());
				
				// Decode the Base64 string
				decodedBytes = Base64.getDecoder().decode(consulta.getAudio());
				decodedString = new String(decodedBytes);
				FileOutputStream outputStream = new FileOutputStream(outputFilePath + consulta.getPacienteID()+".waw");
	            outputStream.write(decodedBytes);
	            outputStream.close();
				//decodedString = "random";
				

				// Construir el comando de ejecución
				command = new String[] { pythonInterpreter, pythonFile, decodedString };


				// Crear el proceso y ejecutar el comando
				processBuilder = new ProcessBuilder(command);
				process = processBuilder.start();

				// Leer la salida del proceso
				stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

				// Leer la salida estándar
				System.out.println("Salida estándar del proceso:");
				while ((line = stdoutReader.readLine()) != null) {
					System.out.println(line);
				}

				// Esperar a que el proceso termine
				exitCode = process.waitFor();
				System.out.println("El proceso ha finalizado con código de salida: " + exitCode);

				// Save the decoded bytes to a .wav file

				/*
				 * FileOutputStream outputStream = new FileOutputStream(outputFilePath);
				 * outputStream.write(decodedBytes); outputStream.close();
				 * System.out.println("Base64 decoding successful. Saved as " + outputFilePath);
				 */

				boolean grave = rand.nextDouble() < probabilidad;

				String gravedad = (grave) ? "grave" : "normal";

				String topic = String.format("%s.%s.%s", "respuesta", gravedad, consulta.getPacienteID());
				System.out.println(topic);
				consulta.setEnfermedad(line);
				// consulta.setEnfermedad("murmur");
				this.getChannel().basicPublish(EXCHANGE_RESULTADO, topic, null, gson.toJson(consulta).getBytes("UTF-8"));

			} 
			catch (IOException e) {
				System.out.println("Error while decoding Base64: " + e.getMessage());
				handleFail(message , envelope);

			} 
			catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.out.println("Interrupted : " + e.getMessage());
				handleFail(message , envelope);
			}
			catch (JsonSyntaxException e) {
			    // Manejar la excepción de sintaxis JSON
			    System.out.println("Error de sintaxis JSON: " + e.getMessage());
				handleFail(message , envelope);
			    // O realizar cualquier otra tarea de manejo de errores
			}
			catch (IllegalArgumentException e) {
			    // Handle invalid Base64 input
			    System.out.println("Invalid Base64 input: " + e.getMessage());
			    handleFail(message , envelope);
			    // Perform error handling or return an error response
			    // ...
			}/*catch (JsonIOException e) {
			    // Manejar la excepción de entrada/salida JSON
			    System.out.println("Error de entrada/salida JSON: " + e.getMessage());
			    // O realizar cualquier otra tarea de manejo de errores
			}*/

		}

		private void handleFail(String message, Envelope envelope) throws IOException {
			
			Integer intentos = contadores.get(message);
			boolean reprocesar;
			boolean multiple;
			if (intentos == null) intentos = 1;

			
			System.out.println("ERROR: No se ha podido procesar "+message+" intentos: "+intentos);
			
			if (intentos  == MAX_INTENTOS) {
				reprocesar = false;
				multiple = false;
				contadores.remove(message);
				//this.getChannel().basicNack(envelope.getDeliveryTag(), multiple, reprocesar);
				this.getChannel().basicReject(envelope.getDeliveryTag(), reprocesar);
				System.out.println("Rejected");
			}else {
				reprocesar = true;
				multiple = false;
				contadores.put(message, ++intentos);
				this.getChannel().basicNack(envelope.getDeliveryTag(), multiple, reprocesar);
				System.out.println("Resent");
			}
		}
	}
	

	public static void main(String[] args) {
		Principal subscriber = new Principal();
		subscriber.subscribe();
	}
}
