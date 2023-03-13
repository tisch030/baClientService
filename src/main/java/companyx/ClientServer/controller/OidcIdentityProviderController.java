package companyx.ClientServer.controller;

import companyx.ClientServer.model.OidcProvider;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static companyx.ClientServer.controller.IdentityProviderController.BASE_IDENTITY_PROVIDER_INFORMATION_RESOURCE_ENDPOINT;
import static companyx.ClientServer.controller.IdentityProviderController.IDENTITY_PROVIDER_ENDPOINT;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Controller
@RequiredArgsConstructor
public class OidcIdentityProviderController {

    private static final String OIDC_OVERVIEW_ENDPOINT = IDENTITY_PROVIDER_ENDPOINT + "/oidc/{identityProviderId}";
    private static final String OIDC_CREATE_ENDPOINT = IDENTITY_PROVIDER_ENDPOINT + "/oidc/create";
    private static final String OIDC_UPDATE_ENDPOINT = IDENTITY_PROVIDER_ENDPOINT + "/oidc/update/{identityProviderId}";

    private static final String BASE_OIDC_PROVIDER_INFORMATION_RESOURCE_ENDPOINT = BASE_IDENTITY_PROVIDER_INFORMATION_RESOURCE_ENDPOINT + "/oidc";

    @NonNull
    private final WebClient webClient;

    @NonNull
    @GetMapping(value = OIDC_OVERVIEW_ENDPOINT)
    public String retrieveOverviewOidcForm(@PathVariable final String identityProviderId,
                                           @RegisteredOAuth2AuthorizedClient("ba-client") final OAuth2AuthorizedClient authorizedClient,
                                           @NonNull final Model model) {
        final OidcProvider oidcProvider = retrieveOidcProvider(authorizedClient, identityProviderId);
        model.addAttribute("oidcProvider", oidcProvider);
        return "oidc";
    }

    @NonNull
    @GetMapping(value = OIDC_UPDATE_ENDPOINT)
    public String retrieveUpdateOidcForm(@RegisteredOAuth2AuthorizedClient("ba-client") final OAuth2AuthorizedClient authorizedClient,
                                         @PathVariable final String identityProviderId,
                                         @NonNull final Model model) {
        final OidcProvider oidcProvider = retrieveOidcProvider(authorizedClient, identityProviderId);
        model.addAttribute("oidcProvider", oidcProvider);
        return "oidcUpdate";
    }

    @NonNull
    @PostMapping(value = OIDC_UPDATE_ENDPOINT)
    public String updateOidcProvider(@RegisteredOAuth2AuthorizedClient("ba-client") final OAuth2AuthorizedClient authorizedClient,
                                     @PathVariable final String identityProviderId,
                                     @RequestParam @NonNull final Map<String, String> urlParameter,
                                     @NonNull final RedirectAttributes redirectAttributes) {

        final IdentityProviderController.IdentityProviderUpdateContainer identityProviderUpdateContainer = new IdentityProviderController.IdentityProviderUpdateContainer(
                urlParameter.get("name"),
                urlParameter.get("unique-attribute"),
                Integer.parseInt(urlParameter.get("position")),
                urlParameter.get("button-text")
        );

        final OidcIdentityProviderUpdateContainer updateContainer = new OidcIdentityProviderUpdateContainer(
                identityProviderUpdateContainer,
                urlParameter.get("client-id"),
                urlParameter.get("client-secret"),
                Arrays.stream(urlParameter.get("scopes").split(","))
                        .map(String::trim)
                        .toList(),
                urlParameter.get("discovery") != null,
                urlParameter.get("issuer-url"),
                urlParameter.get("authorization-url"),
                urlParameter.get("jwks-url"),
                urlParameter.get("token-url"),
                urlParameter.get("user-info-url")
        );

        final String error = this.webClient
                .post()
                .uri(BASE_OIDC_PROVIDER_INFORMATION_RESOURCE_ENDPOINT + "/" + identityProviderId)
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .bodyValue(updateContainer)
                .exchangeToMono(response -> response.bodyToMono(String.class))
                .block();

        if (error != null && !error.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", error);
            return "redirect:/identity-provider/oidc/update/" + identityProviderId;
        }

        return "redirect:/identity-provider/oidc/" + identityProviderId;
    }

