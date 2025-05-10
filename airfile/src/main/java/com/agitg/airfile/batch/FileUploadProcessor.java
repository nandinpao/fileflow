package com.agitg.airfile.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.Tika;
import org.springframework.batch.item.ItemProcessor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUploadProcessor implements ItemProcessor<FileUploadEvent, FileUploadEvent> {

    private final Tika tika = new Tika();

    @Override
    public FileUploadEvent process(FileUploadEvent task) {

        File file = new File(task.getFilePath());
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                String type;

                type = tika.detect(is);

                log.debug("File Id {}, content-type: {}", task.getId(), type);

            } catch (FileNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }

        return task;
    }
}