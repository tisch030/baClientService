package companyx.ClientServer.controller;

import companyx.ClientServer.model.SamlProvider;
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

import java.util.Map;

import static companyx.ClientServer.controller.IdentityProviderController.BASE_IDENTITY_PROVIDER_INFORMATION_RESOURCE_ENDPOINT;
import static companyx.ClientServer.controller.IdentityProviderController.IDENTITY_PROVIDER_ENDPOINT;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Controller
@RequiredArgsConstructor
public class SamlIdentityProviderController {

    private static final String SAML_OVERVIEW_ENDPOINT = IDENTITY_PROVIDER_ENDPOINT + "/saml/{identityProviderId}";
    private static final String SAML_CREATE_ENDPOINT = IDENTITY_PROVIDER_ENDPOINT + "/saml/create";
    private static final String SAML_UPDATE_ENDPOINT = IDENTITY_PROVIDER_ENDPOINT + "/saml/update/{identityProviderId}";

    private static final String BASE_SAML_PROVIDER_INFORMATION_RESOURCE_ENDPOINT = BASE_IDENTITY_PROVIDER_INFORMATION_RESOURCE_ENDPOINT + "/saml";

    @NonNull
    private final WebClient webClient;

    @NonNull
    @GetMapping(value = SAML_OVERVIEW_ENDPOINT)
    public String retrieveOverviewSamlForm(@PathVariable final String identityProviderId,
                                           @RegisteredOAuth2AuthorizedClient("ba-client") final OAuth2AuthorizedClient authorizedClient,
                                           @NonNull final Model model) {
        final SamlProvider samlProvider = retrieveSamlProvider(authorizedClient, identityProviderId);
        model.addAttribute("samlProvider", samlProvider);
        return "saml";
    }

    @NonNull
    @GetMapping(value = SAML_UPDATE_ENDPOINT)
    public String retrieveUpdateSmlForm(@RegisteredOAuth2AuthorizedClient("ba-client") final OAuth2AuthorizedClient authorizedClient,
                                        @PathVariable final String identityProviderId,
                                        @NonNull final Model model) {
        final SamlProvider samlProvider = retrieveSamlProvider(authorizedClient, identityProviderId);
        model.addAttribute("samlProvider", samlProvider);
        return "samlUpdate";
    }

    @NonNull
    @PostMapping(value = SAML_UPDATE_ENDPOINT)
    public String updateSamlProvider(@RegisteredOAuth2AuthorizedClient("ba-client") final OAuth2AuthorizedClient authorizedClient,
                                     @PathVariable final String identityProviderId,
                                     @RequestParam @NonNull final Map<String, String> urlParameter,
                                     @NonNull final RedirectAttributes redirectAttributes) {

        final IdentityProviderController.IdentityProviderUpdateContainer identityProviderUpdateContainer = new IdentityProviderController.IdentityProviderUpdateContainer(
                urlParameter.get("name"),
                urlParameter.get("unique-attribute"),
                Integer.parseInt(urlParameter.get("position")),
                urlParameter.get("button-text")
        );

        final SamlIdentityProviderUpdateContainer updateContainer = new SamlIdentityProviderUpdateContainer(
                identityProviderUpdateContainer,
                urlParameter.get("issuer-url"),
                urlParameter.get("stork-qaa")
        );

        final String error = this.webClient
                .post()
                .uri(BASE_SAML_PROVIDER_INFORMATION_RESOURCE_ENDPOINT + "/" + identityProviderId)
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .bodyValue(updateContainer)
                .exchangeToMono(response -> response.bodyToMono(String.class))
                .block();

        if (error != null && !error.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", error);
            return "redirect:/identity-provider/saml/update/" + identityProviderId;
        }

        return "redirect:/identity-provider/saml/" + identityProviderId;
    }

    @NonNull
    @GetMapping(value = SAML_CREATE_ENDPOINT)
    public String retrieveCreateSamlForm() {
        return "samlCreate";
    }

    @NonNull
    @PostMapping(value = SAML_CREATE_ENDPOINT)
    public String createSamlProvider(@RegisteredOAuth2AuthorizedClient("ba-client") final OAuth2AuthorizedClient authorizedClient,
                                     @RequestParam @NonNull final Map<String, String> urlParameter,
                                     @NonNull final RedirectAttributes redirectAttributes) {

        final IdentityProviderController.IdentityProviderCreateContainer identityProviderCreateContainer = new IdentityProviderController.IdentityProviderCreateContainer(
                urlParameter.get("name"),
                urlParameter.get("unique-attribute"),
                Integer.parseInt(urlParameter.get("position")),
                urlParameter.get("button-text")
        );

        final String issuerUrl = urlParameter.get("issuer-url");
        final String storkQaa = urlParameter.get("stork-qaa");
        final SamlIdentityProviderCreateContainer createContainer = new SamlIdentityProviderCreateContainer(
                identityProviderCreateContainer,
                issuerUrl,
                storkQaa == null || storkQaa.isEmpty() ? null : storkQaa
        );


        try {
            final String identityProviderId = this.webClient
                    .post()
                    .uri(BASE_SAML_PROVIDER_INFORMATION_RESOURCE_ENDPOINT)
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
            return "redirect:/identity-provider/saml/" + identityProviderId;
        } catch (final IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/identity-provider/saml/create";
        }
    }


    @Nullable
    private SamlProvider retrieveSamlProvider(@NonNull final OAuth2AuthorizedClient authorizedClient,
                                              @NonNull final String identityProviderId) {
        return this.webClient
                .get()
                .uri(BASE_SAML_PROVIDER_INFORMATION_RESOURCE_ENDPOINT + "/" + identityProviderId)
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(SamlProvider.class)
                .block();
    }

    public record SamlIdentityProviderCreateContainer(
            @NonNull IdentityProviderController.IdentityProviderCreateContainer identityProviderCreateContainer,
            @NonNull String issuerUrl,
            @Nullable String storkQaaLevel) {

    }

    record SamlIdentityProviderUpdateContainer(
            @NonNull IdentityProviderController.IdentityProviderUpdateContainer identityProviderUpdateContainer,
            @NonNull String issuerUrl,
            @Nullable String storkQaaLevel) {
    }

}
