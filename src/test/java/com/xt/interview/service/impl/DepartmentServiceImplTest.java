package com.xt.interview.service.impl;

import com.xt.interview.dto.DepartmentDTO;
import com.xt.interview.entity.Department;
import com.xt.interview.repository.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    @Test
    void createDefaultsToEnabledAndNormalizesNameAndCode() {
        DepartmentDTO dto = departmentDTO(" 研发部 ", " RD ", null);
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department department = invocation.getArgument(0);
            department.setId(1L);
            assertEquals(0, department.getDeleted());
            return department;
        });

        DepartmentDTO result = departmentService.createDepartment(dto);

        assertEquals(1L, result.getId());
        assertEquals("研发部", result.getName());
        assertEquals("RD", result.getCode());
        assertEquals(1, result.getStatus());
        verify(departmentRepository).existsByCode("RD");
    }

    @Test
    void createRejectsCodesReservedByAnyDepartmentIncludingDeletedOnes() {
        when(departmentRepository.existsByCode("RD")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.createDepartment(departmentDTO("研发部", "RD", 1)));

        assertEquals("部门编码已存在", exception.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    void updateKeepsExistingStatusWhenOmittedAndAllowsItsOwnCode() {
        Department department = department(1L, "研发部", "RD", 0);
        when(departmentRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);
        DepartmentDTO dto = departmentDTO("研发中心", "RD", null);
        dto.setDescription("新描述");

        DepartmentDTO result = departmentService.updateDepartment(1L, dto);

        assertEquals("研发中心", result.getName());
        assertEquals("新描述", result.getDescription());
        assertEquals(0, result.getStatus());
        verify(departmentRepository).existsByCodeAndIdNot("RD", 1L);
    }

    @Test
    void updateRejectsAnotherDepartmentsCodeBeforeChangingEntity() {
        Department department = department(1L, "研发部", "RD", 1);
        when(departmentRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(department));
        when(departmentRepository.existsByCodeAndIdNot("OPS", 1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> departmentService.updateDepartment(1L, departmentDTO("运维部", "OPS", 0)));

        assertEquals("RD", department.getCode());
        assertEquals("研发部", department.getName());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void updateDepartmentStatusPreservesDepartmentDetails(int status) {
        Department department = department(1L, "研发部", "RD", 1 - status);
        department.setDescription("负责研发工作");
        when(departmentRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);

        DepartmentDTO result = departmentService.updateDepartmentStatus(1L, status);

        assertEquals(status, result.getStatus());
        assertEquals(status, department.getStatus());
        assertEquals("研发部", department.getName());
        assertEquals("RD", department.getCode());
        assertEquals("负责研发工作", department.getDescription());
        assertEquals(0, department.getDeleted());
        assertEquals("研发部", result.getName());
        assertEquals("RD", result.getCode());
        assertEquals("负责研发工作", result.getDescription());
        verify(departmentRepository).findByIdAndDeleted(1L, 0);
        verify(departmentRepository).save(department);
        verifyNoMoreInteractions(departmentRepository);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L})
    void updateDepartmentStatusRejectsMissingOrDeletedDepartment(Long id) {
        when(departmentRepository.findByIdAndDeleted(id, 0)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> departmentService.updateDepartmentStatus(id, 0));

        assertEquals("部门不存在", exception.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 2})
    void updateDepartmentStatusRejectsInvalidStatusBeforeChangingDepartment(Integer status) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.updateDepartmentStatus(1L, status));

        assertEquals("状态必须为0或1", exception.getMessage());
        verifyNoInteractions(departmentRepository);
    }

    @Test
    void deleteMarksDeletedWithoutRemovingTheRow() {
        Department department = department(1L, "研发部", "RD", 1);
        when(departmentRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(department));

        departmentService.deleteDepartment(1L);

        assertEquals(1, department.getDeleted());
        verify(departmentRepository).save(department);
        verify(departmentRepository, never()).delete(any(Department.class));
        verify(departmentRepository, never()).deleteById(any());
    }

    @Test
    void readOnlyReturnsNonDeletedDepartments() {
        Department department = department(1L, "研发部", "RD", 1);
        when(departmentRepository.findByDeleted(0)).thenReturn(List.of(department));
        when(departmentRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(department));

        List<DepartmentDTO> list = departmentService.getAllDepartments();
        DepartmentDTO result = departmentService.getDepartmentById(1L);

        assertEquals(1, list.size());
        assertEquals("RD", list.getFirst().getCode());
        assertEquals(1L, result.getId());
        assertEquals("研发部", result.getName());
    }

    @Test
    void readRejectsMissingOrDeletedDepartment() {
        when(departmentRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> departmentService.getDepartmentById(1L));

        assertEquals("部门不存在", exception.getMessage());
    }

    private DepartmentDTO departmentDTO(String name, String code, Integer status) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setName(name);
        dto.setCode(code);
        dto.setStatus(status);
        return dto;
    }

    private Department department(Long id, String name, String code, Integer status) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setCode(code);
        department.setStatus(status);
        department.setDeleted(0);
        return department;
    }
}
