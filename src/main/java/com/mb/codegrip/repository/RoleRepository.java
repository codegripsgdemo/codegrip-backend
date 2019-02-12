package com.mb.codegrip.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mb.codegrip.dto.Roles;

@Repository
public interface RoleRepository extends JpaRepository<Roles, Integer> {
    Set<Roles> findByName(String roleName);

	Set<Roles> findById(Integer id);
    
}