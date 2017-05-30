/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.acme.bestpublishing.error;

/*
 * Processing Error codes for the different Best Publishing components that support the workflow.
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
public enum ProcessingErrorCode {
    INGESTION_DIR_NOT_FOUND(1, "Directory to check does not exist."),
    INGESTION_DIR_IS_FILE(1, "The file path must be to a directory."),
    INGESTION_NO_ISBN_IN_ZIP_NAME(3, "No ISBN in ZIP name"),

    CONTENT_INGESTION_GENERAL(100, "Content ingestion general error"),
    CONTENT_INGESTION_EXTRACT_ZIP(101, "Error extracting the content zip file"),
    CONTENT_INGESTION_HANDLE_CHAPTERS(102, "Error extracting and importing chapters"),
    CONTENT_INGESTION_HANDLE_SUPPLEMENTARY_FILES(103, "Error extracting and importing supplementary files"),
    CONTENT_INGESTION_HANDLE_ARTWORK_FILES(104, "Error extracting and importing artwork files"),
    CONTENT_INGESTION_HANDLE_XML_FILE(105, "Error extracting and importing XML file"),
    CONTENT_INGESTION_HANDLE_ADOBE_BOOK(106, "Error extracting the PDF book from the Adobe folder"),
    CONTENT_INGESTION_CHAPTER_FILES_MISMATCH(107, "Different number of chapter PDF and XML files"),

    METADATA_INGESTION_GENERAL(200, "Metadata ingestion general error"),
    METADATA_INGESTION_EXTRACT_ZIP(201, "Error extracting the metadata zip file"),

    PUBLISHING_CHECKER_GENERAL(300, "Publishing checker general error");

    private final int code;
    private final String description;

    ProcessingErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
