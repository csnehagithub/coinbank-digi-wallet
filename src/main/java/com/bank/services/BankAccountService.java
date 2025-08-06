package com.bank.services;

import com.bank.dtos.*;
import com.bank.exceptions.BalanceNotSufficientException;
import com.bank.exceptions.BankAccountNotFound;
import com.bank.exceptions.CustomerNotFoundException;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public interface BankAccountService {
    SavingBankAccountDTO saveSavingBankAccount(double initialBalance, double interestRate, Long customerId) throws CustomerNotFoundException;
    CurrentBankAccountDTO saveCurrentBankAccount(double initialBalance, double overdraft, Long customerId) throws CustomerNotFoundException;
    BankAccountDTO getBankAccount(String accountId) throws BankAccountNotFound;
    void debit(String accountId, double amount, String description, String upiId) throws BalanceNotSufficientException, BankAccountNotFound;
    void credit(String accountId, double amount, String description) throws BankAccountNotFound;
    void transfer(String accountIdSource, String accountIdDestination, double amount, String description) throws BankAccountNotFound, BalanceNotSufficientException;
    List<BankAccountDTO> bankAccountListOfCustomer(Long customerId);
    BankAccountsDTO getBankAccountList(int page);
    List<AccountOperationDTO> accountOperationHistory(String accountId);
    AccountHistoryDTO getAccoutHistory(String accountId, int page, int size) throws BankAccountNotFound;
    BankAccountDTO updateBankAccount(BankAccountDTO bankAccountDTO);
}
