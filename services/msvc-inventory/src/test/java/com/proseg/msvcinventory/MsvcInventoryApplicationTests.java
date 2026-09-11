package com.proseg.msvcinventory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class MsvcInventoryApplicationTests {

    @Test
    void mainClass_instantiationDoesNotRequireSpringContext() {
        assertThatCode(MsvcInventoryApplication::new).doesNotThrowAnyException();
    }

}
