package de.maeddes.RestEndpoint;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class RestEndpointApplication {

	@Value("${api.endpoint}")
	String apiEndpoint;

	@GetMapping("${api.endpoint}")
	String sayHello() {

		String returnString;
		if (apiEndpoint.equals("/default"))
			returnString = "api.endpoint env variable probably not set by user. Default is being used. You ended up in: " + apiEndpoint;
		else
			returnString = "saying Hello from: " + apiEndpoint;
		return returnString;
	}

	@GetMapping("/test")
	String test() {

		return "this is static endpoint /test. api.endpoint is set to: " + apiEndpoint;
	}

	public static void main(String[] args) {
		SpringApplication.run(RestEndpointApplication.class, args);
	}

}
