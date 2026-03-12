package br.jus.tjpb.polvo_api.application.meta.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaResponseDTO;
import br.jus.tjpb.polvo_api.boundaries.api.mapper.MetaMapper;
import br.jus.tjpb.polvo_api.domain.Coordenador;
import br.jus.tjpb.polvo_api.domain.CoordenadorRepository;
import br.jus.tjpb.polvo_api.domain.EixoTematico;
import br.jus.tjpb.polvo_api.domain.EixoTematicoRepository;
import br.jus.tjpb.polvo_api.domain.Meta;
import br.jus.tjpb.polvo_api.domain.MetaRepository;
import br.jus.tjpb.polvo_api.domain.Setor;
import br.jus.tjpb.polvo_api.domain.SetorRepository;

@Service
public class UpdateMetaCommandHandler {
    private final MetaRepository metaRepository;
    private final EixoTematicoRepository eixoTematicoRepository;
    private final SetorRepository setorRepository;
    private final CoordenadorRepository coordenadorRepository;
    private final MetaMapper metaMapper;

    public UpdateMetaCommandHandler(MetaRepository metaRepository,
            EixoTematicoRepository eixoTematicoRepository,
            SetorRepository setorRepository,
            CoordenadorRepository coordenadorRepository,
            MetaMapper metaMapper) {
        this.metaRepository = metaRepository;
        this.eixoTematicoRepository = eixoTematicoRepository;
        this.setorRepository = setorRepository;
        this.coordenadorRepository = coordenadorRepository;
        this.metaMapper = metaMapper;
    }

    @Transactional
    @SuppressWarnings("null")
    public MetaResponseDTO handle(UpdateMetaCommand command) {
        Long id = java.util.Objects.requireNonNull(command.getId(), "ID não pode ser nulo");
        Meta meta = metaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meta não encontrada"));

        var dto = command.getDto();
        metaMapper.updateEntityFromDTO(dto, meta);

        // Resolução de Eixo
        if (dto.getEixoId() != null) {
            meta.setEixo(eixoTematicoRepository.getReferenceById(dto.getEixoId()));
        } else if (dto.getEixoNome() != null && !dto.getEixoNome().isBlank()) {
            EixoTematico eixo = eixoTematicoRepository.findByNome(dto.getEixoNome().trim())
                    .orElseGet(() -> eixoTematicoRepository.save(new EixoTematico(dto.getEixoNome().trim())));
            meta.setEixo(eixo);
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
        }

        // Resolução de Coordenador
        if (dto.getCoordenadorId() != null) {
            meta.setCoordenador(coordenadorRepository.getReferenceById(dto.getCoordenadorId()));
        } else if (dto.getCoordenadorNome() != null && !dto.getCoordenadorNome().isBlank()) {
            String nomeCoord = dto.getCoordenadorNome().trim();
            Coordenador coord = coordenadorRepository.findByNome(nomeCoord)
                    .orElseGet(() -> coordenadorRepository.save(new Coordenador(nomeCoord)));
            meta.setCoordenador(coord);
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
            case TOTALMENTE_CUMPRIDA -> meta.setPontosAtingidos(meta.getPMaximo());
            case NAO_CUMPRIDA -> meta.setPontosAtingidos(java.math.BigDecimal.ZERO);
            case PENDENTE, NAO_SE_APLICA -> meta.setPontosAtingidos(null);
            default -> {
            }
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
