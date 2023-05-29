package edu.mondragon.soundbeats.RabbitMQ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestorPacientes {
	final static int NUM_DATOS = 5;
	
	Map<String, List<Integer>> datosPacientes;
	
	public GestorPacientes() {
		datosPacientes = new HashMap<>();
	}
	public double getMedia (String id, int pulsaciones) {
		List<Integer> datosPaciente = datosPacientes.get(id);
		double media = 0;
		if (datosPaciente == null) {
			datosPaciente = new ArrayList<>();
		}
		if (datosPaciente.size()>=NUM_DATOS) {
			datosPaciente.remove(0);
		}
		datosPaciente.add(pulsaciones);
		datosPacientes.put(id, datosPaciente);
		if (datosPaciente.size() == NUM_DATOS) {
			media = datosPaciente.stream().mapToInt(Integer::intValue).average().getAsDouble();
			
		}
		return media;
	}
}
