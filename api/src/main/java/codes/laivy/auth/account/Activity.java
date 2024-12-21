package codes.laivy.auth.account;

import codes.laivy.address.Address;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public interface Activity {

    @NotNull Address getAddress();
    
    @NotNull Instant getJoinDate();
    @NotNull Instant getQuitDate();

    @NotNull Integer getVersion();

}
