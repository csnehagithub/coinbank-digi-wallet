package com.bank.services;

import com.bank.dtos.CustomerDTO;
import com.bank.dtos.CustomersDTO;
import com.bank.entities.Customer;
import com.bank.exceptions.CustomerNotFoundException;
import com.bank.mappers.BankAccountMapperImplementation;
import com.bank.repositories.CustomerRepository;
import com.bank.services.CustomerService;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

	@Autowired
//	@Order(1)
	private CustomerRepository customerRepository;
	@Autowired
    private BankAccountMapperImplementation dtoMapper;

    @Override
    public CustomerDTO saveCustomer(CustomerDTO customerDTO) throws CustomerNotFoundException {
        Customer customer = dtoMapper.fromCustomerDTO(customerDTO);
        Customer savedCustomer = customerRepository.save(customer);
        return dtoMapper.fromCustomer(savedCustomer);
    }

    @Override
    public CustomerDTO updateCustomer(CustomerDTO customerDTO) {
        Customer customer = dtoMapper.fromCustomerDTO(customerDTO);
        Customer updated = customerRepository.save(customer);
        return dtoMapper.fromCustomer(updated);
    }

    @Override
    public void deleteCustomer(Long customerId) {
        customerRepository.deleteById(customerId);
    }

    @Override
    public List<CustomerDTO> listCustomers(int page) {
        return customerRepository.findAll().stream()
                .map(dtoMapper::fromCustomer)
                .collect(Collectors.toList());
    }

    @Override
    public List<Customer> listCustomer() {
        return customerRepository.findAll();
    }

    @Override
	public CustomerDTO getCustomer(Long customerId) throws CustomerNotFoundException {
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
		return dtoMapper.fromCustomer(customer);
	}

    @Override
    public CustomerDTO getCustomerByName(String name) {
        Customer customer = customerRepository.getCustomerByName(name);
        return dtoMapper.fromCustomer(customer);
    }

	@Override
	public CustomersDTO getCustomerByName(String keyword, int page) throws CustomerNotFoundException {
		Page<Customer> customers;

		customers = customerRepository.searchByName(keyword, PageRequest.of(page, 5));
		List<CustomerDTO> customerDTOS = customers.getContent().stream().map(c -> dtoMapper.fromCustomer(c))
				.collect(Collectors.toList());
		if (customers == null)
			throw new CustomerNotFoundException("customer not fount");

		CustomersDTO customersDTO = new CustomersDTO();
		customersDTO.setCustomerDTO(customerDTOS);
		customersDTO.setTotalpage(customers.getTotalPages());
		return customersDTO;
    }

							    
}

