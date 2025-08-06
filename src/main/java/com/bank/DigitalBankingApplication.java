package com.bank;

import com.bank.dtos.CustomerDTO;
import com.bank.entities.AccountOperation;
import com.bank.entities.CurrentAccount;
import com.bank.entities.SavingAccount;
import com.bank.enums.AccountStatus;
import com.bank.enums.OperationType;
import com.bank.exceptions.CustomerNotFoundException;
import com.bank.mappers.BankAccountMapperImplementation;
import com.bank.repositories.AccountOperationRepository;
import com.bank.repositories.BankAccountRepository;
import com.bank.repositories.CustomerRepository;
import com.bank.services.BankAccountService;
import com.bank.services.CustomerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class DigitalBankingApplication {

	
    private final CustomerService customerService;
    
    @Autowired
    private BankAccountMapperImplementation dtoMapper;


    DigitalBankingApplication(CustomerService customerService) {
        this.customerService = customerService;
    }

    public static void main(String[] args) {
        SpringApplication.run(DigitalBankingApplication.class, args);
    }
    

    @Bean
    CommandLineRunner commandLineRunner(CustomerService customerService) {
        return args -> {
            Stream.of("Amit", "Shubham", "Mandar").forEach(name -> {
                CustomerDTO customer = new CustomerDTO();
                customer.setName(name);
                customer.setEmail(name + "@gmail.com");
                try {
                    customerService.saveCustomer(customer);
                } catch (CustomerNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        };
    }

    @Bean
    CommandLineRunner start(CustomerRepository customerRepository,
                            BankAccountRepository bankAccountRepository,
                            AccountOperationRepository accountOperationRepository) {
        return args -> {
            customerRepository.findAll().forEach(cust -> {
                CurrentAccount currentAccount = new CurrentAccount();
                currentAccount.setId(UUID.randomUUID().toString());
                currentAccount.setBalance(Math.random() * 90000);
                currentAccount.setCreatedAt(new Date());
                currentAccount.setStatus(AccountStatus.CREATED);
                currentAccount.setCustomer(cust);
                currentAccount.setOverDraft(9000);
                bankAccountRepository.save(currentAccount);

                SavingAccount savingAccount = new SavingAccount();
                savingAccount.setId(UUID.randomUUID().toString());
                savingAccount.setBalance(Math.random() * 90000);
                savingAccount.setCreatedAt(new Date());
                savingAccount.setStatus(AccountStatus.CREATED);
                savingAccount.setCustomer(cust);
                savingAccount.setInterestRate(5.5);
                bankAccountRepository.save(savingAccount);
            });

            bankAccountRepository.findAll().forEach(acc -> {
                for (int i = 0; i < 10; i++) {
                    AccountOperation accountOperation = new AccountOperation();
                    accountOperation.setOperationDate(new Date());
                    accountOperation.setAmount(Math.random() * 12000);
                    accountOperation.setType(Math.random() > 0.5 ? OperationType.DEBIT : OperationType.CREDIT);
                    accountOperation.setBankAccount(acc);
                    accountOperationRepository.save(accountOperation);
                }
            });
        };
    }
}
