package com.experimentops.platformapi.model.entity;

import com.experimentops.platformapi.model.type.StatusEnum;
import com.experimentops.platformapi.model.type.converter.StatusEnumConverter;
import com.experimentops.utils.ExperimentOpsJsonType;
import com.experimentops.utils.model.entity.ExperimentOpsEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name="tbl_experiment_types")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentType extends ExperimentOpsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    @Type(ExperimentOpsJsonType.class)
    @Column(name = "default_config", columnDefinition = "json")
    private List<ExperimentTypeDefaultConfig> defaultConfig;

    @Type(ExperimentOpsJsonType.class)
    @Column(name = "format_mappings", columnDefinition = "json")
    private List<ExperimentTypeFormatMapping> formatMappings;

    @Column(name ="status")
    @Convert(converter = StatusEnumConverter.class)
    private StatusEnum status;

    @Column(name = "time_weight")
    private Integer timeWeight;

    @Override
    @Generated
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExperimentType)) return false;
        return (getUuid().equals(((ExperimentType) o).getUuid()));
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(getUuid());
    }
}
