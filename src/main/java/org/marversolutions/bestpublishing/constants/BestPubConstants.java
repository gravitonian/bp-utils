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

import java.util.regex.Pattern;

/**
 * Common constants used throughout the Best Publishing solution
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
public interface BestPubConstants {
    public static final int ISBN_NUMBER_LENGTH = 13;
    public static final Pattern ISBN_REGEXP_PATTERN = Pattern.compile("^(97[8|9]\\d{10})");

    /**
     * Default Alfresco folder names, usernames, and groups
     */
    public static final String COMPANY_HOME_NAME = "Company Home";
    public static final String DATA_DICTIONARY_NAME = "Data Dictionary";
    public static final String ADMIN_USER_NAME = "admin";
    public static final String ALFRESCO_ADMINISTRATORS_GROUP_NAME = "GROUP_ALFRESCO_ADMINISTRATORS";

    /**
     * Best Publishing Specific Folder names
     */
    public static final String BESTPUB_FOLDER_NAME = "BESTPUB";
    public static final String ADOBE_FOLDER_NAME = "Adobe";
    public static final String ADOBE_CHAPTERS_FOLDER_NAME = "Adobe Chapters";
    public static final String ARTWORK_FOLDER_NAME = "Artwork";
    public static final String SUPPLEMENTARY_FOLDER_NAME = "Supplementary";
    public static final String DATA_DICTIONARY_BESTPUB_FOLDER_PATH = "/" + DATA_DICTIONARY_NAME + "/BESTPUB";
    public static final String METADATA_CHECKER_STATUS_FOLDER_NAME = "Metadata Checker";
    public static final String METADATA_CHECKER_STATUS_FOLDER_PATH = DATA_DICTIONARY_BESTPUB_FOLDER_PATH + "/" +
            METADATA_CHECKER_STATUS_FOLDER_NAME;
    public static final String INCOMING_BASE_FOLDER_PATH = DATA_DICTIONARY_BESTPUB_FOLDER_PATH + "/Incoming";
    public static final String INCOMING_CONTENT_FOLDER_PATH = INCOMING_BASE_FOLDER_PATH + "/Content";
    public static final String INCOMING_METADATA_FOLDER_PATH = INCOMING_BASE_FOLDER_PATH + "/Metadata";
    public static final String INCOMING_MANUAL_METADATA_FOLDER_PATH = INCOMING_METADATA_FOLDER_PATH + "/Manual";
    public static final String INCOMING_PUBSTATUS_FOLDER_PATH = INCOMING_BASE_FOLDER_PATH + "/PublishingStatus";
    public static final String PATH_TO_BESTPUB = "/app:company_home/cm:BESTPUB";

    /**
     * Chapter folder naming, chapter1, chapter2 etc
     */
    public static final String CHAPTER_FOLDER_NAME_PREFIX = "chapter";


}
