package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaResponseDTO;
import br.jus.tjpb.polvo_api.boundaries.api.mapper.MetaMapper;
import br.jus.tjpb.polvo_api.domain.*;
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
        var dto = command.getDto();
        Meta meta = metaMapper.toEntity(dto);

        // Resolução de Eixo
        if (dto.getEixoId() != null) {
            meta.setEixo(eixoTematicoRepository.getReferenceById(dto.getEixoId()));
        } else if (dto.getEixoNome() != null && !dto.getEixoNome().isBlank()) {
            EixoTematico eixo = eixoTematicoRepository.findByNome(dto.getEixoNome().trim())
                    .orElseGet(() -> eixoTematicoRepository.save(new EixoTematico(dto.getEixoNome().trim())));
            meta.setEixo(eixo);
        } else {
            throw new IllegalArgumentException("É necessário informar o eixoId ou o eixoNome.");
        }

        // Resolução de Setor
        if (dto.getSetorId() != null) {
            meta.setSetor(setorRepository.getReferenceById(dto.getSetorId()));
        } else if (dto.getSetorNome() != null && !dto.getSetorNome().isBlank()) {
            String nome = dto.getSetorNome().trim();
            Setor setor = setorRepository.findByNome(nome)
                    .orElseGet(() -> {
                        String sigla = nome.length() > 50 ? nome.substring(0, 50) : nome;
                        return setorRepository.save(new Setor(sigla, nome));
                    });
            meta.setSetor(setor);
        } else {
            throw new IllegalArgumentException("É necessário informar o setorId ou o setorNome.");
        }

        if (meta.getDeadline() == null && dto.getAnoCiclo() != null) {
            meta.setDeadline(java.time.LocalDate.of(dto.getAnoCiclo(), 12, 31));
        }

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
