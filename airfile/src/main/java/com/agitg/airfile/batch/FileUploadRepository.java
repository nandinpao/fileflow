package com.agitg.airfile.batch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUploadEvent, Long> {

    // @Query(value = "SELECT f.id FROM FileUploadEvent f WHERE f.processed = false")
    Page<FileUploadEvent> findByProcessedFalse(Pageable pageable);

}
