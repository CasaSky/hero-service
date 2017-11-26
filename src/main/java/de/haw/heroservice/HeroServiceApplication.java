package de.haw.heroservice;

import de.haw.heroservice.component.HeroDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class HeroServiceApplication {

	public static RestTemplate restTemplate;

	@Autowired
	private HeroDto heroDto;

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		RestTemplate template = builder.build();
		List<HttpMessageConverter<?>> converters = template.getMessageConverters();
		for(HttpMessageConverter<?> converter : converters){
			if(converter instanceof MappingJackson2HttpMessageConverter){
				try{
					((MappingJackson2HttpMessageConverter) converter).setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON));
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		return template;
	}

	public static void main(String[] args) {
		SpringApplication.run(HeroServiceApplication.class, args);
	}
}
