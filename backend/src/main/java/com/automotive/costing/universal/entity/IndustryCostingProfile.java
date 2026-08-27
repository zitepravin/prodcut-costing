package com.automotive.costing.universal.entity;
import jakarta.persistence.*; import lombok.*;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="industry_costing_profile")
public class IndustryCostingProfile {
  @Id @Column(name="profile_id",length=36,nullable=false,updatable=false,columnDefinition="CHAR(36)") private String profileId;
  @Column(name="profile_code",length=64,nullable=false) private String profileCode;
  @Column(name="profile_name",length=200,nullable=false) private String profileName;
  @Column(name="description",length=500) private String description;
  @Column(name="default_methods_json",columnDefinition="JSON") private String defaultMethodsJson;
  @Column(name="default_components_json",columnDefinition="JSON") private String defaultComponentsJson;
  @Column(name="default_drivers_json",columnDefinition="JSON") private String defaultDriversJson;
  @Column(name="active",nullable=false) private boolean active=true;
}
