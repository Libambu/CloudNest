package com.yulong.easypan.entity.result;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadResultVO {
    private String fileId;
    private String status;

    public String getFileId() {
        return fileId;
    }

    @Override
    public String toString() {
        return "UploadResultVO{" +
                "fileId='" + fileId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
