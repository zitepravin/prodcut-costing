package com.automotive.costing.masterdata.mapper;

import com.automotive.costing.masterdata.dto.MachineMasterDtos.CreateRequest;
import com.automotive.costing.masterdata.dto.MachineMasterDtos.Response;
import com.automotive.costing.masterdata.dto.MachineMasterDtos.UpdateRequest;
import com.automotive.costing.masterdata.entity.MachineMaster;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MachineMasterMapper {

    Response toResponse(MachineMaster entity);

    @Mapping(target = "recordId", ignore = true)
    @Mapping(target = "versionNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    MachineMaster toEntity(CreateRequest request);

    void updateEntityFromRequest(UpdateRequest request, @MappingTarget MachineMaster entity);
}
