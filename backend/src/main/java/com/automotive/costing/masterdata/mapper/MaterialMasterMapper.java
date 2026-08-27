package com.automotive.costing.masterdata.mapper;

import com.automotive.costing.masterdata.dto.MaterialMasterDtos.CreateRequest;
import com.automotive.costing.masterdata.dto.MaterialMasterDtos.Response;
import com.automotive.costing.masterdata.entity.MaterialMaster;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MaterialMasterMapper {

    Response toResponse(MaterialMaster entity);

    @Mapping(target = "recordId", ignore = true)
    @Mapping(target = "versionNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    MaterialMaster toEntity(CreateRequest request);

    void updateEntityFromRequest(com.automotive.costing.masterdata.dto.MaterialMasterDtos.UpdateRequest request,
                                  @MappingTarget MaterialMaster entity);
}
