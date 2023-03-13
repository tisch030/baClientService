package companyx.ClientServer.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public record SamlSettings(@NonNull String id,
                           @NonNull String identityProviderId,
                           @NonNull String issuerUrl,
                           @Nullable StorkQaaLevel storkQaaLevel) {
}
