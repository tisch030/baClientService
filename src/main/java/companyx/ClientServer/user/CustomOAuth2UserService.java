package companyx.ClientServer.user;

import companyx.ClientServer.model.UserProfile;
import edu.umd.cs.findbugs.annotations.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that customizes the loading of a OAuth2User authenticated principal.
 * Instead of getting the user information about the authenticated person from the user info endpoint
 * of the authorization server, we look up the user in our resource server,
 * in order to be able to show his custom profile based upon the received personId and credentialsId.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String NAME_ATTRIBUTE_KEY = "username";
    private static final String BASE_IDENTITY_PROVIDER_INFORMATION_RESOURCE_ENDPOINT = "http://localhost:9000/user/";

    @NonNull
    private final WebClient webClient;

    @NonNull
    @Override
    public OAuth2User loadUser(@NonNull final OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        final NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri("http://127.0.0.1:9500/api/auth/jwks").build();
        final String accessTokenValue = userRequest.getAccessToken().getTokenValue();
        final Jwt accessTokenDecoded = jwtDecoder.decode(accessTokenValue);
        final String personId = accessTokenDecoded.getClaims().get("sub").toString();

        // Retrieve user info from resource server
        final UserProfile userProfile = this.webClient
                .get()
                .uri(BASE_IDENTITY_PROVIDER_INFORMATION_RESOURCE_ENDPOINT + personId)
                .headers(header -> header.setBearerAuth(accessTokenValue))
                .retrieve()
                .bodyToMono(UserProfile.class)
                .block();

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("username", userProfile.userName());
        attributes.put("activities", userProfile.activities());
        attributes.put("modules", userProfile.modules());

        return new DefaultOAuth2User(List.of(), attributes, NAME_ATTRIBUTE_KEY);
    }
}
