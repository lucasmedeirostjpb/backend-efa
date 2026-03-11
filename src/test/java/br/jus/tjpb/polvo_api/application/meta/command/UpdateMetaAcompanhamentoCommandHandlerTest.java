package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaAcompanhamentoRequestDTO;
import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaResponseDTO;
import br.jus.tjpb.polvo_api.boundaries.api.mapper.MetaMapper;
import br.jus.tjpb.polvo_api.config.security.AppUser;
import br.jus.tjpb.polvo_api.config.security.AppUserRoles;
import br.jus.tjpb.polvo_api.domain.Meta;
import br.jus.tjpb.polvo_api.domain.MetaRepository;
import br.jus.tjpb.polvo_api.domain.NivelDificuldade;
import br.jus.tjpb.polvo_api.domain.StatusMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMetaAcompanhamentoCommandHandlerTest {

    @Mock
    private MetaRepository metaRepository;
    @Mock
    private MetaMapper metaMapper;

    @InjectMocks
    private UpdateMetaAcompanhamentoCommandHandler handler;

    private Meta meta;
    private MetaAcompanhamentoRequestDTO dto;
    private AppUser appUser;

    @BeforeEach
    void setup() {
        dto = new MetaAcompanhamentoRequestDTO();
        dto.setStatus(StatusMeta.EM_ANDAMENTO);
        dto.setNivelDificuldade(NivelDificuldade.EM_ALERTA);
        dto.setEvidenciasAuditoria("Evidência de auditoria válida com mais de vinte caracteres.");
        dto.setObservacoes("Observação atualizada");
        dto.setEstimativaReal(new BigDecimal("40"));
        dto.setTetoEstimado(new BigDecimal("50"));
        dto.setPontosAtingidos(new BigDecimal("10"));

        appUser = new AppUser("coord", "coord", Set.of(AppUserRoles.COORDENADOR));

        meta = new Meta();
        meta.setTitulo("Meta estrutural");
        meta.setDescricao("Descrição original");
        meta.setArtigo("Art. 7");
        meta.setAnoCiclo(2026);
        meta.setPMaximo(new BigDecimal("100"));
        meta.setStatus(StatusMeta.PENDENTE);

        when(metaRepository.findById(1L)).thenReturn(Optional.of(meta));
    }

    @Test
    void shouldUpdateOnlyAcompanhamentoFieldsAndPreserveStructure() {
        when(metaRepository.save(meta)).thenReturn(meta);
        when(metaMapper.toDTO(meta)).thenReturn(new MetaResponseDTO());

        handler.handle(new UpdateMetaAcompanhamentoCommand(appUser, 1L, dto));

        assertEquals("Meta estrutural", meta.getTitulo());
        assertEquals("Descrição original", meta.getDescricao());
        assertEquals("Art. 7", meta.getArtigo());
        assertEquals(2026, meta.getAnoCiclo());
        assertEquals(new BigDecimal("100"), meta.getPMaximo());
        assertEquals(StatusMeta.EM_ANDAMENTO, meta.getStatus());
        assertEquals(NivelDificuldade.EM_ALERTA, meta.getNivelDificuldade());
        assertEquals("Observação atualizada", meta.getObservacoes());
        assertEquals(new BigDecimal("40"), meta.getEstimativaReal());
        assertEquals(new BigDecimal("50"), meta.getTetoEstimado());
        assertEquals(new BigDecimal("10"), meta.getPontosAtingidos());
    }

    @Test
    void shouldApplyMathSanitizationForTotalmenteCumprida() {
        dto.setStatus(StatusMeta.TOTALMENTE_CUMPRIDA);
        when(metaRepository.save(meta)).thenReturn(meta);
        when(metaMapper.toDTO(meta)).thenReturn(new MetaResponseDTO());

        handler.handle(new UpdateMetaAcompanhamentoCommand(appUser, 1L, dto));

        assertNull(meta.getEstimativaReal());
        assertNull(meta.getTetoEstimado());
        assertEquals(new BigDecimal("100"), meta.getPontosAtingidos());
    }

    @Test
    void shouldRequireAuditoriaEvidenceForCompletedStatuses() {
        dto.setStatus(StatusMeta.NAO_CUMPRIDA);
        dto.setEvidenciasAuditoria("curta");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> handler.handle(new UpdateMetaAcompanhamentoCommand(appUser, 1L, dto)));

        assertEquals(
                "Para metas em fase de conclusão, é obrigatório fornecer pelo menos 20 caracteres de evidências para auditoria.",
                exception.getMessage());
    }
}