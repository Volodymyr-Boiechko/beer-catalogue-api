package com.haufe.beercatalogue.manufacturer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.haufe.beercatalogue.common.dto.PageResponse;
import com.haufe.beercatalogue.common.exception.ConflictException;
import com.haufe.beercatalogue.common.exception.NotFoundException;
import com.haufe.beercatalogue.manufacturer.dto.ManufacturerRequest;
import com.haufe.beercatalogue.manufacturer.dto.ManufacturerResponse;
import com.haufe.beercatalogue.security.OwnershipChecker;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ManufacturerServiceTest {

    @Mock
    ManufacturerRepository repository;

    @Mock
    OwnershipChecker ownershipChecker;

    @InjectMocks
    ManufacturerService service;

    @BeforeEach
    void setUpMocks() {
        lenient().when(ownershipChecker.canEditManufacturer(any())).thenReturn(true);
        lenient().when(ownershipChecker.isAdmin()).thenReturn(true);
    }

    @Test
    void create_savesEntityWithCorrectFieldsAndReturnsResponse() {
        Manufacturer saved = ManufacturerTestFactory.manufacturer(1L, "Heineken", "Netherlands");

        when(repository.save(any(Manufacturer.class))).thenReturn(saved);

        ManufacturerResponse response = service.create(new ManufacturerRequest("Heineken", "Netherlands"));

        ArgumentCaptor<Manufacturer> captor = ArgumentCaptor.forClass(Manufacturer.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Heineken");
        assertThat(captor.getValue().getCountry()).isEqualTo("Netherlands");
        assertThat(response).isEqualTo(new ManufacturerResponse(1L, "Heineken", "Netherlands"));
    }

    @Test
    void getById_existingId_returnsResponse() {
        Manufacturer manufacturer = ManufacturerTestFactory.manufacturer(2L, "Guinness", "Ireland");
        when(repository.findById(2L)).thenReturn(Optional.of(manufacturer));

        assertThat(service.getById(2L)).isEqualTo(new ManufacturerResponse(2L, "Guinness", "Ireland"));
    }

    @Test
    void getById_notFound_throwsNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    void search_returnsMappedPage() {
        Manufacturer m1 = ManufacturerTestFactory.manufacturer(1L, "Budweiser", "USA");
        Manufacturer m2 = ManufacturerTestFactory.manufacturer(2L, "Heineken", "Netherlands");
        Pageable pageable = PageRequest.of(0, 20);
        Page<Manufacturer> page = new PageImpl<>(List.of(m1, m2), pageable, 2);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ManufacturerResponse> response = service.search(
            new ManufacturerSearchCriteria(null, null), pageable);

        assertThat(response.content()).containsExactly(
            new ManufacturerResponse(1L, "Budweiser", "USA"),
            new ManufacturerResponse(2L, "Heineken", "Netherlands")
        );
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
    }

    @Test
    void update_existingId_appliesChangesAndReturnsResponse() {
        Manufacturer existing = ManufacturerTestFactory.manufacturer(3L, "Old Name", "Old Country");
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        ManufacturerResponse response = service.update(3L, new ManufacturerRequest("New Name", "New Country"));

        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getCountry()).isEqualTo("New Country");
        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.country()).isEqualTo("New Country");
    }

    @Test
    void update_unchangedKey_skipsConflictCheckAndAlwaysSaves() {
        Manufacturer existing = ManufacturerTestFactory.manufacturer(3L, "Heineken", "Netherlands");
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        ManufacturerResponse response = service.update(3L, new ManufacturerRequest("Heineken", "Netherlands"));

        assertThat(response).isEqualTo(new ManufacturerResponse(3L, "Heineken", "Netherlands"));
        verify(repository, never()).existsByNameAndCountry(any(), any());
        verify(repository).save(existing);
    }

    @Test
    void update_changingKeyToExistingValue_throwsConflictExceptionAndNeverSaves() {
        Manufacturer existing = ManufacturerTestFactory.manufacturer(3L, "Heineken", "Netherlands");
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameAndCountry("Guinness", "Ireland")).thenReturn(true);

        assertThatThrownBy(() -> service.update(3L, new ManufacturerRequest("Guinness", "Ireland")))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already exists");
        verify(repository, never()).save(any());
    }

    @Test
    void update_changingKeyToFreeValue_savesAndReturnsUpdatedResponse() {
        Manufacturer existing = ManufacturerTestFactory.manufacturer(3L, "Old Name", "Old Country");
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameAndCountry("New Name", "New Country")).thenReturn(false);
        when(repository.save(existing)).thenReturn(existing);

        ManufacturerResponse response = service.update(3L, new ManufacturerRequest("New Name", "New Country"));

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.country()).isEqualTo("New Country");
        verify(repository).save(existing);
    }

    @Test
    void create_duplicate_throwsConflictExceptionAndNeverSaves() {
        when(repository.existsByNameAndCountry("Heineken", "Netherlands")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new ManufacturerRequest("Heineken", "Netherlands")))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already exists");
        verify(repository, never()).save(any());
    }

    @Test
    void update_notFound_throwsNotFoundExceptionAndNeverSaves() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new ManufacturerRequest("Name", "Country")))
            .isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void delete_existingId_callsDeleteById() {
        when(repository.existsById(4L)).thenReturn(true);

        service.delete(4L);

        verify(repository).deleteById(4L);
    }

    @Test
    void delete_notFound_throwsNotFoundExceptionAndNeverCallsDeleteById() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
            .isInstanceOf(NotFoundException.class);
        verify(repository, never()).deleteById(any());
    }
}
