package br.jus.tjpb.polvo_api.shared.dto;

import java.time.LocalDateTime;
import java.util.List;

public class HistoricoAlteracaoDTO {
    private String autor;
    private LocalDateTime dataHora;
    private String tipoMudanca;
    private List<PropriedadeAlteradaDTO> propriedadesAlteradas;

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getTipoMudanca() {
        return tipoMudanca;
    }

    public void setTipoMudanca(String tipoMudanca) {
        this.tipoMudanca = tipoMudanca;
    }

    public List<PropriedadeAlteradaDTO> getPropriedadesAlteradas() {
        return propriedadesAlteradas;
    }

    public void setPropriedadesAlteradas(List<PropriedadeAlteradaDTO> propriedadesAlteradas) {
        this.propriedadesAlteradas = propriedadesAlteradas;
    }
}
