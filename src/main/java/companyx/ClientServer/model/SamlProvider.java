package companyx.ClientServer.model;

import edu.umd.cs.findbugs.annotations.NonNull;

public record SamlProvider(@NonNull IdentityProvider identityProvider,
                           @NonNull SamlSettings samlSettings,
                           @NonNull SamlServiceProviderInformationUi samlServiceProviderInformationUi) {
}
