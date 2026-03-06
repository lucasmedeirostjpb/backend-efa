package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaRequestDTO;
import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaResponseDTO;
import br.jus.tjpb.polvo_api.boundaries.api.mapper.MetaMapper;
import br.jus.tjpb.polvo_api.config.security.AppUser;
import br.jus.tjpb.polvo_api.domain.EixoTematico;
import br.jus.tjpb.polvo_api.domain.EixoTematicoRepository;
import br.jus.tjpb.polvo_api.domain.Meta;
import br.jus.tjpb.polvo_api.domain.MetaRepository;
import br.jus.tjpb.polvo_api.domain.Setor;
import br.jus.tjpb.polvo_api.domain.SetorRepository;
import br.jus.tjpb.polvo_api.domain.StatusMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateMetaCommandHandlerTest {

    @Mock
    private MetaRepository metaRepository;
    @Mock
    private EixoTematicoRepository eixoTematicoRepository;
    @Mock
    private SetorRepository setorRepository;
    @Mock
    private MetaMapper metaMapper;

    @InjectMocks
    private CreateMetaCommandHandler handler;

    @org.springframework.lang.NonNull
    private Meta meta = new Meta();
    @org.springframework.lang.NonNull
    private MetaRequestDTO dto = new MetaRequestDTO();
    private AppUser appUser;

    @BeforeEach
    void setup() {
        dto = new MetaRequestDTO();
        dto.setEixoId(1L);
        dto.setSetorId(1L);

        appUser = new AppUser("1", "test", java.util.Set.of());

        meta = new Meta();
        meta.setPMaximo(new BigDecimal("100"));
        meta.setTetoEstimado(new BigDecimal("50"));
        meta.setEstimativaReal(new BigDecimal("40"));
        meta.setPontosAtingidos(new BigDecimal("10"));

        when(metaMapper.toEntity(dto)).thenReturn(meta);
        when(eixoTematicoRepository.getReferenceById(1L)).thenReturn(new EixoTematico());
        when(setorRepository.getReferenceById(1L)).thenReturn(new Setor());
        when(metaRepository.save(meta)).thenReturn(meta);
        when(metaMapper.toDTO(meta)).thenReturn(new MetaResponseDTO());
    }

    @Test
    void shouldClearTetoAndEstimativaIfStatusIsNotEmAndamento() {
        meta.setStatus(StatusMeta.PENDENTE);

        handler.handle(new CreateMetaCommand(appUser, dto));

        assertNull(meta.getTetoEstimado());
        assertNull(meta.getEstimativaReal());
        assertNull(meta.getPontosAtingidos());
    }

    @Test
    void shouldKeepTetoAndEstimativaIfStatusIsEmAndamento() {
        meta.setStatus(StatusMeta.EM_ANDAMENTO);

        handler.handle(new CreateMetaCommand(appUser, dto));

        assertEquals(new BigDecimal("50"), meta.getTetoEstimado());
        assertEquals(new BigDecimal("40"), meta.getEstimativaReal());
        assertEquals(new BigDecimal("10"), meta.getPontosAtingidos());
    }

    @Test
    void shouldSetPontosToPMaximoIfStatusIsTotalmenteCumprida() {
        meta.setStatus(StatusMeta.TOTALMENTE_CUMPRIDA);

        handler.handle(new CreateMetaCommand(appUser, dto));

        assertEquals(new BigDecimal("100"), meta.getPontosAtingidos());
        assertNull(meta.getTetoEstimado());
        assertNull(meta.getEstimativaReal());
    }

    @Test
    void shouldSetPontosToZeroIfStatusIsNaoCumprida() {
        meta.setStatus(StatusMeta.NAO_CUMPRIDA);

        handler.handle(new CreateMetaCommand(appUser, dto));

        assertEquals(BigDecimal.ZERO, meta.getPontosAtingidos());
        assertNull(meta.getTetoEstimado());
        assertNull(meta.getEstimativaReal());
    }

    @Test
    void shouldClearPontosIfStatusIsNaoSeAplica() {
        meta.setStatus(StatusMeta.NAO_SE_APLICA);

        handler.handle(new CreateMetaCommand(appUser, dto));

        assertNull(meta.getPontosAtingidos());
        assertNull(meta.getTetoEstimado());
        assertNull(meta.getEstimativaReal());
    }

    @Test
    void shouldKeepPontosIfStatusIsParcialmenteCumprida() {
        meta.setStatus(StatusMeta.PARCIALMENTE_CUMPRIDA);

        handler.handle(new CreateMetaCommand(appUser, dto));

        assertEquals(new BigDecimal("10"), meta.getPontosAtingidos());
        assertNull(meta.getTetoEstimado());
        assertNull(meta.getEstimativaReal());
    }
}
