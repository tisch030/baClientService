package companyx.ClientServer.model;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.List;

public record UserProfile(@NonNull String userName,
                          @NonNull List<String> activities,
                          @NonNull List<String> modules) {
}
