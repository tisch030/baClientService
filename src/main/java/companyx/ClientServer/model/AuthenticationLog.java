package companyx.ClientServer.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.time.LocalDateTime;

public record AuthenticationLog(@NonNull String id,
                                @Nullable AuthenticationOperation authenticationOperation,
                                @Nullable String ipAddress,
                                boolean ipAddressAnonymized,
                                @Nullable String username,
                                @Nullable String personId,
                                @NonNull LocalDateTime createdAt) {
}
