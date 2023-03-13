package companyx.ClientServer.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configurer the security filter chain, that defines that only unauthenticated access
 * to our login endpoint is allowed and all further endpoints need an authentication before the
 * access is granted.
 * Also configures the oauth2Login and the logout mechanism for the client.
 * The login triggers the oauth2 authorization process in order to create the authenticated user.
 * Upon successful authentication or logout, we will get redirected to our login page.
 */
@EnableWebSecurity
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(@NonNull final HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/login").permitAll()
                                .anyRequest().authenticated())
                // We want to make use of the oauth login feature, because we want
                // to create an authenticated user after the client receives the authorization
                // code from the authorization server.
                // Another way would be using the oauth2ClientConfigurer instead,
                // which would just exchange the authorization code against an access token, save the token
                // and does not create an authenticated user for the client.
                // But this would mean that we have to let the resource server handle the user identification,
                // before the resources can get returned.
                // To make the temporary implementation of the resource server and client easier, we let the client
                // handle the sessions and user identification.
                .oauth2Login(oauth2Login -> oauth2Login
                        // After successfully log in, we want to get redirected to our homepage/start page.
                        .defaultSuccessUrl("/index")
                        .loginPage("/login"))
                .logout(logout -> logout
                        // After successfully log out, we want to get redirected to our login page.
                        .logoutSuccessUrl("/login"));

        return http.build();
    }
}
