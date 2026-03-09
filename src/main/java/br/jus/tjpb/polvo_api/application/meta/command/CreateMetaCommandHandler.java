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
public class CreateMetaCommandHandler {
    private final MetaRepository metaRepository;
    private final EixoTematicoRepository eixoTematicoRepository;
    private final SetorRepository setorRepository;
    private final MetaMapper metaMapper;

    public CreateMetaCommandHandler(MetaRepository metaRepository,
            EixoTematicoRepository eixoTematicoRepository,
            SetorRepository setorRepository,
            MetaMapper metaMapper) {
        this.metaRepository = metaRepository;
        this.eixoTematicoRepository = eixoTematicoRepository;
        this.setorRepository = setorRepository;
        this.metaMapper = metaMapper;
    }

    @Transactional
    public MetaResponseDTO handle(CreateMetaCommand command) {
        Meta meta = metaMapper.toEntity(command.getDto());

        meta.setEixo(eixoTematicoRepository
                .getReferenceById(java.util.Objects.requireNonNull(command.getDto().getEixoId())));
        meta.setSetor(
                setorRepository.getReferenceById(java.util.Objects.requireNonNull(command.getDto().getSetorId())));

        sanitizarValoresMatematicos(meta);
        validarRegrasAuditoria(meta);

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

    private void validarRegrasAuditoria(Meta meta) {
        if (meta.getStatus() == br.jus.tjpb.polvo_api.domain.StatusMeta.TOTALMENTE_CUMPRIDA ||
                meta.getStatus() == br.jus.tjpb.polvo_api.domain.StatusMeta.PARCIALMENTE_CUMPRIDA ||
                meta.getStatus() == br.jus.tjpb.polvo_api.domain.StatusMeta.NAO_CUMPRIDA) {

            String evidencias = meta.getEvidenciasAuditoria();
            if (evidencias == null || evidencias.trim().isEmpty() || evidencias.trim().length() < 20) {
                throw new IllegalArgumentException(
                        "Para metas em fase de conclusão, é obrigatório fornecer pelo menos 20 caracteres de evidências para auditoria.");
            }
        }
    }
}
