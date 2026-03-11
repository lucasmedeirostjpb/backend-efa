package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaAcompanhamentoRequestDTO;
import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaResponseDTO;
import br.jus.tjpb.polvo_api.boundaries.api.mapper.MetaMapper;
import br.jus.tjpb.polvo_api.domain.Meta;
import br.jus.tjpb.polvo_api.domain.MetaRepository;
import br.jus.tjpb.polvo_api.domain.StatusMeta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateMetaAcompanhamentoCommandHandler {
    private final MetaRepository metaRepository;
    private final MetaMapper metaMapper;

    public UpdateMetaAcompanhamentoCommandHandler(MetaRepository metaRepository, MetaMapper metaMapper) {
        this.metaRepository = metaRepository;
        this.metaMapper = metaMapper;
    }

    @Transactional
    public MetaResponseDTO handle(UpdateMetaAcompanhamentoCommand command) {
        Long id = java.util.Objects.requireNonNull(command.getId(), "ID não pode ser nulo");
        Meta meta = metaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meta não encontrada"));

        MetaAcompanhamentoRequestDTO dto = command.getDto();
        meta.setStatus(dto.getStatus());
        meta.setNivelDificuldade(dto.getNivelDificuldade());
        meta.setEvidenciasAuditoria(dto.getEvidenciasAuditoria());
        meta.setObservacoes(dto.getObservacoes());
        meta.setEstimativaReal(dto.getEstimativaReal());
        meta.setTetoEstimado(dto.getTetoEstimado());
        meta.setPontosAtingidos(dto.getPontosAtingidos());

        sanitizarValoresMatematicos(meta);
        validarRegrasAuditoria(meta);

        Meta savedMeta = metaRepository.save(meta);
        return metaMapper.toDTO(savedMeta);
    }

    private void sanitizarValoresMatematicos(Meta meta) {
        if (meta.getStatus() == null) {
            return;
        }

        if (meta.getStatus() != StatusMeta.EM_ANDAMENTO) {
            meta.setTetoEstimado(null);
            meta.setEstimativaReal(null);
        }

        switch (meta.getStatus()) {
            case TOTALMENTE_CUMPRIDA -> meta.setPontosAtingidos(meta.getPMaximo());
            case NAO_CUMPRIDA -> meta.setPontosAtingidos(java.math.BigDecimal.ZERO);
            case PENDENTE, NAO_SE_APLICA -> meta.setPontosAtingidos(null);
            default -> {
            }
        }
    }

    private void validarRegrasAuditoria(Meta meta) {
        if (meta.getStatus() == StatusMeta.TOTALMENTE_CUMPRIDA ||
                meta.getStatus() == StatusMeta.PARCIALMENTE_CUMPRIDA ||
                meta.getStatus() == StatusMeta.NAO_CUMPRIDA) {

            String evidencias = meta.getEvidenciasAuditoria();
            if (evidencias == null || evidencias.trim().isEmpty() || evidencias.trim().length() < 20) {
                throw new IllegalArgumentException(
                        "Para metas em fase de conclusão, é obrigatório fornecer pelo menos 20 caracteres de evidências para auditoria.");
            }
        }
    }
}