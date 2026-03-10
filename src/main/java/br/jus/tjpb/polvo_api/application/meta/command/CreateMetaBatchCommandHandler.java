package br.jus.tjpb.polvo_api.application.meta.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaRequestDTO;
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
import br.jus.tjpb.polvo_api.domain.StatusMeta;

@Service
public class CreateMetaBatchCommandHandler {
    private final MetaRepository metaRepository;
    private final EixoTematicoRepository eixoTematicoRepository;
    private final SetorRepository setorRepository;
    private final CoordenadorRepository coordenadorRepository;
    private final MetaMapper metaMapper;

    public CreateMetaBatchCommandHandler(MetaRepository metaRepository,
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
    public List<MetaResponseDTO> handle(CreateMetaBatchCommand command) {
        List<MetaResponseDTO> responses = new ArrayList<>();

        for (MetaRequestDTO dto : command.getDtos()) {
            Meta meta = metaMapper.toEntity(dto);

            // Resolução de Eixo (Mesma lógica do CreateMetaCommandHandler)
            if (dto.getEixoId() != null) {
                meta.setEixo(eixoTematicoRepository.getReferenceById(java.util.Objects.requireNonNull(dto.getEixoId())));
            } else if (dto.getEixoNome() != null && !dto.getEixoNome().isBlank()) {
                String nomeEixo = dto.getEixoNome().trim();
                EixoTematico eixo = eixoTematicoRepository.findByNome(nomeEixo)
                        .orElseGet(() -> eixoTematicoRepository.save(new EixoTematico(nomeEixo)));
                meta.setEixo(eixo);
            }

            // Resolução de Setor (Mesma lógica do CreateMetaCommandHandler)
            if (dto.getSetorId() != null) {
                meta.setSetor(setorRepository.getReferenceById(java.util.Objects.requireNonNull(dto.getSetorId())));
            } else if (dto.getSetorNome() != null && !dto.getSetorNome().isBlank()) {
                String nomeSetor = dto.getSetorNome().trim();
                Setor setor = setorRepository.findByNome(nomeSetor)
                        .orElseGet(() -> {
                            String sigla = nomeSetor.length() > 50 ? nomeSetor.substring(0, 50) : nomeSetor;
                            return setorRepository.save(new Setor(sigla, nomeSetor));
                        });
                meta.setSetor(setor);
            }

            // Resolução de Coordenador (find-or-create)
            if (dto.getCoordenadorId() != null) {
                meta.setCoordenador(coordenadorRepository.getReferenceById(java.util.Objects.requireNonNull(dto.getCoordenadorId())));
            } else if (dto.getCoordenadorNome() != null && !dto.getCoordenadorNome().isBlank()) {
                String nomeCoord = dto.getCoordenadorNome().trim();
                Coordenador coord = coordenadorRepository.findByNome(nomeCoord)
                        .orElseGet(() -> coordenadorRepository.save(new Coordenador(nomeCoord)));
                meta.setCoordenador(coord);
            }

            if (meta.getDeadline() == null && dto.getAnoCiclo() != null) {
                meta.setDeadline(java.time.LocalDate.of(dto.getAnoCiclo(), 12, 31));
            }

            sanitizarValoresMatematicos(meta);
            validarRegrasAuditoria(meta);

            Meta savedMeta = metaRepository.save(meta);
            responses.add(metaMapper.toDTO(savedMeta));
        }

        return responses;
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
