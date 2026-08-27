package com.automotive.costing.platform.access.repository;
import com.automotive.costing.platform.access.entity.PermissionCatalogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PermissionCatalogEntryRepository extends JpaRepository<PermissionCatalogEntry, String> {
    List<PermissionCatalogEntry> findByActiveTrueOrderBySortOrderAsc();
}
