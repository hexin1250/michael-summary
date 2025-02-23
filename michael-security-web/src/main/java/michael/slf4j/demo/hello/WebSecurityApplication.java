package michael.slf4j.demo.hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class WebSecurityApplication {

    public static void main(String[] args) throws Throwable {
        SpringApplication.run(WebSecurityApplication.class, args);
    }

}