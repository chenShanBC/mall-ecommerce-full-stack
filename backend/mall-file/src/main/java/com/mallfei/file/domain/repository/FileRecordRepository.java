package com.mallfei.file.domain.repository;

import com.mallfei.file.domain.model.FileRecord;

public interface FileRecordRepository {

    FileRecord save(FileRecord fileRecord);
}
