package com.xt.interview.service;

import com.xt.interview.dto.DepartmentDTO;

import java.util.List;

public interface DepartmentService {

    DepartmentDTO createDepartment(DepartmentDTO dto);

    DepartmentDTO updateDepartment(Long id, DepartmentDTO dto);

    DepartmentDTO updateDepartmentStatus(Long id, Integer status);

    void deleteDepartment(Long id);

    DepartmentDTO getDepartmentById(Long id);

    List<DepartmentDTO> getAllDepartments();
}
