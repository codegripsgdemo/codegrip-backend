package com.mb.codegrip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mb.codegrip.dto.Products;

@Repository
public interface ProductsRepository extends JpaRepository<Products, Integer>{

	Products findByProductName(String property);

}
