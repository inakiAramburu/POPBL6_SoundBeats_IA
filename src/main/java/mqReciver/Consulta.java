package mqReciver;
//import com.fasterxml.jackson.annotation.JsonCreator;
//import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Consulta {

    private String nuss;
    private String audio;
    private String enfermedad;

    public String getEnfermedad() {
		return enfermedad;
	}

	public void setEnfermedad(String enfermedad) {
		this.enfermedad = enfermedad;
	}

	public Consulta(String pacienteID, String audio) {
        this.nuss = pacienteID;
        this.audio = audio;
    }

    public String getPacienteID() {
        return nuss;
    }

    public String getAudio() {
        return audio;
    }

    // Otros métodos y propiedades de la clase...
}