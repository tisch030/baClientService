package companyx.ClientServer.controller;

import companyx.ClientServer.model.AuthenticationLog;
import edu.umd.cs.findbugs.annotations.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Controller
@RequiredArgsConstructor
public class AuthenticationLogController {

    private static final String BASE_AUTHENTICATION_LOG_RESOURCE_ENDPOINT = "http://localhost:9000/authentication-log";

    @NonNull
    private final WebClient webClient;

    @NonNull
    @GetMapping(value = "/logs")
    public String authenticationLog(@RegisteredOAuth2AuthorizedClient("ba-client") final OAuth2AuthorizedClient authorizedClient,
                                    @NonNull final Model model) {

        final AuthenticationLog[] authenticationLogs = this.webClient
                .get()
                .uri(BASE_AUTHENTICATION_LOG_RESOURCE_ENDPOINT)
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(AuthenticationLog[].class)
                .block();
        model.addAttribute("logs", authenticationLogs);
        return "logs";
    }
}
