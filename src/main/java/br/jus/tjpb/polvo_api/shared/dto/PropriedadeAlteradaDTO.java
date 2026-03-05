package br.jus.tjpb.polvo_api.shared.dto;

public class PropriedadeAlteradaDTO {
    private String propriedade;
    private Object valorAntigo;
    private Object valorNovo;

    public String getPropriedade() {
        return propriedade;
    }

    public void setPropriedade(String propriedade) {
        this.propriedade = propriedade;
    }

    public Object getValorAntigo() {
        return valorAntigo;
    }

    public void setValorAntigo(Object valorAntigo) {
        this.valorAntigo = valorAntigo;
    }

    public Object getValorNovo() {
        return valorNovo;
    }

    public void setValorNovo(Object valorNovo) {
        this.valorNovo = valorNovo;
    }
}
