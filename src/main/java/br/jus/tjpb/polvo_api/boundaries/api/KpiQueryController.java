package br.jus.tjpb.polvo_api.boundaries.api;

import br.jus.tjpb.polvo_api.boundaries.api.dto.DashboardKpiDTO;
import br.jus.tjpb.polvo_api.domain.MetaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/kpis")
public class KpiQueryController {

    private final MetaRepository metaRepository;

    public KpiQueryController(MetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('COORDENADOR', 'DIGOV')")
    public DashboardKpiDTO getDashboard() {
        DashboardKpiDTO raw = metaRepository.obterKpisGlobaisRaw();

        BigDecimal totalMetas = BigDecimal.valueOf(raw.totalMetas() != null ? raw.totalMetas() : 0L);
        BigDecimal aplicáveis = raw.somaPontosAplicaveis() != null ? raw.somaPontosAplicaveis() : BigDecimal.ZERO;
        BigDecimal atingidos = raw.somaPontosAtingidos() != null ? raw.somaPontosAtingidos() : BigDecimal.ZERO;

        BigDecimal percentualTracao = BigDecimal.ZERO;
        if (aplicáveis.compareTo(BigDecimal.ZERO) > 0) {
            percentualTracao = atingidos.divide(aplicáveis, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return new DashboardKpiDTO(
                totalMetas.longValue(),
                aplicáveis,
                atingidos,
                percentualTracao);
    }
}
