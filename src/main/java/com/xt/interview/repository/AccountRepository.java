package com.xt.interview.repository;

import com.xt.interview.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {
    @EntityGraph(attributePaths = "department")
    List<Account> findByDeleted(Integer deleted);
    @EntityGraph(attributePaths = "department")
    Optional<Account> findByIdAndDeleted(Long id, Integer deleted);
    Optional<Account> findByUsernameAndDeleted(String username, Integer deleted);
    @EntityGraph(attributePaths = "department")
    Optional<Account> findByUsernameAndPasswordAndDeleted(String username, String password, Integer deleted);
    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByUsernameAndDeleted(String username, Integer deleted);

    @Override
    @EntityGraph(attributePaths = "department")
    Page<Account> findAll(Specification<Account> specification, Pageable pageable);
}
