# michael-security-web
Spring provides Spring-security, which can do permission filter in spring MVC.

pom dependency.
```
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

MVC config in JAVA
```
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/home").setViewName("home");
		registry.addViewController("/").setViewName("home");
		registry.addViewController("/hello").setViewName("hello");
		registry.addViewController("/login").setViewName("login");
	}
}

```

Web Security Config in JAVA
```
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers("/", "/home").permitAll().anyRequest().authenticated()
			.and().formLogin().loginPage("/login").permitAll()
			.and().logout().permitAll();
	}

	@Bean
	@Override
	public UserDetailsService userDetailsService() {
		UserDetails user = User.withDefaultPasswordEncoder().username("user").password("password").roles("USER")
				.build();

		return new InMemoryUserDetailsManager(user);
	}

}
```

login template
```
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
	xmlns:th="http://www.thymeleaf.org"
	xmlns:sec="http://www.thymeleaf.org/thymeleaf-extras-springsecurity3">
	<head>
		<title>Spring Security Example</title>
	</head>
	<body>
		<div th:if="${param.error}">Invalid username and password.</div>
		<div th:if="${param.logout}">You have been logged out.</div>
		<form th:action="@{/login}" method="post">
			<div>
				<label> User Name : <input type="text" name="username" />
				</label>
			</div>
			<div>
				<label> Password: <input type="password" name="password" />
				</label>
			</div>
			<div>
				<input type="submit" value="Sign In" />
			</div>
		</form>
	</body>
</html>
```

This is very useful. It's easy to set up security for each module.

## References
* For quick start, please refer to [Getting-Started](https://spring.io/guides/gs/securing-web/).
* For future information, please refer to [architecture](https://spring.io/guides/topicals/spring-security-architecture).
