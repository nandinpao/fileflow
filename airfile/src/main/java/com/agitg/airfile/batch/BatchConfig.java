
package com.agitg.airfile.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;

import org.apache.tika.Tika;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    private final Tika tika = new Tika();

    @Bean
    public Job fileUploadJob(JobRepository jobRepository, Step fileUploadStep) {
        return new JobBuilder("fileUploadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(fileUploadStep)
                .build();
    }

    @Bean
    public Step fileUploadStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            RepositoryItemReader<FileUploadEvent> reader,
            ItemProcessor<FileUploadEvent, FileUploadEvent> processor,
            ItemWriter<FileUploadEvent> writer) {

        return new StepBuilder("fileUploadStep", jobRepository)
                .<FileUploadEvent, FileUploadEvent>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public RepositoryItemReader<FileUploadEvent> reader(FileUploadRepository repository) {

        RepositoryItemReader<FileUploadEvent> reader = new RepositoryItemReader<>();
        reader.setRepository(repository);
        reader.setMethodName("findByProcessedFalse");
        reader.setPageSize(10);
        reader.setSort(Collections.singletonMap("id", Sort.Direction.ASC));

        return reader;
    }

    @Bean
    public ItemProcessor<FileUploadEvent, FileUploadEvent> processor() {
        return task -> {
            File file = new File(task.getFilePath());
            if (file.exists()) {
                try (InputStream is = new FileInputStream(file)) {
                    String type = tika.detect(is);
                    log.debug("File Id: {}, Name: {}, content-type: {}", task.getId(), task.getFileName(), type);
                }
            }
            return task;
        };
    }

    @Bean
    public ItemWriter<FileUploadEvent> writer(FileUploadRepository repo) {
        return items -> {
            for (FileUploadEvent task : items) {
                task.setProcessed(true);
                repo.save(task);
                repo.delete(task); // ❗刪除成功處理過的任務
            }
        };
    }

}