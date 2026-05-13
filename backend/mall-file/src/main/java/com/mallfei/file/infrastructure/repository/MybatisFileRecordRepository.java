package com.mallfei.file.infrastructure.repository;

import com.mallfei.file.domain.model.FileRecord;
import com.mallfei.file.domain.repository.FileRecordRepository;
import com.mallfei.file.infrastructure.persistence.dataobject.FileRecordDO;
import com.mallfei.file.infrastructure.persistence.mapper.FileRecordMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisFileRecordRepository implements FileRecordRepository {

    private final FileRecordMapper fileRecordMapper;

    public MybatisFileRecordRepository(FileRecordMapper fileRecordMapper) {
        this.fileRecordMapper = fileRecordMapper;
    }

    @Override
    public FileRecord save(FileRecord fileRecord) {
        FileRecordDO fileRecordDO = toDataObject(fileRecord);
        fileRecordMapper.insert(fileRecordDO);
        return toDomain(fileRecordDO);
    }

    private FileRecordDO toDataObject(FileRecord fileRecord) {
        FileRecordDO fileRecordDO = new FileRecordDO();
        fileRecordDO.setId(fileRecord.id());
        fileRecordDO.setBizType(fileRecord.bizType());
        fileRecordDO.setStorageType(fileRecord.storageType());
        fileRecordDO.setFileName(fileRecord.fileName());
        fileRecordDO.setOriginalFileName(fileRecord.originalFileName());
        fileRecordDO.setRelativePath(fileRecord.relativePath());
        fileRecordDO.setAccessUrl(fileRecord.accessUrl());
        fileRecordDO.setContentType(fileRecord.contentType());
        fileRecordDO.setFileSize(fileRecord.fileSize());
        fileRecordDO.setUploaderId(fileRecord.uploaderId());
        fileRecordDO.setUploaderType(fileRecord.uploaderType());
        fileRecordDO.setUploaderAccount(fileRecord.uploaderAccount());
        fileRecordDO.setUploaderNickname(fileRecord.uploaderNickname());
        fileRecordDO.setStatus(fileRecord.status());
        return fileRecordDO;
    }

    private FileRecord toDomain(FileRecordDO fileRecordDO) {
        return new FileRecord(
                fileRecordDO.getId(),
                fileRecordDO.getBizType(),
                fileRecordDO.getStorageType(),
                fileRecordDO.getFileName(),
                fileRecordDO.getOriginalFileName(),
                fileRecordDO.getRelativePath(),
                fileRecordDO.getAccessUrl(),
                fileRecordDO.getContentType(),
                fileRecordDO.getFileSize() == null ? 0L : fileRecordDO.getFileSize(),
                fileRecordDO.getUploaderId(),
                fileRecordDO.getUploaderType(),
                fileRecordDO.getUploaderAccount(),
                fileRecordDO.getUploaderNickname(),
                fileRecordDO.getStatus()
        );
    }
}
