package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaResponseDTO;
import br.jus.tjpb.polvo_api.boundaries.api.mapper.MetaMapper;
import br.jus.tjpb.polvo_api.domain.Meta;
import br.jus.tjpb.polvo_api.domain.MetaRepository;
import br.jus.tjpb.polvo_api.domain.EixoTematicoRepository;
import br.jus.tjpb.polvo_api.domain.SetorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateMetaCommandHandler {
    private final MetaRepository metaRepository;
    private final EixoTematicoRepository eixoTematicoRepository;
    private final SetorRepository setorRepository;
    private final MetaMapper metaMapper;

    public UpdateMetaCommandHandler(MetaRepository metaRepository,
            EixoTematicoRepository eixoTematicoRepository,
            SetorRepository setorRepository,
            MetaMapper metaMapper) {
        this.metaRepository = metaRepository;
        this.eixoTematicoRepository = eixoTematicoRepository;
        this.setorRepository = setorRepository;
        this.metaMapper = metaMapper;
    }

    @Transactional
    @SuppressWarnings("null")
    public MetaResponseDTO handle(UpdateMetaCommand command) {
        Long id = java.util.Objects.requireNonNull(command.getId(), "ID não pode ser nulo");
        Meta meta = metaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meta não encontrada"));

        metaMapper.updateEntityFromDTO(command.getDto(), meta);

        if (command.getDto().getEixoId() != null) {
            meta.setEixo(eixoTematicoRepository
                    .getReferenceById(java.util.Objects.requireNonNull(command.getDto().getEixoId())));
        }
        if (command.getDto().getSetorId() != null) {
            meta.setSetor(
                    setorRepository.getReferenceById(java.util.Objects.requireNonNull(command.getDto().getSetorId())));
        }

        sanitizarValoresMatematicos(meta);

        Meta savedMeta = metaRepository.save(meta);
        return metaMapper.toDTO(savedMeta);
    }

    private void sanitizarValoresMatematicos(Meta meta) {
        if (meta.getStatus() == null) {
            return;
        }

        if (meta.getStatus() != br.jus.tjpb.polvo_api.domain.StatusMeta.EM_ANDAMENTO) {
            meta.setTetoEstimado(null);
            meta.setEstimativaReal(null);
        }

        switch (meta.getStatus()) {
            case TOTALMENTE_CUMPRIDA:
                meta.setPontosAtingidos(meta.getPMaximo());
                break;
            case NAO_CUMPRIDA:
                meta.setPontosAtingidos(java.math.BigDecimal.ZERO);
                break;
            case PENDENTE:
            case NAO_SE_APLICA:
                meta.setPontosAtingidos(null);
                break;
            default:
                break;
        }
    }
}
