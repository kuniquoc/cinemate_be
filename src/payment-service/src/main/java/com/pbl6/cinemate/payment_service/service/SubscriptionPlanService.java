package com.pbl6.cinemate.payment_service.service;

import com.pbl6.cinemate.payment_service.dto.response.SubscriptionPlanResponse;
import com.pbl6.cinemate.payment_service.entity.SubscriptionPlan;
import com.pbl6.cinemate.payment_service.exception.ResourceNotFoundException;
import com.pbl6.cinemate.payment_service.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {
    
    private final SubscriptionPlanRepository planRepository;
    private final ModelMapper modelMapper;
    
    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getAllPlans() {
        return planRepository.findByIsActiveTrue().stream()
                .map(plan -> modelMapper.map(plan, SubscriptionPlanResponse.class))
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlanById(UUID id) {
        SubscriptionPlan plan = planRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", "id", id));
        return modelMapper.map(plan, SubscriptionPlanResponse.class);
    }
    
    @Transactional(readOnly = true)
    public SubscriptionPlan getPlanEntityById(UUID id) {
        return planRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", "id", id));
    }
}
