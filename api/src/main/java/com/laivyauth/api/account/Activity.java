package com.laivyauth.api.account;

import codes.laivy.address.Address;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public interface Activity {

    @NotNull Address getAddress();
    
    @NotNull Instant getJoinDate();
    @NotNull Instant getQuitDate();

    int getVersion();

}
