package com.haufe.beercatalogue.beer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.haufe.beercatalogue.beer.dto.BeerRequest;
import com.haufe.beercatalogue.beer.dto.BeerResponse;
import com.haufe.beercatalogue.common.dto.PageResponse;
import com.haufe.beercatalogue.common.exception.NotFoundException;
import com.haufe.beercatalogue.manufacturer.Manufacturer;
import com.haufe.beercatalogue.manufacturer.ManufacturerRepository;
import com.haufe.beercatalogue.manufacturer.ManufacturerTestFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
class BeerServiceTest {

    @Mock
    BeerRepository beerRepository;

    @Mock
    ManufacturerRepository manufacturerRepository;

    @InjectMocks
    BeerService service;

    @Test
    void create_existingManufacturer_savesWithLinkedManufacturerAndReturnsResponse() {
        Manufacturer m = ManufacturerTestFactory.manufacturer(1L, "Heineken", "Netherlands");
        Beer saved = BeerTestFactory.beer(10L, "Corona", new BigDecimal("4.50"), BeerType.LAGER, "Refreshing", m);

        when(manufacturerRepository.findById(1L)).thenReturn(Optional.of(m));
        when(beerRepository.save(any(Beer.class))).thenReturn(saved);

        BeerRequest request = new BeerRequest("Corona", new BigDecimal("4.50"), BeerType.LAGER, "Refreshing", 1L);
        BeerResponse response = service.create(request);

        ArgumentCaptor<Beer> captor = ArgumentCaptor.forClass(Beer.class);
        verify(beerRepository).save(captor.capture());
        Beer captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("Corona");
        assertThat(captured.getAbv()).isEqualByComparingTo("4.50");
        assertThat(captured.getType()).isEqualTo(BeerType.LAGER);
        assertThat(captured.getDescription()).isEqualTo("Refreshing");
        assertThat(captured.getManufacturer()).isSameAs(m);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Corona");
        assertThat(response.manufacturerId()).isEqualTo(1L);
        assertThat(response.manufacturerName()).isEqualTo("Heineken");
    }

    @Test
    void create_missingManufacturer_throwsNotFoundExceptionAndNeverSaves() {
        when(manufacturerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            service.create(new BeerRequest("Beer", BigDecimal.ONE, BeerType.ALE, null, 99L)))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("99");
        verify(beerRepository, never()).save(any());
    }

    @Test
    void getById_existingId_returnsResponse() {
        Manufacturer m = ManufacturerTestFactory.manufacturer(1L, "Guinness Co", "Ireland");
        Beer b = BeerTestFactory.beer(5L, "Stout", new BigDecimal("4.20"), BeerType.STOUT, null, m);
        when(beerRepository.findById(5L)).thenReturn(Optional.of(b));

        BeerResponse response = service.getById(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("Stout");
        assertThat(response.manufacturerName()).isEqualTo("Guinness Co");
    }

    @Test
    void getById_notFound_throwsNotFoundException() {
        when(beerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    void search_noFilters_returnsMappedPage() {
        Manufacturer m = ManufacturerTestFactory.manufacturer(1L, "Heineken", "Netherlands");
        Beer b1 = BeerTestFactory.beer(1L, "Lager", new BigDecimal("5.0"), BeerType.LAGER, null, m);
        Beer b2 = BeerTestFactory.beer(2L, "IPA", new BigDecimal("6.5"), BeerType.IPA, "Hoppy", m);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Beer> page = new PageImpl<>(List.of(b1, b2), pageable, 2);
        when(beerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<BeerResponse> response = service.search(
            new BeerSearchCriteria(null, null, null, null, null, null), pageable);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).name()).isEqualTo("Lager");
        assertThat(response.content().get(1).name()).isEqualTo("IPA");
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    @Test
    void update_existingBeerAndManufacturer_appliesAllChanges() {
        Manufacturer oldM = ManufacturerTestFactory.manufacturer(1L, "Old Brew", "Germany");
        Manufacturer newM = ManufacturerTestFactory.manufacturer(2L, "New Brew", "Belgium");
        Beer existing = BeerTestFactory.beer(7L, "Old Beer", new BigDecimal("4.0"), BeerType.ALE, null, oldM);

        when(beerRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(manufacturerRepository.findById(2L)).thenReturn(Optional.of(newM));
        when(beerRepository.save(existing)).thenReturn(existing);

        BeerResponse response = service.update(7L,
            new BeerRequest("New Beer", new BigDecimal("5.5"), BeerType.IPA, "Updated", 2L));

        assertThat(existing.getName()).isEqualTo("New Beer");
        assertThat(existing.getAbv()).isEqualByComparingTo("5.5");
        assertThat(existing.getType()).isEqualTo(BeerType.IPA);
        assertThat(existing.getDescription()).isEqualTo("Updated");
        assertThat(existing.getManufacturer()).isSameAs(newM);
        assertThat(response.manufacturerName()).isEqualTo("New Brew");
    }

    @Test
    void update_beerNotFound_throwsNotFoundExceptionAndNeverSaves() {
        when(beerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            service.update(99L, new BeerRequest("Beer", BigDecimal.ONE, BeerType.ALE, null, 1L)))
            .isInstanceOf(NotFoundException.class);
        verify(beerRepository, never()).save(any());
    }

    @Test
    void update_manufacturerNotFound_throwsNotFoundExceptionAndNeverSaves() {
        Manufacturer m = ManufacturerTestFactory.manufacturer(1L, "Heineken", "Netherlands");
        Beer existing = BeerTestFactory.beer(7L, "Beer", BigDecimal.ONE, BeerType.ALE, null, m);
        when(beerRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(manufacturerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            service.update(7L, new BeerRequest("Beer", BigDecimal.ONE, BeerType.ALE, null, 99L)))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("99");
        verify(beerRepository, never()).save(any());
    }

    @Test
    void delete_existingId_callsDeleteById() {
        when(beerRepository.existsById(8L)).thenReturn(true);

        service.delete(8L);

        verify(beerRepository).deleteById(8L);
    }

    @Test
    void delete_notFound_throwsNotFoundExceptionAndNeverCallsDeleteById() {
        when(beerRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
            .isInstanceOf(NotFoundException.class);
        verify(beerRepository, never()).deleteById(any());
    }
}
