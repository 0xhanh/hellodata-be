package ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.cbnative;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "cb_object_permissions")
public class ObjectPermissions {
    private String objectId;
    private String objectType;
    private String subjectType;
    protected String permission;
    private LocalDateTime grantTime;
    protected String grantedBy;
}

