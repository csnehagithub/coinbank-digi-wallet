package com.bank.services;

import com.bank.dtos.*;
import com.bank.entities.*;
import com.bank.enums.AccountStatus;
import com.bank.enums.OperationType;
import com.bank.exceptions.BalanceNotSufficientException;
import com.bank.exceptions.BankAccountNotFound;
import com.bank.exceptions.CustomerNotFoundException;
import com.bank.mappers.BankAccountMapperImplementation;
import com.bank.repositories.*;
//import com.bank.security.services.AccountService;
import com.bank.services.BankAccountService;
import com.bank.utils.AccountUtils;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class BankAccountServiceImplementation implements BankAccountService {

	public static ArrayList<DebitDTO> debList = new ArrayList<>();
	public static HashMap<String, FdDto> fdMap=new HashMap<String, FdDto>();
	@Autowired
	@Order(1)
	BankAccountRepository bankAccountRepository;
	@Autowired
	@Order(1)
	CustomerRepository customerRepository;
	@Autowired
	@Order(1)
	AccountOperationRepository accountOperationRepository;

	@Autowired
	BankAccountMapperImplementation dtoMapper;
//	@Autowired
//	AccountService accountService;

	@Autowired
	private EmailService emailService;

	@Override
	public SavingBankAccountDTO saveSavingBankAccount(double initialBalance, double interestRate, Long customerId)
			throws CustomerNotFoundException {

		Customer customer = customerRepository.findById(customerId).orElse(null);

		if (customer == null)
			throw new CustomerNotFoundException("Customer Not Found");
		SavingAccount savingAccount = new SavingAccount();
		savingAccount.setCreatedAt(new Date());
		// savingAccount.setId(UUID.randomUUID().toString());
		savingAccount.setId(AccountUtils.generateAccountNumber());
		savingAccount.setInterestRate(interestRate);
		savingAccount.setCustomer(customer);
		savingAccount = bankAccountRepository.save(savingAccount);

		return dtoMapper.fromSavingBankAccount(savingAccount);
	}

	@Override
	public CurrentBankAccountDTO saveCurrentBankAccount(double initialBalance, double overdraft, Long customerId)
			throws CustomerNotFoundException {

		Customer customer = customerRepository.findById(customerId).orElse(null);

		if (customer == null)
			throw new CustomerNotFoundException("Customer Not Found");
		CurrentAccount currentAccount = new CurrentAccount();
		currentAccount.setCreatedAt(new Date());
		currentAccount.setId(AccountUtils.generateAccountNumber());
		currentAccount.setOverDraft(overdraft);
		currentAccount.setCustomer(customer);
		currentAccount = bankAccountRepository.save(currentAccount);
		return dtoMapper.fromCurrentBankAccount(currentAccount);

	}

	@Override
	public BankAccountDTO getBankAccount(String accountId) throws BankAccountNotFound {
		BankAccount bankAccount = bankAccountRepository.findById(accountId)
				.orElseThrow(() -> new BankAccountNotFound("Bank account Not Found"));

		if (bankAccount instanceof SavingAccount) {
			SavingAccount savingAccount = (SavingAccount) bankAccount;
			SavingBankAccountDTO savingBankAccountDTO = dtoMapper.fromSavingBankAccount(savingAccount);
			return savingBankAccountDTO;
		} else {
			CurrentAccount currentAccount = (CurrentAccount) bankAccount;
			CurrentBankAccountDTO currentBankAccountDTO = dtoMapper.fromCurrentBankAccount(currentAccount);
			return currentBankAccountDTO;
		}
	}

	@Override
	public synchronized void debit(String accountId, double amount, String description,String upiId)
			throws BalanceNotSufficientException, BankAccountNotFound {
		BankAccount bankAccount = bankAccountRepository.findById(accountId)
				.orElseThrow(() -> new BankAccountNotFound("Account not found"));
		if (bankAccount.getBalance() < amount) {
			throw new BalanceNotSufficientException("Balance not sufficient");
		}
		
		AccountOperation accountOperation = new AccountOperation();
		accountOperation.setType(OperationType.DEBIT);
		bankAccount.setBalance(bankAccount.getBalance() - amount);
		accountOperation.setBankAccount(bankAccount);
		accountOperation.setOperationDate(new Date());
		accountOperation.setDescription(description);

		accountOperation.setAmount(amount);
		bankAccountRepository.save(bankAccount);
		accountOperationRepository.save(accountOperation);
		
		debList.add(new DebitDTO(accountId,amount,description,upiId));
		if(description.contains("FD"))
		{
			FdDto fdDto=new FdDto();
			fdDto.setAccountId(accountId);
			fdDto.setAmount(amount);
			fdDto.setDescription(description);
			
			fdMap.put(accountId, fdDto);
		}
		ExecutorService executorService = Executors.newSingleThreadExecutor();

		executorService.submit(() -> {
			// Send the email notification
			EmailDetails emailDetails = EmailDetails.builder().recipient(bankAccount.getCustomer().getEmail())
					.subject("AMOUNT DEBITED").messageBody(amount + ".Rs debited from your account NO:"
							+ bankAccount.getId() + ".\n Current Balance:" + bankAccount.getBalance())
					.build();

			emailService.sendEmailAlert(emailDetails);
		});

		executorService.shutdown();

	}

    @Override
    public void credit(String accountId, double amount, String description) throws BankAccountNotFound {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFound("Account not found"));

        AccountOperation operation = new AccountOperation();
        operation.setOperationDate(new Date());
        operation.setAmount(amount);
        operation.setType(OperationType.CREDIT);
        operation.setDescription(description);
        operation.setBankAccount(account);
        accountOperationRepository.save(operation);

        account.setBalance(account.getBalance() + amount);
        bankAccountRepository.save(account);
    }

    @Override
    public void transfer(String accountIdSource, String accountIdDestination, double amount, String description) throws BankAccountNotFound, BalanceNotSufficientException {
        debit(accountIdSource, amount, "Transfer to " + accountIdDestination, null);
        credit(accountIdDestination, amount, "Transfer from " + accountIdSource);
    }

    public List<BankAccountDTO> bankAccountListOfCustomer(Long id) {
		Customer customer = new Customer();
		customer.setId(id);
		List<BankAccount> bankAccounts = bankAccountRepository.findByCustomer(customer);
		List<BankAccountDTO> bankAccountDTOS = bankAccounts.stream().map(bankAccount -> {
			if (bankAccount instanceof SavingAccount) {
				SavingAccount savingAccount = (SavingAccount) bankAccount;
				return dtoMapper.fromSavingBankAccount(savingAccount);
			} else {
				CurrentAccount currentAccount = (CurrentAccount) bankAccount;
				return dtoMapper.fromCurrentBankAccount(currentAccount);
			}
		}).collect(Collectors.toList());
		return bankAccountDTOS;
	}


	@Override
	public BankAccountsDTO getBankAccountList(int page) {

		Page<BankAccount> bankAccounts = bankAccountRepository.findAll(PageRequest.of(page, 5));
		List<BankAccountDTO> bankAccountDTOList = bankAccounts.stream().map(bankAccount -> {
			if (bankAccount instanceof SavingAccount) {
				SavingAccount savingAccount = (SavingAccount) bankAccount;
				return dtoMapper.fromSavingBankAccount(savingAccount);
			} else {
				CurrentAccount currentAccount = (CurrentAccount) bankAccount;
				return dtoMapper.fromCurrentBankAccount(currentAccount);
			}
		}

		).collect(Collectors.toList());
		BankAccountsDTO bankAccountsDTO = new BankAccountsDTO();
		bankAccountsDTO.setBankAccountDTOS(bankAccountDTOList);
		bankAccountsDTO.setTotalPage(bankAccounts.getTotalPages());
		return bankAccountsDTO;
	}

    @Override
	public List<AccountOperationDTO> accountOperationHistory(String accountId) {
		List<AccountOperation> accountOperations = accountOperationRepository.findByBankAccountId(accountId);
		List<AccountOperationDTO> accountOperationDTOS = accountOperations.stream().map(accountOperation -> {
			return dtoMapper.fromAccountOperation(accountOperation);
		}).collect(Collectors.toList());
		return accountOperationDTOS;
	}

    @Override
	public BankAccountDTO updateBankAccount(BankAccountDTO bankAccountDTO) {
		BankAccount bankAccount;
		if (bankAccountDTO.getType().equals("saving account")) {
			SavingBankAccountDTO saving = new SavingBankAccountDTO();

			BeanUtils.copyProperties(bankAccountDTO, saving);
			bankAccount = dtoMapper.fromSavingBankAccountDTO(saving);
			bankAccount = bankAccountRepository.save(bankAccount);
			return dtoMapper.fromSavingBankAccount((SavingAccount) bankAccount);

		} else {
			CurrentBankAccountDTO current = new CurrentBankAccountDTO();

			BeanUtils.copyProperties(bankAccountDTO, current);
			bankAccount = dtoMapper.fromCurrentBankAccountDTO(current);
			bankAccount = bankAccountRepository.save(bankAccount);
			return dtoMapper.fromCurrentBankAccount((CurrentAccount) bankAccount);
		}

	}

    @Override
	public AccountHistoryDTO getAccoutHistory(String accountId, int page, int size) throws BankAccountNotFound {
		BankAccount bankAccount = bankAccountRepository.findById(accountId).orElse(null);
		if (bankAccount == null)
			throw new BankAccountNotFound("bank not fount ");
		Page<AccountOperation> accountOperationPage = accountOperationRepository
				.findByBankAccountIdOrderByOperationDateDesc(accountId, PageRequest.of(page, size));

		AccountHistoryDTO accountHistoryDTO = new AccountHistoryDTO();
		// List<AccountOperationDTO>
		// accountOperationDTOList=accountOperationPage.getContent().stream().map(op->dtoMapper.fromAccountOperation(op)).collect(Collectors.toList());
		// System.out.println(accountOperationDTOList);
		List<AccountOperationDTO> accountOperationDTOList = accountOperationPage.getContent().stream().map(op -> {
			LocalDateTime localDateTime = op.getOperationDate().toInstant().atZone(ZoneId.systemDefault())
					.toLocalDateTime();

			AccountOperationDTO accountOperationDTO = dtoMapper.fromAccountOperation(op);
			accountOperationDTO.setOperationDate(localDateTime);
			return accountOperationDTO;
		}).collect(Collectors.toList());
		accountHistoryDTO.setAccountOperationDTOList(accountOperationDTOList);
		accountHistoryDTO.setAccountId(bankAccount.getId());
		accountHistoryDTO.setBalance(bankAccount.getBalance());
		accountHistoryDTO.setPageSize(size);
		accountHistoryDTO.setCurrentPage(page);
		accountHistoryDTO.setTotalPages(accountOperationPage.getTotalPages());
		accountHistoryDTO.setCustomerId(bankAccount.getCustomer().getId());
		return accountHistoryDTO;

	}
}
