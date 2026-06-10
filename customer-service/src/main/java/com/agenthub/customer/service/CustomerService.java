package com.agenthub.customer.service;

import com.agenthub.customer.entity.Customer;
import com.agenthub.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    public Customer create(Customer customer) {
        return customerRepository.save(customer);
    }

    public Customer update(Long id, Customer updatedCustomer) {
        Customer existingCustomer = findById(id);

        existingCustomer
                .setOrganizationName(updatedCustomer.getOrganizationName())
                .setContactName(updatedCustomer.getContactName())
                .setEmail(updatedCustomer.getEmail())
                .setSubscriptionPlan(updatedCustomer.getSubscriptionPlan())
                .setStatus(updatedCustomer.getStatus());

        return customerRepository.save(existingCustomer);
    }

    public void delete(Long id) {
        Customer existingCustomer = findById(id);
        customerRepository.delete(existingCustomer);
    }
}