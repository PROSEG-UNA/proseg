package com.proseg.msvc_archive;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class MsvcArchiveApplicationTests {

    @Test
    void mainClass_instantiationDoesNotRequireSpringContext() {
        assertThatCode(MsvcArchiveApplication::new).doesNotThrowAnyException();
    }

}
