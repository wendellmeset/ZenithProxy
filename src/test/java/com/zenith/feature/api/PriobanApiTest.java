package com.zenith.feature.api;

import com.zenith.Shared;
import com.zenith.feature.api.prioban.PriobanApi;
import org.junit.jupiter.api.Assertions;

import java.util.Optional;

public class PriobanApiTest {

//    @BeforeAll
    public static void setup() {
        Shared.loadConfig();
        Shared.loadLaunchConfig();
    }

//    @Test
    public void notBannedCheck() {
        Optional<Boolean> b = PriobanApi.INSTANCE.checkPrioBan("rfresh2");
        Assertions.assertTrue(b.isPresent());
        Assertions.assertFalse(b.get());
    }

//    @Test
    public void bannedCheck() {
        Optional<Boolean> b = PriobanApi.INSTANCE.checkPrioBan("jared2013");
        Assertions.assertTrue(b.isPresent());
        Assertions.assertTrue(b.get());
    }
}
