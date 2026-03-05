package br.jus.tjpb.polvo_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import co.elastic.apm.attach.ElasticApmAttacher;

@SpringBootApplication
public class PolvoEficienciaEmAcaoApplication {

	public static void main(String[] args) {
		ElasticApmAttacher.attach();
		SpringApplication.run(PolvoEficienciaEmAcaoApplication.class, args);
	}

}
