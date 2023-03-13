package companyx.ClientServer.controller;

import companyx.ClientServer.model.IdentityProvider;
import edu.umd.cs.findbugs.annotations.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Controller
@RequiredArgsConstructor
public class IdentityProviderController {

    public static final String IDENTITY_PROVIDER_ENDPOINT = "/identity-provider";
    public static final String IDENTITY_PROVIDER_SPECIFIC_ENDPOINT = "/identity-provider/{identityProviderId}";

    public static final String BASE_IDENTITY_PROVIDER_INFORMATION_RESOURCE_ENDPOINT = "http://localhost:9000/identity-provider";

    @NonNull
    private final WebClient webClient;


    @NonNull
    @GetMapping(value = IDENTITY_PROVIDER_ENDPOINT)
    public String identityProviders(@NonNull final Model model,
                                    @RegisteredOAuth2AuthorizedClient("ba-client") final OAuth2AuthorizedClient authorizedClient) {

        final IdentityProvider[] idp = this.webClient
                .get()
                .uri(BASE_IDENTITY_PROVIDER_INFORMATION_RESOURCE_ENDPOINT)
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(IdentityProvider[].class)
                .block();

        if (idp != null) {
            final List<IdentityProvider> identityProvidersList = Arrays.stream(idp)
                    .sorted(Comparator.comparingInt(IdentityProvider::position))
                    .toList();
            model.addAttribute("idps", identityProvidersList);
        }

        return "idp";
    }


    @NonNull
    @PostMapping(path = IDENTITY_PROVIDER_SPECIFIC_ENDPOINT,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String changeIdentityProvider(@RegisteredOAuth2AuthorizedClient("ba-client") final OAuth2AuthorizedClient authorizedClient,
                                         @PathVariable final String identityProviderId,
                                         @RequestParam @NonNull final Map<String, String> urlParameter) {

        final String status = urlParameter.get("status");
        if (status == null) {
            this.webClient
                    .delete()
                    .uri(BASE_IDENTITY_PROVIDER_INFORMATION_RESOURCE_ENDPOINT + "/" + identityProviderId)
                    .attributes(oauth2AuthorizedClient(authorizedClient))
                    .retrieve()
                    .toEntity(Void.class)
                    .block();
        } else {
            final boolean idpCurrentlyEnabled = Boolean.parseBoolean(status);
            final Map<String, String> bodyMap = new HashMap<>();
            bodyMap.put("enable", Boolean.toString(!idpCurrentlyEnabled));

            this.webClient
                    .post()
                    .uri(BASE_IDENTITY_PROVIDER_INFORMATION_RESOURCE_ENDPOINT + "/" + identityProviderId)
                    .bodyValue(bodyMap)
                    .attributes(oauth2AuthorizedClient(authorizedClient))
                    .retrieve()
                    .toEntity(Void.class)
                    .block();
        }

        return "redirect:/identity-provider";
    }


    public record IdentityProviderCreateContainer(@NonNull String name,
                                                  @NonNull String uniqueIdentifierAttributeAtIdp,
                                                  int position,
                                                  @NonNull String buttonLabel) {
    }


    public record IdentityProviderUpdateContainer(@NonNull String name,
                                                  @NonNull String uniqueIdentifierAttributeAtIdp,
                                                  int position,
                                                  @NonNull String buttonLabel) {
    }
}

