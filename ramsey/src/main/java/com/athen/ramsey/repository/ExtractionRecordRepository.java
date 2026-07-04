package com.athen.ramsey.repository;

import com.athen.ramsey.document.ExtractionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExtractionRecordRepository extends MongoRepository<ExtractionRecord, String> {

    Page<ExtractionRecord> findByFileNameContainingIgnoreCase(String fileName, Pageable pageable);
}
