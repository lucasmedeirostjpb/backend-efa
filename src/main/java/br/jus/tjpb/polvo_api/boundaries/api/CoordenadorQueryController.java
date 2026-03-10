package br.jus.tjpb.polvo_api.boundaries.api;

import br.jus.tjpb.polvo_api.boundaries.api.dto.CoordenadorResponseDTO;
import br.jus.tjpb.polvo_api.boundaries.api.mapper.CoordenadorMapper;
import br.jus.tjpb.polvo_api.domain.CoordenadorRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/coordenadores")
public class CoordenadorQueryController {

    private final CoordenadorRepository coordenadorRepository;
    private final CoordenadorMapper coordenadorMapper;

    public CoordenadorQueryController(CoordenadorRepository coordenadorRepository,
            CoordenadorMapper coordenadorMapper) {
        this.coordenadorRepository = coordenadorRepository;
        this.coordenadorMapper = coordenadorMapper;
    }

    @GetMapping
    public List<CoordenadorResponseDTO> listarTodos() {
        return coordenadorRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"))
                .stream()
                .map(coordenadorMapper::toDTO)
                .collect(Collectors.toList());
    }
}
