package backend;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.amazonaws.xray.spring.aop.XRayEnabled;

@SpringBootApplication()
@OpenAPIDefinition(info = @Info(title = "My API", version = "1.0", description = "API Documentation"))
@EnableScheduling
@EnableCaching
@XRayEnabled
public class HotelBookingApplication {
	public static void main(String[] args) {
		SpringApplication.run(HotelBookingApplication.class, args);
	}
}