package com.proseg.msvcinventory;

import com.proseg.msvcinventory.dto.request.AssetRequestDto;
import com.proseg.msvcinventory.dto.response.AssetResponseDto;
import com.proseg.msvcinventory.entity.Asset;
import com.proseg.msvcinventory.entity.Location;
import com.proseg.msvcinventory.entity.Model;
import com.proseg.msvcinventory.exception.AssetException;
import com.proseg.msvcinventory.mapper.AssetMapper;
import com.proseg.msvcinventory.repository.*;
import com.proseg.msvcinventory.service.impl.AssetServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceImplTest {

    @Mock
    private com.proseg.msvcinventory.repository.AssetRepository assetRepository;
    @Mock
    private com.proseg.msvcinventory.repository.ModelRepository modelRepository;
    @Mock
    private com.proseg.msvcinventory.repository.LocationRepository locationRepository;
    @Mock
    private com.proseg.msvcinventory.repository.TypeRepository typeRepository;
    @Mock
    private com.proseg.msvcinventory.repository.NetworkInterfaceRepository networkInterfaceRepository;
    @Mock
    private com.proseg.msvcinventory.repository.AssetArchiveRepository assetArchiveRepository;
    @Mock
    private com.proseg.msvcinventory.repository.AssetComponentRepository assetComponentRepository;
    @Mock
    private com.proseg.msvcinventory.repository.ExecutingUnitRepository executingUnitRepository;
    @Mock
    private com.proseg.msvcinventory.repository.EmployeeRepository employeeRepository;
    @Mock
    private AssetMapper assetMapper;

    @InjectMocks
    private AssetServiceImpl service;

    @Test
    @DisplayName("create: cuando el número de serie ya existe lanza excepción")
    void create_cuandoSerialDuplicado_lanzaExcepcion() {
        AssetRequestDto req = new AssetRequestDto();
        req.setSerialNumber("SER-1");

        when(assetRepository.existsBySerialNumber("SER-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req)).isInstanceOf(AssetException.class);

        verify(assetRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: camino feliz crea y devuelve asset")
    void create_caminoFeliz_guardaYRetorna() {
        AssetRequestDto req = new AssetRequestDto();
        req.setModelId(UUID.randomUUID());
        req.setLocationId(UUID.randomUUID());
        req.setSerialNumber("SER-123");

        Model model = new Model();
        model.setId(req.getModelId());
        Location location = new Location();
        location.setId(req.getLocationId());

        Asset entity = new Asset();
        entity.setId(UUID.randomUUID());

        AssetResponseDto responseDto = new AssetResponseDto();
        responseDto.setId(entity.getId());

        when(assetRepository.existsBySerialNumber("SER-123")).thenReturn(false);
        when(modelRepository.findById(req.getModelId())).thenReturn(Optional.of(model));
        when(locationRepository.findById(req.getLocationId())).thenReturn(Optional.of(location));
        when(assetMapper.toEntity(req)).thenReturn(entity);
        when(assetRepository.save(entity)).thenReturn(entity);
        when(assetRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(assetMapper.toResponse(entity)).thenReturn(responseDto);

        AssetResponseDto result = service.create(req);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(entity.getId());
        verify(assetRepository, times(1)).save(entity);
    }

    @Test
    @DisplayName("findById: cuando existe retorna DTO")
    void findById_cuandoExiste_retornaDTO() {
        UUID id = UUID.randomUUID();
        Asset asset = new Asset();
        asset.setId(id);
        AssetResponseDto dto = new AssetResponseDto();
        dto.setId(id);

        when(assetRepository.findById(id)).thenReturn(Optional.of(asset));
        when(assetMapper.toResponse(asset)).thenReturn(dto);

        AssetResponseDto res = service.findById(id);

        assertThat(res).isNotNull();
        assertThat(res.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("existsByAssetNumber delega a repository con y sin excludeId")
    void existsByAssetNumber_delegacionCorrecta() {
        String number = "A-1";
        UUID exclude = UUID.randomUUID();

        when(assetRepository.existsByAssetNumber(number)).thenReturn(true);
        when(assetRepository.existsByAssetNumberAndIdNot(number, exclude)).thenReturn(false);

        assertThat(service.existsByAssetNumber(number, null)).isTrue();
        assertThat(service.existsByAssetNumber(number, exclude)).isFalse();
    }

    @Test
    @DisplayName("findAll: delega a repository y enriquece conteos")
    void findAll_delegacionYEnriquecimiento() {
        AssetResponseDto dto = new AssetResponseDto();
        dto.setId(UUID.randomUUID());
        when(assetMapper.toResponse(any())).thenReturn(dto);
        when(assetRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(new Asset())));
        when(assetArchiveRepository.countByAssetIds(any())).thenReturn(List.of());
        when(assetComponentRepository.countByAssetIds(any())).thenReturn(List.of());

        var res = service.findAll(null, null, PageRequest.of(0, 10));
        assertThat(res).isNotNull();
    }
}
