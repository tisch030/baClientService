package companyx.ClientServer.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.util.List;

public record OidcSettings(@NonNull String id,
                           @NonNull String identityProviderId,
                           @NonNull String clientId,
                           @NonNull String clientSecret,
                           @Nullable List<String> scopes,
                           boolean useDiscovery,
                           @Nullable String issuerUrl,
                           @Nullable String authorizationUrl,
                           @Nullable String jwksUrl,
                           @Nullable String tokenUrl,
                           @Nullable String userInfoEndpoint) {
}
