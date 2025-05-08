package ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.cbnative;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class ObjectPermissionsId implements Serializable {
    private String objectId;
    private String objectType;
    private String subjectType;
    private String permission;
}
