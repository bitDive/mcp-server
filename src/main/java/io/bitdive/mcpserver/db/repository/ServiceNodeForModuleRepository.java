package io.bitdive.mcpserver.db.repository;

import io.bitdive.mcpserver.db.entity.ServiceNodeForModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ServiceNodeForModuleRepository extends JpaRepository<ServiceNodeForModule, Long> {
    @Query(value = """
            select max(snm.id) as id
                      from service_node_for_module snm
                        left join module m on snm.module_id=m.id
                        left join service_for_module sfm on sfm.module_id=snm.module_id and sfm.id=snm.service_id
                    where lower(m.name)=:moduleName and lower(sfm.name)=:serviceName
                   """ ,nativeQuery = true)
    Optional<ServiceNodeForModule> findNativeLastNodeForModuleAndService(String moduleName, String serviceName);
}
