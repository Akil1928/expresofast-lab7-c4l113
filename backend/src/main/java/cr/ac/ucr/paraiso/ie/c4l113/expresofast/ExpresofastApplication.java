package cr.ac.ucr.paraiso.ie.c4l113.expresofast;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ExpresofastApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExpresofastApplication.class, args);
	}

	@Bean
	public Hibernate6Module hibernate6Module() {
		return new Hibernate6Module();
	}
}