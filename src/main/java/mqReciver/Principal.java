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
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Principal {

	private static final String EXCHANGE_NAME = "diagnostico";
	private final static String EXCHANGE_RESULTADO = "categorias";
	private final static Integer MAX_INTENTOS = 20;
	final static String QUEUE_NAME = "numeros";
	final static String DLX_NAME = "el_que_quiera";
	String pythonInterpreter = "python";
	String pythonFile = "src/IA_Simulator.py";
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

			boolean durable = true;
			boolean exclusive = false;
			boolean autodelete = false;
			Map<String, Object> arguments = new HashMap<>();
			arguments.put("x-dead-letter-exchange", DLX_NAME);

			channel.queueDeclare(QUEUE_NAME, durable, exclusive, autodelete, arguments); // Declara la cola de la cual
																							// va a consumir el
																							// suscribir

			// String queueName = channel.queueDeclare().getQueue();
			channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "new-routing-key");

			channel.basicQos(1);
			MyConsumer consumer = new MyConsumer(channel);
			boolean autoAck = false;
			channel.basicConsume(QUEUE_NAME, autoAck, consumer);

			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Esperando mensaje");

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

		public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
				throws IOException {
			String message = new String(body, StandardCharsets.UTF_8);
			Consulta consulta;
			byte[] decodedBytes;

			try {
				consulta = gson.fromJson(message, Consulta.class);
				System.out.println("Mensaje recibido (JSON): " + consulta.getPacienteID());

				// Decode the Base64 string
				decodedBytes = Base64.getDecoder().decode(consulta.getAudio());

				FileOutputStream outputStream = new FileOutputStream(
						outputFilePath + consulta.getPacienteID() + ".wav");
				outputStream.write(decodedBytes);
				outputStream.close();

				String respuesta = request(outputFilePath + consulta.getPacienteID() + ".wav");

				String enfermedadNombre, gravedad;
				System.out.println(respuesta);
				switch (respuesta) {
					case "[0]":
						enfermedadNombre = "normal";
						gravedad = "normal";
						break;
					case "[1]":
						enfermedadNombre = "murmur";
						gravedad = "grave";
						break;
					case "[2]":
						enfermedadNombre = "artifact";
						gravedad = "normal";
						break;
					case "[3]":
						enfermedadNombre = "extrastole";
						gravedad = "grave";
						break;
					case "[4]":
						enfermedadNombre = "extrahls";
						gravedad = "grave";
						break;
					default:
						enfermedadNombre = "error";
						gravedad = "error";
						break;
				}

				String topic = String.format("%s.%s.%s", "respuesta", gravedad, consulta.getPacienteID());
				System.out.println("Respuesta: " + enfermedadNombre);

				consulta.setEnfermedad(enfermedadNombre);
				this.getChannel().basicPublish(EXCHANGE_RESULTADO, topic, MessageProperties.PERSISTENT_TEXT_PLAIN,
						gson.toJson(consulta).getBytes("UTF-8"));

				boolean multiple = false;
				this.getChannel().basicAck(envelope.getDeliveryTag(), multiple);

			} catch (IOException e) {
				System.out.println("Error while decoding Base64: " + e.getMessage());
				handleFail(message, envelope);

			} catch (JsonSyntaxException e) {
				// Manejar la excepción de sintaxis JSON
				System.out.println("Error de sintaxis JSON: " + e.getMessage());
				handleFail(message, envelope);
			} catch (IllegalArgumentException e) {
				// Handle invalid Base64 input
				System.out.println("Invalid Base64 input: " + e.getMessage());
				handleFail(message, envelope);
			} catch (JsonIOException e) {
				// Manejar la excepción de entrada/salida JSON
				System.out.println("Error de entrada/salida JSON: " + e.getMessage());
				handleFail(message, envelope);
			}
		}

		private void handleFail(String message, Envelope envelope) throws IOException {

			Integer intentos = contadores.get(message);
			boolean reprocesar;
			boolean multiple;
			if (intentos == null)
				intentos = 1;

			System.out.println("ERROR: No se ha podido procesar " + message + " intentos: " + intentos);

			if (intentos == MAX_INTENTOS) {
				reprocesar = false;
				multiple = false;
				contadores.remove(message);

				String topic = String.format("%s.%s.%s", "respuesta", "normal", consulta.getPacienteID());
				consulta.setEnfermedad("Ha ocurrido un error, intentelo de nuevo mas tarde");
				this.getChannel().basicPublish(EXCHANGE_RESULTADO, topic, MessageProperties.PERSISTENT_TEXT_PLAIN,
						gson.toJson(consulta).getBytes("UTF-8"));
				
				this.getChannel().basicReject(envelope.getDeliveryTag(), reprocesar);
				System.out.println("Rejected");
			} else {
				reprocesar = true;
				multiple = false;
				contadores.put(message, ++intentos);
				this.getChannel().basicNack(envelope.getDeliveryTag(), multiple, reprocesar);
				System.out.println("Resent");
			}
		}
	}

	public String request(String path) {
		StringBuilder response = new StringBuilder();
		try {
			String url = "http://localhost:5000/predict"; // URL del endpoint

			// Crea la conexión HTTP
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

			// Configura la conexión para peticiones POST
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);

			// Establece los encabezados de la petición
			connection.setRequestProperty("Content-Type", "text/plain");

			// Escribe el cuerpo de la solicitud en el cuerpo de la petición
			byte[] requestBodyBytes = path.getBytes(StandardCharsets.UTF_8);

			connection.setRequestProperty("Content-Length", String.valueOf(requestBodyBytes.length));
			DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
			dataOutputStream.write(requestBodyBytes);
			dataOutputStream.flush();
			dataOutputStream.close();

			// Obtiene la respuesta del servidor
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response.toString();
	}

	public static void main(String[] args) {
		Principal subscriber = new Principal();
		subscriber.subscribe();
	}
}
