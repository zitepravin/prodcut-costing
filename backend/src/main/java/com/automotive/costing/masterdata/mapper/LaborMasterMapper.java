package com.automotive.costing.masterdata.mapper;

import com.automotive.costing.masterdata.dto.LaborMasterDtos.CreateRequest;
import com.automotive.costing.masterdata.dto.LaborMasterDtos.Response;
import com.automotive.costing.masterdata.dto.LaborMasterDtos.UpdateRequest;
import com.automotive.costing.masterdata.entity.LaborMaster;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LaborMasterMapper {

    Response toResponse(LaborMaster entity);

    @Mapping(target = "recordId", ignore = true)
    @Mapping(target = "versionNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    LaborMaster toEntity(CreateRequest request);

    void updateEntityFromRequest(UpdateRequest request, @MappingTarget LaborMaster entity);
}
