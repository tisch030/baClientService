package companyx.ClientServer.model;

import edu.umd.cs.findbugs.annotations.NonNull;

public record OidcProvider(@NonNull IdentityProvider identityProvider,
                           @NonNull OidcSettings oidcSettings) {
}