package companyx.ClientServer.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public record SamlServiceProviderInformationUi(@NonNull String entityId,
                                               @Nullable String idpInitiatedLogoutEndpoint,
                                               @Nullable String x509certificate) {
}
