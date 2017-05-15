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
package org.marversolutions.bestpublishing.constants;

/*
 * Processing Error codes for the different Best Publishing components that support the workflow.
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
public enum ProcessingErrorCode {
    CONTENT_CHECKER_GENERAL(100, "Content ingestion general error"),
    CONTENT_CHECKER_EXTRACT_ZIP(101, "Error extracting the zip file"),
    CONTENT_CHECKER_HANDLE_CHAPTERS(102, "Error extracting and importing chapters"),
    CONTENT_CHECKER_HANDLE_SUPPLEMENTARY_FILES(103, "Error extracting and importing supplementary files"),
    CONTENT_CHECKER_HANDLE_ARTWORK_FILES(104, "Error extracting and importing artwork files"),
    CONTENT_CHECKER_HANDLE_XML_FILE(105, "Error extracting and importing XML file"),
    CONTENT_CHECKER_HANDLE_ADOBE_BOOK(106, "Error extracting the PDF book from the Adobe folder"),
    CONTENT_CHECKER_CHAPTER_FILES_MISMATCH(107, "Different number of chapter PDF and XML files"),

    METADATA_CHECKER_GENERAL(200, "Metadata checker general error"),

    PUBLISHING_CHECKER_GENERAL(300, "Publishing checker general error"),

    REPUBLISHING_CHECKER_GENERAL(400, "Republishing checker general error");

    private final int code;
    private final String description;

    private ProcessingErrorCode(int code, String description) {
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
