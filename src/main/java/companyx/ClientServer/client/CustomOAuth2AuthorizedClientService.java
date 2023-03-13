package companyx.ClientServer.client;

import edu.umd.cs.findbugs.annotations.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientId;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Custom {@link OAuth2AuthorizedClientService} that is based upon the {@link InMemoryOAuth2AuthorizedClientService}
 * with the differance that before returning a saved authorized client, it will check that the correlated
 * access token is not on the blacklist.
 * Also checks that the access token which is included in the {@link OAuth2AuthorizedClient} is not expired.
 * If that is the case, we return null, which indicates that we don't have an authorized client and force spring
 * to initiate the authorization process again.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

    private static final String BLOCKLIST_KEY_PREFIX = "cc:auth:blocklist:";

    @NonNull
    private final Map<OAuth2AuthorizedClientId, OAuth2AuthorizedClient> authorizedClients;

    @NonNull
    private final ClientRegistrationRepository clientRegistrationRepository;

    @NonNull
    private final RedisTemplate<String, String> blocklistRedisTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(@NonNull final String clientRegistrationId,
                                                                     @NonNull final String principalName) {
        Assert.hasText(clientRegistrationId, "clientRegistrationId cannot be empty");
        Assert.hasText(principalName, "principalName cannot be empty");
        final ClientRegistration registration = this.clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
        if (registration == null) {
            return null;
        }

        final OAuth2AuthorizedClient oAuth2AuthorizedClient = this.authorizedClients.get(new OAuth2AuthorizedClientId(clientRegistrationId, principalName));
        if (oAuth2AuthorizedClient == null) {
            return null;
        }
        final String accessTokenValue = oAuth2AuthorizedClient.getAccessToken().getTokenValue();
        final String tokenValueInsideBlocklist = blocklistRedisTemplate.opsForValue().get(BLOCKLIST_KEY_PREFIX + accessTokenValue);
        if (tokenValueInsideBlocklist != null) {
            return null;
        }


        // Check that the token did not expire - if so, we don't have an authorized client in that case and need to
        // authorize ourselves again.
        // The check here is necessary in order to allow a near instant new access token retrieval,
        // so that the user does not see an error page where its stated that the access token expired.
        // Relying on the RemoveAuthorizedClientOAuth2AuthorizationFailureHandler in our ServletOAuth2AuthorizedClientExchangeFilterFunction
        // is not ideal, because as stated the user would receive an error page where its stated that the token expired.
        // Only after a following request for a resource would trigger a new access token retrieval.
        // By checking the token here, we tell our AuthorizedClientServiceOAuth2AuthorizedClientManager (which
        // is configured for our WebClient Bean) that we don't have a valid authorized client and need to authorized ourselves first.
        final Duration tokenExpiration = Duration.between(Instant.now(), oAuth2AuthorizedClient.getAccessToken().getExpiresAt());
        if (tokenExpiration.isNegative()) {
            return null;
        }

        return (T) oAuth2AuthorizedClient;
    }

    @Override
    public void saveAuthorizedClient(@NonNull final OAuth2AuthorizedClient authorizedClient,
                                     @NonNull final Authentication principal) {
        Assert.notNull(authorizedClient, "authorizedClient cannot be null");
        Assert.notNull(principal, "principal cannot be null");
        this.authorizedClients.put(new OAuth2AuthorizedClientId(
                authorizedClient.getClientRegistration().getRegistrationId(), principal.getName()), authorizedClient);
    }

    @Override
    public void removeAuthorizedClient(@NonNull final String clientRegistrationId,
                                       @NonNull final String principalName) {
        Assert.hasText(clientRegistrationId, "clientRegistrationId cannot be empty");
        Assert.hasText(principalName, "principalName cannot be empty");
        final ClientRegistration registration = this.clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
        if (registration != null) {
            this.authorizedClients.remove(new OAuth2AuthorizedClientId(clientRegistrationId, principalName));
        }
    }
}
