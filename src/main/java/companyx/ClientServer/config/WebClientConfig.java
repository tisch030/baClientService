package companyx.ClientServer.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configurer for a WebClient bean.
 * Uses the {@link ServletOAuth2AuthorizedClientExchangeFilterFunction} that automatically
 * removes a saved {@link OAuth2AuthorizedClient} so that further requests lead to a new oauth2 authorization
 * process with our authorization server, if for some reason an outdated authorized client will be used for
 * retrieving information from a resource server.
 * <p>
 * The {@link ServletOAuth2AuthorizedClientExchangeFilterFunction} is configured to use a {@link OAuth2AuthorizedClientManager}
 * that is configured to only use the authorization code flow.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(@NonNull final OAuth2AuthorizedClientManager authorizedClientManager) {

        final ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client = new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        return WebClient.builder()
                .apply(oauth2Client.oauth2Configuration())
                .build();
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(@NonNull final ClientRegistrationRepository clientRegistrationRepository,
                                                                 @NonNull final OAuth2AuthorizedClientService authorizedClientService) {

        final OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .build();
        final AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

}
