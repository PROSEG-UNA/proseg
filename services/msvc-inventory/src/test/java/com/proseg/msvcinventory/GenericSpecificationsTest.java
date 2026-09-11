package com.proseg.msvcinventory;

import com.proseg.msvcinventory.entity.Asset;
import com.proseg.msvcinventory.specification.GenericSpecifications;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class GenericSpecificationsTest {

    @Test
    @DisplayName("sanitizeSort devuelve solo órdenes válidas y mantiene ordenamiento")
    void sanitizeSort_filtraPropiedadesInvalidas() {
        Sort requested = Sort.by(
                Sort.Order.asc("assetNumber"),
                Sort.Order.desc("nonExisting"),
                Sort.Order.asc("serialNumber")
        );

        Sort sanitized = GenericSpecifications.sanitizeSort(Asset.class, requested);

        assertThat(sanitized.isUnsorted()).isFalse();
        assertThat(sanitized.getOrderFor("assetNumber")).isNotNull();
        assertThat(sanitized.getOrderFor("serialNumber")).isNotNull();
        assertThat(sanitized.getOrderFor("nonExisting")).isNull();
    }

    @Test
    @DisplayName("sanitizeSort con null o sin orden devuelve unsorted")
    void sanitizeSort_nullOSinOrden_retornaUnsorted() {
        Sort s1 = GenericSpecifications.sanitizeSort(Asset.class, null);
        assertThat(s1.isUnsorted()).isTrue();

        Sort s2 = GenericSpecifications.sanitizeSort(Asset.class, Sort.unsorted());
        assertThat(s2.isUnsorted()).isTrue();
    }
}