    @NonNull
    @GetMapping(value = OIDC_CREATE_ENDPOINT)
    public String retrieveCreateOidcForm() {
        return "oidcCreate";
    }

    @NonNull
    @PostMapping(value = OIDC_CREATE_ENDPOINT)
    public String createOidcProvider(@RegisteredOAuth2AuthorizedClient("ba-client") final OAuth2AuthorizedClient authorizedClient,
                                     @RequestParam @NonNull final Map<String, String> urlParameter,
                                     @NonNull final RedirectAttributes redirectAttributes) {

        final IdentityProviderController.IdentityProviderCreateContainer identityProviderCreateContainer = new IdentityProviderController.IdentityProviderCreateContainer(
                urlParameter.get("name"),
                urlParameter.get("unique-attribute"),
                Integer.parseInt(urlParameter.get("position")),
                urlParameter.get("button-text")
        );

        final String issuerUrl = urlParameter.get("issuer-url");
        final String authorizationUrl = urlParameter.get("authorization-url");
        final String jwksUrl = urlParameter.get("jwks-url");
        final String tokenUrl = urlParameter.get("token-url");
        final String userInfoUrl = urlParameter.get("user-info-url");

        final OidcIdentityProviderCreateContainer createContainer = new OidcIdentityProviderCreateContainer(
                identityProviderCreateContainer,
                urlParameter.get("client-id"),
                urlParameter.get("client-secret"),
                Arrays.stream(urlParameter.get("scopes").split(","))
                        .map(String::trim)
                        .toList(),
                urlParameter.get("discovery") != null,
                issuerUrl.isEmpty() ? null : issuerUrl,
                authorizationUrl.isEmpty() ? null : authorizationUrl,
                jwksUrl.isEmpty() ? null : jwksUrl,
                tokenUrl.isEmpty() ? null : tokenUrl,
                userInfoUrl.isEmpty() ? null : userInfoUrl
        );


        try {
            final String identityProviderId = this.webClient
                    .post()
                    .uri(BASE_OIDC_PROVIDER_INFORMATION_RESOURCE_ENDPOINT)
                    .attributes(oauth2AuthorizedClient(authorizedClient))
                    .bodyValue(createContainer)
                    .retrieve()
                    .onStatus(HttpStatus.BAD_REQUEST::equals, response ->
                            response.bodyToMono(String.class)
                                    .map(errorMessage -> {
                                        throw new IllegalArgumentException(errorMessage);
                                    }))
                    .bodyToMono(String.class)
                    .block();
            return "redirect:/identity-provider/oidc/" + identityProviderId;
        } catch (final IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/identity-provider/oidc/create";
        }
    }


    @Nullable
    private OidcProvider retrieveOidcProvider(@NonNull final OAuth2AuthorizedClient authorizedClient,
                                              @NonNull final String identityProviderId) {
        return this.webClient
                .get()
                .uri(BASE_OIDC_PROVIDER_INFORMATION_RESOURCE_ENDPOINT + "/" + identityProviderId)
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(OidcProvider.class)
                .block();
    }

    public record OidcIdentityProviderCreateContainer(
            @NonNull IdentityProviderController.IdentityProviderCreateContainer identityProviderCreateContainer,
            @NonNull String clientId,
            @NonNull String clientSecret,
            @NonNull List<String> scopes,
            boolean useDiscovery,
            @Nullable String issuerUrl,
            @Nullable String authorizationUrl,
            @Nullable String jwksUrl,
            @Nullable String tokenUrl,
            @Nullable String userInfoEndpoint) {

    }

    record OidcIdentityProviderUpdateContainer(
            @NonNull IdentityProviderController.IdentityProviderUpdateContainer identityProviderUpdateContainer,
            @NonNull String clientId,
            @NonNull String clientSecret,
            @NonNull List<String> scopes,
            boolean useDiscovery,
            @Nullable String issuerUrl,
            @Nullable String authorizationUrl,
            @Nullable String jwksUrl,
            @Nullable String tokenUrl,
            @Nullable String userInfoEndpoint) {
    }
}
