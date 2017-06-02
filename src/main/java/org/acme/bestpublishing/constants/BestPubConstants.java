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
package org.acme.bestpublishing.constants;

import java.util.regex.Pattern;

/**
 * Common constants used throughout the Best Publishing solution
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
public interface BestPubConstants {
     int ISBN_NUMBER_LENGTH = 13;
     Pattern ISBN_REGEXP_PATTERN = Pattern.compile("^(97[8|9]\\d{10})");

    /**
     * Mime types not found in org.alfresco.repo.content.MimetypeMap
     */
     String MIMETYPE_ZIP_COMPRESSED = "application/x-zip-compressed";

    /**
     * Default Alfresco folder names, usernames, and groups
     */
     String COMPANY_HOME_NAME = "Company Home";
     String DATA_DICTIONARY_NAME = "Data Dictionary";
     String SITES_NAME = "Sites";
     String DOCUMENT_LIBRARY_NAME = "documentLibrary";
     String ADMIN_USER_NAME = "admin";
     String ALFRESCO_ADMINISTRATORS_GROUP_NAME = "GROUP_ALFRESCO_ADMINISTRATORS";

    /**
     * Best Publishing Specific Folder names
     */
     String BOOK_MANAGEMENT_SITE_NAME = "book-management";
     String BESTPUB_FOLDER_NAME = "BestPub";
     String CHAPTERS_FOLDER_NAME = "Chapters";
     String ARTWORK_FOLDER_NAME = "Artwork";
     String SUPPLEMENTARY_FOLDER_NAME = "Supplementary";
     String STYLES_FOLDER_NAME = "Styles";
     String DATA_DICTIONARY_BESTPUB_FOLDER_PATH = "/" + DATA_DICTIONARY_NAME + "/" + BESTPUB_FOLDER_NAME;
     String METADATA_CHECKER_STATUS_FOLDER_NAME = "Metadata Checker";
     String METADATA_CHECKER_STATUS_FOLDER_PATH = DATA_DICTIONARY_BESTPUB_FOLDER_PATH + "/" +
            METADATA_CHECKER_STATUS_FOLDER_NAME;
     String INCOMING_BASE_FOLDER_PATH = DATA_DICTIONARY_BESTPUB_FOLDER_PATH + "/Incoming";
     String INCOMING_CONTENT_FOLDER_PATH = INCOMING_BASE_FOLDER_PATH + "/Content";
     String INCOMING_METADATA_FOLDER_PATH = INCOMING_BASE_FOLDER_PATH + "/Metadata";
     String INCOMING_PUBSTATUS_FOLDER_PATH = INCOMING_BASE_FOLDER_PATH + "/PublishingStatus";
     String PATH_TO_BESTPUB = "/app:company_home/cm:" + BESTPUB_FOLDER_NAME;

    /**
     * Chapter folder naming, chapter-1, chapter-2 etc
     */
     String CHAPTER_FOLDER_NAME_PREFIX = "chapter";


}
