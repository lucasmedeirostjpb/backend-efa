package br.jus.tjpb.polvo_api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class DomainEntityAuditableUpdate extends DomainEntityAuditableCreate {

    @LastModifiedDate
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @LastModifiedBy
    @Column(name = "usuario_atualizacao")
    private String usuarioAtualizacao;

}
