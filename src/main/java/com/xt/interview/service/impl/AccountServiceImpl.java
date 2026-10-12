package com.xt.interview.service.impl;

import com.xt.interview.dto.AccountDTO;
import com.xt.interview.dto.AccountPageDTO;
import com.xt.interview.entity.Account;
import com.xt.interview.entity.Department;
import com.xt.interview.repository.AccountRepository;
import com.xt.interview.repository.DepartmentRepository;
import com.xt.interview.service.AccountService;
import com.xt.interview.util.MD5Util;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    private static final String DEFAULT_PASSWORD = "123456";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public AccountDTO createAccount(AccountDTO dto) {
        if (accountRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        Department department = resolveDepartment(dto.getDepartmentId(), null);
        Account account = new Account();
        account.setUsername(dto.getUsername());
        account.setName(dto.getName());
        account.setGender(dto.getGender());
        account.setPhone(dto.getPhone());
        String password = (dto.getPassword() == null || dto.getPassword().isEmpty())
                ? DEFAULT_PASSWORD : dto.getPassword();
        account.setPassword(MD5Util.encode(password));
        account.setRoleLevel(dto.getRoleLevel());
        account.setStatus(dto.getStatus());
        account.setIsSuper(dto.getIsSuper());
        account.setDepartment(department);
        account.setDeleted(NOT_DELETED);
        account = accountRepository.save(account);
        return convertToDTO(account);
    }

    @Override
    public AccountDTO updateAccount(Long id, AccountDTO dto) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new EntityNotFoundException("账号不存在"));

        if (accountRepository.existsByUsernameAndIdNot(dto.getUsername(), id)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        Department department = resolveDepartment(dto.getDepartmentId(), account.getDepartment());

        account.setUsername(dto.getUsername());
        account.setName(dto.getName());
        account.setGender(dto.getGender());
        account.setPhone(dto.getPhone());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            account.setPassword(MD5Util.encode(dto.getPassword()));
        }
        if (dto.getRoleLevel() != null) {
            account.setRoleLevel(dto.getRoleLevel());
        }
        if (dto.getStatus() != null) {
            account.setStatus(dto.getStatus());
        }
        if (dto.getIsSuper() != null) {
            account.setIsSuper(dto.getIsSuper());
        }
        account.setDepartment(department);
        account = accountRepository.save(account);
        return convertToDTO(account);
    }

    @Override
    public AccountDTO updateAccountStatus(Long id, Integer status) {
        if (status == null || status < 0 || status > 1) {
            throw new IllegalArgumentException("状态必须为0或1");
        }
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new EntityNotFoundException("账号不存在"));
        account.setStatus(status);
        account = accountRepository.save(account);
        return convertToDTO(account);
    }

    @Override
    public void deleteAccount(Long id) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new EntityNotFoundException("账号不存在"));
        account.setDeleted(DELETED);
        accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountById(Long id) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new EntityNotFoundException("账号不存在"));
        return convertToDTO(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() {
        return accountRepository.findByDeleted(NOT_DELETED).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountPageDTO getAccountPage(String username, Integer status, Integer roleLevel, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("页码不能小于0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("每页条数必须为1到100");
        }
        if (status != null && (status < 0 || status > 1)) {
            throw new IllegalArgumentException("状态必须为0或1");
        }
        if (roleLevel != null && (roleLevel < 1 || roleLevel > 3)) {
            throw new IllegalArgumentException("权限级别必须为1到3");
        }

        Specification<Account> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("deleted"), NOT_DELETED));
            if (username != null && !username.trim().isEmpty()) {
                String escapedUsername = username.trim().replace("!", "!!")
                        .replace("%", "!%").replace("_", "!_");
                predicates.add(builder.like(root.get("username"), "%" + escapedUsername + "%", '!'));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (roleLevel != null) {
                predicates.add(builder.equal(root.get("roleLevel"), roleLevel));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        Page<Account> accountPage = accountRepository.findAll(specification,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        AccountPageDTO dto = new AccountPageDTO();
        dto.setList(accountPage.getContent().stream().map(this::convertToDTO).collect(Collectors.toList()));
        dto.setTotalElements(accountPage.getTotalElements());
        dto.setTotalPages(accountPage.getTotalPages());
        dto.setCurrentPage(accountPage.getNumber());
        dto.setPageSize(accountPage.getSize());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO login(String username, String password) {
        Account account = accountRepository.findByUsernameAndPasswordAndDeleted(
                username, MD5Util.encode(password), NOT_DELETED)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!Integer.valueOf(1).equals(account.getStatus())) {
            throw new IllegalArgumentException("账号已禁用");
        }
        return convertToDTO(account);
    }

    private Department resolveDepartment(Long departmentId, Department currentDepartment) {
        if (departmentId == null) {
            return null;
        }
        if (currentDepartment != null && Objects.equals(departmentId, currentDepartment.getId())) {
            return currentDepartment;
        }
        return departmentRepository.findByIdAndDeleted(departmentId, NOT_DELETED)
                .orElseThrow(() -> new IllegalArgumentException("部门不存在或已删除"));
    }

    private AccountDTO convertToDTO(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setUsername(account.getUsername());
        dto.setName(account.getName());
        dto.setGender(account.getGender());
        dto.setPhone(account.getPhone());
        dto.setRoleLevel(account.getRoleLevel());
        dto.setStatus(account.getStatus());
        dto.setIsSuper(account.getIsSuper());
        if (account.getDepartment() != null) {
            dto.setDepartmentId(account.getDepartment().getId());
            dto.setDepartmentName(account.getDepartment().getName());
        }
        dto.setCreateTime(account.getCreateTime());
        dto.setUpdateTime(account.getUpdateTime());
        return dto;
    }
}
