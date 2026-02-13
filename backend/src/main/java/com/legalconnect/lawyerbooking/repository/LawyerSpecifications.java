package com.legalconnect.lawyerbooking.repository;

import com.legalconnect.lawyerbooking.dto.LawyerSearchCriteria;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class LawyerSpecifications {

    public static Specification<Lawyer> withCriteria(LawyerSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.getName())) {
                String namePattern = "%" + criteria.getName().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("fullName")), namePattern));
            }

            if (criteria.getSpecialization() != null) {
                predicates.add(cb.isMember(criteria.getSpecialization(), root.get("specializations")));
            }

            if (criteria.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), criteria.getMinRating()));
            }

            if (criteria.getMinExperience() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("yearsOfExperience"), criteria.getMinExperience()));
            }

            if (criteria.getMinCompletedCases() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("completedCasesCount"), criteria.getMinCompletedCases()));
            }

            if (StringUtils.hasText(criteria.getAvailability())) {
                // Determine logic for availability. For now, simple text match if field exists
                // Or maybe we just check if availabilityInfo interacts with it.
                // Assuming availabilityInfo contains the string (days/hours)
                String availPattern = "%" + criteria.getAvailability().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("availabilityInfo")), availPattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
