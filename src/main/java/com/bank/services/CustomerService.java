package com.bank.services;

import com.bank.dtos.CustomerDTO;
import com.bank.dtos.CustomersDTO;
import com.bank.entities.Customer;
import com.bank.exceptions.CustomerNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CustomerService {
    CustomerDTO saveCustomer(CustomerDTO customerDTO) throws CustomerNotFoundException;
    CustomerDTO updateCustomer(CustomerDTO customerDTO);
    void deleteCustomer(Long customerId);
    List<CustomerDTO> listCustomers(int page);
    List<Customer> listCustomer();
    CustomerDTO getCustomer(Long customerId) throws CustomerNotFoundException;
    CustomerDTO getCustomerByName(String name);
    CustomersDTO getCustomerByName(String keyword, int page) throws CustomerNotFoundException;
}