package com.xt.interview.service.impl;

import com.xt.interview.dto.AccountDTO;
import com.xt.interview.dto.AccountPageDTO;
import com.xt.interview.entity.Account;
import com.xt.interview.entity.Department;
import com.xt.interview.repository.AccountRepository;
import com.xt.interview.repository.DepartmentRepository;
import com.xt.interview.util.MD5Util;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private AccountServiceImpl service;

    @Test
    void createAccountAssociatesDepartmentAndDoesNotReturnPassword() {
        Department department = department(10L, 0);
        department.setStatus(0);
        AccountDTO request = request(10L);
        when(departmentRepository.findByIdAndDeleted(10L, 0)).thenReturn(Optional.of(department));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountDTO result = service.createAccount(request);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getDepartment()).isSameAs(department);
        assertThat(accountCaptor.getValue().getPassword()).isEqualTo(MD5Util.encode("123456"));
        assertThat(result.getDepartmentId()).isEqualTo(10L);
        assertThat(result.getDepartmentName()).isEqualTo("研发部");
        assertThat(result.getPassword()).isNull();
    }

    @Test
    void createAccountAllowsNoDepartment() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountDTO result = service.createAccount(request(null));

        assertThat(result.getDepartmentId()).isNull();
        assertThat(result.getDepartmentName()).isNull();
        verifyNoInteractions(departmentRepository);
    }

    @Test
    void createAccountRejectsMissingOrDeletedDepartmentBeforeSaving() {
        when(departmentRepository.findByIdAndDeleted(10L, 0)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAccount(request(10L)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("部门不存在或已删除");

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void createAccountRejectsUsernameEvenWhenItsPreviousAccountWasDeleted() {
        when(accountRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.createAccount(request(null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("用户名已存在");

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void updateAccountPreservesExistingDeletedDepartmentAndPassword() {
        Account account = account(department(10L, 1));
        when(accountRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        AccountDTO result = service.updateAccount(1L, request(10L));

        assertThat(result.getDepartmentName()).isEqualTo("研发部");
        assertThat(account.getDepartment().getDeleted()).isEqualTo(1);
        assertThat(account.getPassword()).isEqualTo("existing-password-hash");
        verifyNoInteractions(departmentRepository);
    }

    @Test
    void updateAccountCanClearExistingDepartment() {
        Account account = account(department(10L, 1));
        when(accountRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        AccountDTO result = service.updateAccount(1L, request(null));

        assertThat(account.getDepartment()).isNull();
        assertThat(result.getDepartmentId()).isNull();
        assertThat(result.getDepartmentName()).isNull();
    }

    @Test
    void updateAccountAllowsUsernameCaseChangeWithoutConflictingWithItself() {
        Account account = account(null);
        AccountDTO request = request(null);
        request.setUsername("ALICE");
        when(accountRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        AccountDTO result = service.updateAccount(1L, request);

        assertThat(result.getUsername()).isEqualTo("ALICE");
        verify(accountRepository).existsByUsernameAndIdNot("ALICE", 1L);
        verify(accountRepository, never()).existsByUsername("ALICE");
    }

    @Test
    void updateAccountPreservesPermissionsAndStatusWhenOptionalFieldsAreOmitted() {
        Account account = account(null);
        account.setRoleLevel(3);
        account.setStatus(1);
        account.setIsSuper(1);
        AccountDTO request = new AccountDTO();
        request.setUsername("alice");
        request.setName("更新后的姓名");
        request.setDepartmentId(10L);
        when(accountRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(account));
        when(departmentRepository.findByIdAndDeleted(10L, 0)).thenReturn(Optional.of(department(10L, 0)));
        when(accountRepository.save(account)).thenReturn(account);

        AccountDTO result = service.updateAccount(1L, request);

        assertThat(result.getName()).isEqualTo("更新后的姓名");
        assertThat(result.getDepartmentId()).isEqualTo(10L);
        assertThat(result.getRoleLevel()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo(1);
        assertThat(result.getIsSuper()).isEqualTo(1);
    }

    @Test
    void updateAccountRejectsUsernameUsedByAnotherAccountBeforeChangingAccount() {
        Account account = account(null);
        AccountDTO request = request(null);
        request.setUsername("bob");
        when(accountRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(account));
        when(accountRepository.existsByUsernameAndIdNot("bob", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateAccount(1L, request))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("用户名已存在");

        assertThat(account.getUsername()).isEqualTo("alice");
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void updateAccountRejectsInvalidNewDepartmentWithoutChangingAccount() {
        Account account = account(department(10L, 0));
        AccountDTO request = request(20L);
        request.setName("修改后的姓名");
        when(accountRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(account));
        when(departmentRepository.findByIdAndDeleted(20L, 0)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAccount(1L, request))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("部门不存在或已删除");

        assertThat(account.getName()).isEqualTo("Alice");
        assertThat(account.getDepartment().getId()).isEqualTo(10L);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void updateAccountStatusPreservesAccountDetailsAndDepartment(int status) {
        Department department = department(10L, 1);
        Account account = account(department);
        account.setGender("男");
        account.setPhone("13800138000");
        account.setRoleLevel(3);
        account.setIsSuper(1);
        account.setStatus(1 - status);
        when(accountRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        AccountDTO result = service.updateAccountStatus(1L, status);

        assertThat(result.getStatus()).isEqualTo(status);
        assertThat(account.getStatus()).isEqualTo(status);
        assertThat(account.getUsername()).isEqualTo("alice");
        assertThat(account.getName()).isEqualTo("Alice");
        assertThat(account.getGender()).isEqualTo("男");
        assertThat(account.getPhone()).isEqualTo("13800138000");
        assertThat(account.getPassword()).isEqualTo("existing-password-hash");
        assertThat(account.getRoleLevel()).isEqualTo(3);
        assertThat(account.getIsSuper()).isEqualTo(1);
        assertThat(account.getDepartment()).isSameAs(department);
        assertThat(account.getDeleted()).isEqualTo(0);
        assertThat(result.getDepartmentId()).isEqualTo(10L);
        assertThat(result.getDepartmentName()).isEqualTo("研发部");
        assertThat(result.getPassword()).isNull();
        verify(accountRepository).findByIdAndDeleted(1L, 0);
        verify(accountRepository).save(account);
        verifyNoMoreInteractions(accountRepository);
        verifyNoInteractions(departmentRepository);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L})
    void updateAccountStatusRejectsSoftDeletedOrMissingAccount(Long id) {
        when(accountRepository.findByIdAndDeleted(id, 0)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAccountStatus(id, 0))
                .isInstanceOf(EntityNotFoundException.class).hasMessage("账号不存在");

        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(departmentRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 2})
    void updateAccountStatusRejectsInvalidStatusBeforeChangingAccount(Integer status) {
        assertThatThrownBy(() -> service.updateAccountStatus(1L, status))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("状态必须为0或1");

        verifyNoInteractions(accountRepository, departmentRepository);
    }

    @Test
    void getAccountRejectsSoftDeletedOrMissingAccount() {
        when(accountRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccountById(1L))
                .isInstanceOf(EntityNotFoundException.class).hasMessage("账号不存在");
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageIncludesDepartmentAndDelegatesPaginationAndAllFiltersToRepository() {
        Account account = account(department(10L, 1));
        PageRequest pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "id"));
        when(accountRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(account), pageable, 5));

        AccountPageDTO result = service.getAccountPage(" alice%_! ", 1, 2, 1, 2);

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getDepartmentName()).isEqualTo("研发部");
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getCurrentPage()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(2);

        ArgumentCaptor<Specification<Account>> specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(accountRepository).findAll(specificationCaptor.capture(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(pageable);

        Root<Account> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path<String> username = mock(Path.class);
        Path<Object> deleted = mock(Path.class);
        Path<Object> status = mock(Path.class);
        Path<Object> roleLevel = mock(Path.class);
        when(root.<String>get("username")).thenReturn(username);
        when(root.get("deleted")).thenReturn(deleted);
        when(root.get("status")).thenReturn(status);
        when(root.get("roleLevel")).thenReturn(roleLevel);
        specificationCaptor.getValue().toPredicate(root, query, builder);

        verify(builder).equal(deleted, 0);
        verify(builder).like(username, "%alice!%!_!!%", '!');
        verify(builder).equal(status, 1);
        verify(builder).equal(roleLevel, 2);
        verify(builder).and(any(Predicate[].class));
    }

    @ParameterizedTest
    @CsvSource({"-1,10,1,1", "0,0,1,1", "0,101,1,1", "0,10,2,1", "0,10,1,0", "0,10,1,4"})
    void pageRejectsInvalidParametersBeforeQuerying(int page, int size, int status, int roleLevel) {
        assertThatThrownBy(() -> service.getAccountPage(null, status, roleLevel, page, size))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(accountRepository);
    }

    private AccountDTO request(Long departmentId) {
        AccountDTO dto = new AccountDTO();
        dto.setUsername("alice");
        dto.setName("Alice");
        dto.setRoleLevel(1);
        dto.setStatus(1);
        dto.setIsSuper(0);
        dto.setDepartmentId(departmentId);
        return dto;
    }

    private Account account(Department department) {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("alice");
        account.setName("Alice");
        account.setPassword("existing-password-hash");
        account.setDepartment(department);
        account.setDeleted(0);
        return account;
    }

    private Department department(Long id, int deleted) {
        Department department = new Department();
        department.setId(id);
        department.setName("研发部");
        department.setDeleted(deleted);
        return department;
    }
}
