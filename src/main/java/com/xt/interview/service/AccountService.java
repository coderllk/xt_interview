package com.xt.interview.service;

import com.xt.interview.dto.AccountDTO;
import com.xt.interview.dto.AccountPageDTO;

import java.util.List;

public interface AccountService {

    AccountDTO createAccount(AccountDTO dto);

    AccountDTO updateAccount(Long id, AccountDTO dto);

    AccountDTO updateAccountStatus(Long id, Integer status);

    void deleteAccount(Long id);

    AccountDTO getAccountById(Long id);

    List<AccountDTO> getAllAccounts();

    AccountPageDTO getAccountPage(String username, Integer status, Integer roleLevel, int page, int size);

    AccountDTO login(String username, String password);
}
